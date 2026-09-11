# M01-07 · AOP、Filter、Interceptor：日志、traceId、耗时统计的正确位置

> **预计阅读时间**：90～120 分钟
> **配套作业**：[作业 07 · AOP/Filter/Interceptor 实战](../作业/07-AOP-Filter-Interceptor实战.md)
> **版本基线**：Java 21、Spring Boot 4.1.0

> **本章目标**
> - 说清 Filter、Interceptor、AOP 三者的运行位置、能拿到什么、执行顺序
> - 能为每一类横切关注点（traceId、访问日志、耗时统计、鉴权）选对落点
> - 用 Filter + MDC 给每个请求打上 traceId，让一次请求的日志能串起来
> - 理解为什么"哪里都能写日志"不等于"哪里都该写日志"

---

## 一、横切关注点：为什么需要专门的机制

商品服务里，除了"创建商品""查询商品"这些**业务逻辑**，还有一类需求：

- 每个请求分配一个 traceId，方便排查问题时把散落的日志串起来；
- 记录每个请求的方法、路径、状态码、耗时；
- 统计某些 Service 方法的执行时间；
- 登录态校验、权限校验；
- 跨域、请求体大小限制、压缩。

这些需求的共同点是：**它们横切在很多业务方法上，本身不属于任何一个业务**。如果把它们写进每个 Controller 或 Service，会出现两个问题：

1. 重复——每个方法都要写一遍 `long start = ...; ...; log(耗时)`；
2. 污染——业务代码里混进了大量与业务无关的样板。

横切关注点（Cross-Cutting Concern）需要一套机制，把它们从业务代码里抽出来，集中管理。Spring 提供了三层落点：**Filter、Interceptor、AOP**。选错落点，要么拿不到需要的信息，要么在错误的时机执行。

---

## 二、请求处理链路全景

先建立一张地图。一个 HTTP 请求打到 Spring Boot 应用，大致经过：

```
客户端
  │  HTTP 请求
  ▼
Servlet 容器（Tomcat）
  │
  ▼
┌─────────────── Filter 链（Servlet 规范层）───────────────┐
│  Filter A.doFilter 前半段                                 │
│    ▼                                                       │
│  DispatcherServlet                                         │
│    │                                                       │
│    ▼                                                       │
│  ┌────────── Interceptor 链（Spring MVC 层）──────────┐   │
│  │  Interceptor.preHandle                              │   │
│  │    ▼                                                │   │
│  │  ┌────── AOP 切面（Spring Bean 方法层）──────┐      │   │
│  │  │  @Around 前 / @Before                      │      │   │
│  │  │    ▼                                        │      │   │
│  │  │  Controller 方法 → Service 方法             │      │   │
│  │  │    ▲                                        │      │   │
│  │  │  @Around 后 / @AfterReturning              │      │   │
│  │  └────────────────────────────────────────────┘      │   │
│  │    ▲                                                │   │
│  │  Interceptor.postHandle                             │   │
│  │  Interceptor.afterCompletion                        │   │
│  └─────────────────────────────────────────────────────┘   │
│    ▲                                                       │
│  Filter A.doFilter 后半段                                  │
└────────────────────────────────────────────────────────────┘
  │  HTTP 响应
  ▼
客户端
```

三层从外到内、由早到晚包裹住业务方法。**越靠外的机制，越早执行、越晚返回，能拦截的范围越大，但离业务越远、拿到的信息越"原始"**。

---

## 三、Filter：Servlet 规范层的最外圈

### 3.1 Filter 是什么

Filter 是 Servlet 规范（`jakarta.servlet.Filter`）定义的，不属于 Spring MVC。它工作在 `DispatcherServlet` 之前，拿到的是最原始的 `HttpServletRequest` / `HttpServletResponse`。

```java
public class TraceIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        // ---- 请求进入：doFilter 前半段 ----
        // 这里可以读写请求头、包装请求体、决定是否放行
        chain.doFilter(request, response);   // 放行到下一个 Filter / DispatcherServlet
        // ---- 响应返回：doFilter 后半段 ----
        // 这里请求已经处理完，可以读写响应头、记录整体耗时
    }
}
```

关键点：`chain.doFilter()` 之前是"进"，之后是"出"。一个 Filter 天然能包住整个后续处理，包括 Spring MVC 之外的静态资源、错误转发。

### 3.2 Filter 的能力边界

- **能拿到**：原始 request/response、请求头、URL、请求体（需要包装才能重复读）；
- **拿不到**：这个请求最终会由哪个 Controller 方法处理（此时还没进入 MVC 的映射阶段）；
- **适合**：traceId 生成、请求/响应体记录、字符编码、跨域、压缩、限流——这些不关心"打到哪个方法"的关注点。

### 3.3 在 Spring Boot 里注册 Filter

两种常用方式：

```java
// 方式一：@Component + implements Filter，自动注册（顺序不易控制）
@Component
public class TraceIdFilter implements Filter { ... }

// 方式二：FilterRegistrationBean，能精确控制顺序和 URL 匹配（推荐）
@Configuration
public class WebFilterConfig {
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new TraceIdFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);   // 数字越小越先执行
        return reg;
    }
}
```

Spring 还提供 `OncePerRequestFilter`（`org.springframework.web.filter`），它保证一次请求内只执行一次（避免 forward/include 时重复执行），日常业务 Filter 更推荐继承它：

```java
public class TraceIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 已经是强类型的 HttpServletRequest/Response
        chain.doFilter(request, response);
    }
}
```

---

## 四、Interceptor：Spring MVC 层，知道打到哪个方法

### 4.1 Interceptor 是什么

`HandlerInterceptor` 是 Spring MVC 提供的，工作在 `DispatcherServlet` 内部、Controller 方法前后。它比 Filter 晚执行，但**知道这个请求要交给哪个 handler（Controller 方法）处理**。

```java
public class AccessLogInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        // Controller 方法执行前。handler 就是即将执行的方法（HandlerMethod）
        // 返回 false 可以拦截请求，不再进入 Controller
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // Controller 方法正常返回后执行（抛异常则不执行）
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // 整个请求完成后执行，无论成功还是抛异常，都会执行——适合记录最终耗时/清理
    }
}
```

### 4.2 三个方法的执行时机 ⭐

| 方法 | 时机 | 异常时是否执行 | 典型用途 |
|---|---|---|---|
| `preHandle` | Controller 方法前 | —（它决定是否放行） | 鉴权、开始计时、放行判断 |
| `postHandle` | Controller 正常返回后、渲染视图前 | ❌ 抛异常不执行 | 修改 ModelAndView（前后端分离下少用） |
| `afterCompletion` | 请求彻底结束后 | ✅ 一定执行 | 记录耗时、清理资源 |

**记住**：计时"开始"放 `preHandle`，计时"结束"和访问日志放 `afterCompletion`——因为只有 `afterCompletion` 在异常时也会执行，能保证日志不丢。

### 4.3 注册 Interceptor

```java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AccessLogInterceptor accessLogInterceptor;

    public WebMvcConfig(AccessLogInterceptor accessLogInterceptor) {
        this.accessLogInterceptor = accessLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/api/**")           // 只拦业务接口
                .excludePathPatterns("/actuator/**");  // 放过监控端点
    }
}
```

Interceptor 能按路径精确匹配，这是它相对 Filter 的一大优势——Filter 的 URL 匹配能力较弱。

---

## 五、AOP：Bean 方法层，能拿到方法入参和返回值

### 5.1 AOP 是什么

AOP（Aspect-Oriented Programming，面向切面编程）作用在 **Spring Bean 的方法调用**上。它靠动态代理实现：Spring 为目标 Bean 生成代理对象，在方法调用前后插入切面逻辑。

它比 Interceptor 更深入——不仅是 Controller，任何 Spring 管理的 Bean 方法（Service、Repository）都能被切。而且它能拿到**方法的参数、返回值、抛出的异常**。

### 5.2 核心术语

| 术语 | 含义 |
|---|---|
| Aspect（切面） | 横切逻辑的载体，一个 `@Aspect` 类 |
| Join Point（连接点） | 可以插入切面的位置，Spring AOP 里就是"方法调用" |
| Pointcut（切点） | 用表达式描述"切哪些方法" |
| Advice（通知） | 切面在连接点做什么，分 `@Before`/`@After`/`@Around` 等 |

### 5.3 一个耗时统计切面

```java
@Aspect
@Component
public class TimingAspect {

    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    // 切点：com.study.mall.product.service 包下所有 public 方法
    @Around("execution(public * com.study.mall.product.service..*(..))")
    public Object logTiming(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.nanoTime();
        try {
            return pjp.proceed();   // 执行目标方法
        } finally {
            long costMs = (System.nanoTime() - start) / 1_000_000;
            log.info("{} 耗时 {}ms", pjp.getSignature().toShortString(), costMs);
        }
    }
}
```

`@Around` 是最强的通知：它包住目标方法，能决定是否执行（`pjp.proceed()`）、能改入参、能改返回值、能捕获异常。`finally` 保证异常时也记录耗时。

### 5.4 五种 Advice

| 注解 | 执行时机 |
|---|---|
| `@Before` | 方法执行前 |
| `@AfterReturning` | 方法正常返回后（能拿到返回值） |
| `@AfterThrowing` | 方法抛异常后（能拿到异常） |
| `@After` | 方法结束后（相当于 finally，成功失败都执行） |
| `@Around` | 包住方法，最灵活，也最容易写错（忘了 `proceed()` 会导致目标方法不执行） |

### 5.5 Spring AOP 的两个坑 ⚠️

1. **自调用失效**：同一个类里 A 方法调用本类的 B 方法，B 上的切面不生效——因为自调用没走代理对象。解决：拆到别的 Bean，或注入自身代理。这也是 `@Transactional`、`@Cacheable` 同类自调用失效的根因。
2. **只能切 Spring 管理的 Bean**：`new` 出来的对象、static 方法切不到。

Spring AOP 基于代理，只覆盖"经过容器的方法调用"。要切任意方法（构造器、字段、非 Bean），需要 AspectJ 编译期/加载期织入，本课程不涉及。

---

## 六、三者对比与选型 ⭐

| 维度 | Filter | Interceptor | AOP |
|---|---|---|---|
| 规范/框架 | Servlet 规范 | Spring MVC | Spring AOP |
| 作用位置 | DispatcherServlet 之前 | Controller 前后 | 任意 Bean 方法前后 |
| 能否知道目标 Controller 方法 | ❌ | ✅（HandlerMethod） | ✅（且能拿参数/返回值） |
| 能拿到方法入参/返回值 | ❌ | ❌ | ✅ |
| 能否切 Service/Repository | ❌ | ❌（只到 Controller） | ✅ |
| 拿到的对象 | 原始 request/response | request/response + handler | 方法签名、参数、返回值 |
| 典型用途 | traceId、编码、跨域、限流、请求体记录 | 鉴权、访问日志、请求耗时 | 方法级耗时、业务审计、缓存、事务 |

选型口诀：

- 关注**整个请求、不关心打到哪个方法** → **Filter**（traceId 就该在这里生成，越早越好）；
- 关注**请求级、需要知道 handler 或按路径拦截** → **Interceptor**（访问日志、鉴权）；
- 关注**具体某类方法的入参/返回值/耗时** → **AOP**（Service 方法监控、审计）。

---

## 七、执行顺序：一次请求的完整轨迹 ⭐

对一个正常请求，三者的执行顺序是：

```
Filter 前
  └─ Interceptor.preHandle
       └─ AOP @Around 前
            └─ Controller 方法（→ Service，Service 方法也可能被 AOP 切）
       └─ AOP @Around 后
  └─ Interceptor.postHandle
  └─ Interceptor.afterCompletion
Filter 后
```

即：**Filter 最外，Interceptor 居中，AOP 最内**。进入时由外到内，返回时由内到外，像剥洋葱。

抛异常时的差异：

- Interceptor 的 `postHandle` 不执行，`afterCompletion` 执行；
- AOP `@Around` 的 `finally` / `@After` 执行；
- 异常最终由 `@RestControllerAdvice`（第 05 章的 `GlobalExceptionHandler`）处理。

多个同类之间的顺序：Filter 用 `@Order` / `FilterRegistrationBean.setOrder`，Interceptor 按注册顺序，切面用 `@Order`。

---

## 八、traceId 实战：让一次请求的日志串起来

### 8.1 问题

生产环境并发几百个请求，日志交织在一起。排查"某个用户的下单为什么失败"时，无法把属于同一次请求的日志挑出来。解决办法：给每个请求分配唯一 traceId，让这次请求产生的每一条日志都带上它。

### 8.2 MDC：日志框架的上下文

MDC（Mapped Diagnostic Context）是 SLF4J/Logback 提供的**线程绑定的键值上下文**。往 MDC 里放一个值，同一线程后续所有日志都能自动带上它。

```java
import org.slf4j.MDC;

MDC.put("traceId", "abc123");
// 之后这个线程打的每条日志都能通过日志 pattern 输出 traceId
MDC.remove("traceId");   // 用完必须清理，否则线程池复用时会串味
```

### 8.3 用 Filter 注入 traceId

traceId 要尽可能早地生成，覆盖整个请求，所以放在 **Filter**（最外层）：

```java
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 优先复用上游传入的 traceId（网关/上游服务），没有才自己生成
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);   // 回传给客户端，方便对账
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);   // ⚠️ 必须清理，防止线程池复用污染
        }
    }
}
```

### 8.4 让日志 pattern 输出 traceId

在 `application.yml` 里配置日志格式，用 `%X{traceId}` 取 MDC 里的值：

```yaml
logging:
  pattern:
    level: "[%X{traceId:-}] %5p"   # -} 表示 traceId 不存在时输出空
```

之后每条日志都会带上 `[abc123...]`，同一请求的日志用 traceId 就能筛出来。

### 8.5 为什么 traceId 不放 Interceptor 或 AOP

- 放 Interceptor：Filter 阶段、DispatcherServlet 映射阶段的日志就没有 traceId，覆盖不全；
- 放 AOP：只有被切的方法内部有 traceId，Controller 之外的日志都没有；
- 放 Filter：从请求最外层就注入，整条链路的日志都能覆盖——这就是"正确的位置"。

---

## 九、访问日志与耗时：放 Interceptor

访问日志需要知道请求打到哪个 handler、最终状态码和耗时，且要保证异常时也记录，所以放 **Interceptor**，用 `preHandle` 计时开始、`afterCompletion` 计时结束：

```java
@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AccessLogInterceptor.class);
    private static final String START = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) {
        req.setAttribute(START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse resp,
                                Object handler, Exception ex) {
        long cost = System.currentTimeMillis() - (long) req.getAttribute(START);
        log.info("{} {} -> {} ({}ms)", req.getMethod(), req.getRequestURI(),
                resp.getStatus(), cost);
    }
}
```

注意：耗时状态跨方法传递不要用成员变量（Interceptor 是单例，多线程共享会串），要用 `request.setAttribute` 挂在请求对象上。

---

## 十、放错位置的代价（真实场景）

| 错误做法 | 后果 |
|---|---|
| traceId 放 Interceptor | Filter 层、静态资源、映射阶段日志无 traceId，链路断裂 |
| 访问日志放 `postHandle` | 请求抛异常时不记录，最需要日志的失败请求反而没日志 |
| 耗时用 Interceptor 成员变量存 start | 单例被多线程共享，并发下耗时算错 |
| MDC 用完不 `remove` | 线程池复用，下个请求打出上个请求的 traceId |
| 鉴权写进每个 Controller | 重复、易漏，改规则要改一片 |
| 用 AOP 切 Controller 记访问日志 | 拿不到 HTTP 状态码，还和 MVC 生命周期脱节，不如 Interceptor 顺手 |

"哪里都能打日志"是对的，但**每类关注点都有唯一最合适的落点**，选对了才不留死角、不出并发 bug。

---

## 十一、小结

| 知识点 | 记住这一句 |
|---|---|
| Filter | Servlet 层最外圈，拿原始 request，不知道打到哪个方法，traceId 在这里生成 |
| Interceptor | Spring MVC 层，知道 handler，`afterCompletion` 一定执行，访问日志/鉴权在这里 |
| AOP | Bean 方法层，能拿参数/返回值，Service 方法监控/审计在这里 |
| 执行顺序 | Filter → Interceptor.preHandle → AOP → Controller → AOP → postHandle → afterCompletion → Filter |
| traceId | Filter + MDC，`%X{traceId}` 输出，`finally` 里 `remove` |
| 耗时/访问日志 | Interceptor preHandle 起、afterCompletion 止，状态挂 request 属性 |
| AOP 自调用 | 同类方法自调用切面失效，根因是没走代理 |

---

## 十二、自检问题

不查资料，先口头回答：

1. Filter、Interceptor、AOP 三者的执行顺序是什么？进入和返回分别是什么方向？
2. traceId 为什么必须放在 Filter，而不是 Interceptor 或 AOP？
3. Interceptor 的 `postHandle` 和 `afterCompletion` 有什么区别？记录耗时该放哪个？为什么？
4. 为什么访问日志的耗时开始时间不能存成 Interceptor 的成员变量？
5. MDC 用完为什么必须 `remove`？不清理会出什么问题？
6. AOP 的 `@Around` 忘了调用 `proceed()` 会发生什么？
7. 同一个类里 A 方法调用本类 B 方法，B 上的 `@Around` 为什么不生效？
8. 记录"某个 Service 方法的入参和返回值"，应该用 Filter、Interceptor 还是 AOP？为什么？

答不上 1、2、3 的，回看六、七、八章节再做作业。

---

## 十三、延伸阅读

- [Spring Framework Reference - Aspect Oriented Programming](https://docs.spring.io/spring-framework/reference/core/aop.html)（AOP 概念、切点表达式、代理机制）
- [Spring Framework Reference - Filters and Interceptors](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-servlet/handlermapping-interceptor.html)（HandlerInterceptor 生命周期）
- [SLF4J MDC 文档](https://www.slf4j.org/api/org/slf4j/MDC.html)（诊断上下文的线程语义）

> Content was rephrased for compliance with licensing restrictions.

---

## 下一步

完成[作业 07 · AOP/Filter/Interceptor 实战](../作业/07-AOP-Filter-Interceptor实战.md)。核心目标：

1. 用 Filter + MDC 给 `product-service` 每个请求注入 traceId，日志格式带上它（正式交付）；
2. 用 Interceptor 记录访问日志和耗时，保证异常时也不丢日志（正式交付）；
3. 在实验区观察 Filter / Interceptor / AOP 的完整执行顺序，并用 AOP 统计 Service 方法耗时（知识点实验）。
