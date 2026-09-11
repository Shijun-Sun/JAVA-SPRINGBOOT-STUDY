# 作业 07 · AOP / Filter / Interceptor 实战

> **对应讲义**：[M01-07 · AOP、Filter、Interceptor](../学习/07-AOP-Filter-Interceptor.md)
> **预计耗时**：3～5 小时
> **难度**：中等
> **Review 模式**：默认"只指出错误，不直接给答案"

> 正式交付任务在现有 `product-service` 上继续迭代，不要另起工程。
> 机制观察类实验在 `homework/experimental/web-chain-order-lab/`（已提供脚手架）里做，不要塞进正式工程。

---

## 一、背景设定

`product-service` 的 CRUD、配置、异常处理、分层都已就绪。现在补上**可观测性地基**：

- 每个请求要有唯一 traceId，日志能按 traceId 串起一次完整请求；
- 每个请求要有访问日志，记录方法、路径、状态码、耗时，且请求失败时也不能丢；
- 理解 Filter / Interceptor / AOP 的执行顺序和各自适用场景（机制观察，放实验区）。

这是模块验收清单里"每个请求有 traceId，日志可串起完整链路"的落地。

---

## 二、必做任务

---

### 任务一：traceId 过滤器

> - 性质：正式交付
> - 实现位置：`homework/delivered/product-service/src/main/java/com/study/mall/product/common/web/`（新建 `web` 包）
> - 级别：必做
> - 是否阻塞推进：是
> - 前置交付：作业 04/05/06 完成
> - 验证方式：任意接口响应头含 `X-Trace-Id`，且服务端日志带同一 traceId

新建 `TraceIdFilter`，要求：

1. 继承 `OncePerRequestFilter`；
2. 优先复用请求头 `X-Trace-Id`，没有才用 `UUID` 生成（去掉连字符）；
3. 把 traceId 放入 `MDC`（key 用 `traceId`）；
4. 把 traceId 通过响应头 `X-Trace-Id` 回传客户端；
5. 在 `finally` 里 `MDC.remove("traceId")`；
6. 用 `FilterRegistrationBean` 注册，`order` 设为最高优先级，`urlPatterns` 为 `/*`；
7. 在 `application.yml` 配置日志 pattern，用 `%X{traceId:-}` 输出 traceId。

**验收标准：**

- `curl -i` 任意接口，响应头出现 `X-Trace-Id`；
- 服务端日志每一行都带上该 traceId；
- 客户端传入自定义 `X-Trace-Id` 时，服务端复用该值而不是重新生成；
- 连续两个请求的 traceId 不同（证明 `MDC.remove` 生效，无串味）。

---

### 任务二：访问日志拦截器

> - 性质：正式交付
> - 实现位置：`homework/delivered/product-service/src/main/java/com/study/mall/product/common/web/`
> - 级别：必做
> - 是否阻塞推进：是
> - 前置交付：任务一
> - 验证方式：正常请求和异常请求都能看到访问日志

新建 `AccessLogInterceptor`，要求：

1. 实现 `HandlerInterceptor`；
2. `preHandle` 里记录开始时间，**挂在 `request.setAttribute` 上**，不要用成员变量；
3. `afterCompletion` 里计算耗时，记录一行访问日志：`method uri -> status (costms)`；
4. 通过 `WebMvcConfigurer.addInterceptors` 注册，`addPathPatterns("/api/**")`，`excludePathPatterns("/actuator/**")`。

**验收标准：**

- 正常请求（如 `GET /api/v1/products`）有访问日志，含方法、路径、状态码、耗时；
- 触发异常的请求（如 `GET /api/v1/products/99999` → 404）**也有**访问日志（证明用的是 `afterCompletion` 而非 `postHandle`）；
- 访问日志带 traceId（证明任务一的 Filter 在外层，MDC 已就绪）；
- `/actuator/**` 不产生访问日志。

---

### 任务三：端到端验证

> - 性质：正式交付（验证记录）
> - 级别：必做
> - 是否阻塞推进：是
> - 验证方式：curl + 日志

启动服务，依次执行并在交付说明中粘贴关键证据：

```bash
# 1. 正常请求，看响应头和日志的 traceId
curl -i http://localhost:8080/api/v1/products

# 2. 自定义 traceId，验证被复用
curl -i -H "X-Trace-Id: my-trace-001" http://localhost:8080/api/v1/products

# 3. 异常请求，验证访问日志仍然记录
curl -i http://localhost:8080/api/v1/products/99999

# 4. 连续两次默认请求，验证 traceId 不同（无串味）
curl -s -i http://localhost:8080/api/v1/products | grep -i x-trace-id
curl -s -i http://localhost:8080/api/v1/products | grep -i x-trace-id
```

**验收标准：**

- 响应头 traceId 与服务端日志 traceId 一致；
- 自定义 traceId 被复用；
- 异常请求有访问日志；
- 两次请求 traceId 不同。

---

## 三、进阶任务（按需选做）

### 任务四：执行顺序观察实验

> - 性质：知识点实验
> - 实现位置：`homework/experimental/web-chain-order-lab/`（已提供脚手架）
> - 级别：进阶
> - 是否阻塞推进：否
> - 验证方式：一次请求的日志中，各环节按预期顺序打印

脚手架里已有一个最小 Spring Boot 工程和一个 `/demo` 接口，但 `Filter`、`Interceptor`、`Aspect` 三个类的方法体是 **TODO**。补全它们，各自在"进入"和"返回"时打印一行带序号的日志，然后请求 `/demo`，观察并回答：

1. 三者进入的顺序是什么？返回的顺序是什么？
2. `Interceptor.postHandle` 和 `afterCompletion` 谁先谁后？
3. 把 `/demo` 改成抛异常，哪些环节还会执行，哪些不会？
4. 用日志证据说明"Filter 最外、Interceptor 居中、AOP 最内"。

脚手架初始状态可编译、可启动，`/demo` 返回固定字符串；补全 TODO 后再观察顺序。**不要把这些观察用的日志类塞进 `product-service`。**

### 任务五：AOP 统计 Service 方法耗时

> - 性质：知识点实验
> - 实现位置：`homework/experimental/web-chain-order-lab/`
> - 级别：进阶
> - 是否阻塞推进：否
> - 验证方式：调用 `/demo` 时打印 Service 方法耗时

在同一脚手架里，给 `DemoService` 的方法加一个 `@Around` 切面统计耗时。回答：

1. 切点表达式怎么写才能只切 `service` 包？
2. 如果 `DemoController` 直接调用本类的另一个方法，切面会生效吗？为什么？
3. `@Around` 里如果不调用 `proceed()`，接口会返回什么？

---

## 四、挑战任务（面试 / 答辩级）

### 任务六：traceId 跨线程传递

> - 级别：挑战

MDC 是线程绑定的。如果 Controller 里用 `@Async` 或线程池异步执行一段逻辑，子线程的日志会**丢失 traceId**。分析：

1. 为什么子线程拿不到 traceId？
2. 有哪些方案能把 traceId 传到子线程（`TaskDecorator`、手动 `MDC.getCopyOfContextMap` 等）？
3. 这些方案各自的代价是什么？

只需提交分析，不要求在 `product-service` 里落地异步。

---

## 五、提交检查清单

### 正式交付（product-service）

- [ ] `TraceIdFilter` 继承 `OncePerRequestFilter`，复用/生成 traceId，`finally` 里 `remove`
- [ ] 用 `FilterRegistrationBean` 注册，order 最高优先级
- [ ] 日志 pattern 用 `%X{traceId:-}` 输出 traceId
- [ ] `AccessLogInterceptor` 用 `afterCompletion` 记录，start 挂 `request` 属性
- [ ] 拦截 `/api/**`，排除 `/actuator/**`
- [ ] 异常请求也有访问日志
- [ ] 未把机制观察用的类塞进正式工程

### 验证证据

- [ ] 响应头与日志 traceId 一致
- [ ] 自定义 traceId 被复用
- [ ] 异常请求访问日志存在
- [ ] 两次请求 traceId 不同

### 构建

```bash
./mvnw package -DskipTests
```

- [ ] 构建成功，启动后接口可访问
- [ ] `target/` 未提交

---

## 六、交付说明（完成后填写）

```markdown
完成时间：2026-09-09（交付门禁核验）
完成任务：必做 1 / 2 / 3；进阶：任务四脚手架已提供（web-chain-order-lab）；挑战：未提交

### traceId 验证（任务一 / 三）
- 响应头 X-Trace-Id：`X-Trace-Id: 831d20c468e84aeaa982eff1c9c202bf`
- 日志中同一 traceId 示例行：
  `2026-09-09T09:44:14.387+08:00 [831d20c468e84aeaa982eff1c9c202bf] INFO ... c.s.m.p.common.web.AccessLogInterceptor : GET /api/v1/products -> 200 (37ms)`
  （响应头与日志 traceId 一致）
- 自定义 traceId 复用证据：请求头 `X-Trace-Id: my-trace-001` → 响应头回传 `my-trace-001`，
  日志 `[my-trace-001] ... GET /api/v1/products -> 200 (1ms)`
- 两次请求 traceId（应不同）：`ff466394ec2b4eaeb3eaa1f0ad363e79` 与 `542d05fd679e4398ab7fada31bae4e7e`（不同，MDC.remove 生效）

### 访问日志验证（任务二 / 三）
- 正常请求日志行：`[831d20c...] ... GET /api/v1/products -> 200 (37ms)`
- 异常请求日志行（证明 afterCompletion）：`[592f856f...] ... GET /api/v1/products/99999 -> 404 (1ms)`（404 仍记录）
- /actuator 是否被记录（应否）：否。访问日志中无 `/actuator/**` 行，excludePathPatterns 生效。

### 执行顺序实验（任务四，选做）
- 脚手架 `homework/experimental/web-chain-order-lab/` 已就位；观察结论待用户补充。

### 构建结果
- `./mvnw -o clean package -DskipTests`：BUILD SUCCESS，产出 `target/product-service-0.0.1-SNAPSHOT.jar`
- `./mvnw test`（经本机代理补齐 surefire-junit-platform 插件后）：`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`，Spring Boot 4.1.0 / Java 26 上下文加载通过
- 启动烟测：`--spring.profiles.active=dev` 启动成功，端口 8080，四条 curl 用例全部符合验收标准

遇到的问题：
- 无 profile 直接启动会因 `ProductConfig` 绑定校验失败（`product.storage/pagination/pricing.cache-time` 只在 dev/prod profile 提供）。这是第 03 章"主配置只放公共段"的既定设计，启动需带 `--spring.profiles.active=dev`。
- 旁路观察：`GET /actuator/health` 返回 500（`NoResourceFoundException`），因 base 配置仅暴露 `conditions/beans/mappings` 未含 `health`，与本章 traceId/访问日志无关，留待配置/监控章节处理。

尚未完成或需要 review 的部分：无（必做项审计通过，未新增或改动 delivered 代码）
```

---

## 七、提交后如何请求 Review

完成后直接说：

> Review 作业 07，只指出错误，不直接给答案。

我会按固定格式检查：结论、错误清单、改进建议、架构视角、可选优化、下一步。
