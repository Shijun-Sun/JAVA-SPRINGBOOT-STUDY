# 02 · IoC 容器与依赖注入

> **本章目标**
> - 说清 IoC 容器的核心价值，以及它和 `new` 的本质区别
> - 掌握 Bean 的注册方式、生命周期和作用域
> - 理解依赖注入的三种方式及各自的取舍
> - 会用条件装配（`@Conditional` 系列）控制 Bean 是否生效
> - 能独立排查常见 Bean 注入错误

---

## 一、为什么要有 IoC 容器

### 1.1 没有容器的世界

```java
// 传统写法：全部手动 new，依赖链一旦复杂就会爆炸
public class OrderService {
    private UserService userService = new UserService(
        new UserRepository(
            new DataSource("jdbc:mysql://...", "root", "pass")
        )
    );
    // ...
}
```

问题一览：
- **耦合**：`OrderService` 知道 `UserRepository` 怎么造，违反"只关心我要什么，不关心怎么造"的原则
- **测试**：换个假的 `UserRepository` 很麻烦，必须改源码
- **复用**：同一个 `DataSource` 可能被 new 了十几次，每次都是一个新连接池

### 1.2 IoC 的核心思想

> "我不创建依赖，我声明我需要它，容器来注入。"

控制权反转（Inversion of Control）：对象的创建和依赖关系的组装，**从调用方转移到了容器**。

```
传统：A.main() → new B() → new C()
IoC：容器启动时扫描 → 造出 C → 造出 B（把 C 注入进去）→ 造出 A（把 B 注入进去）
```

好处：
- 调用方只写 `@Autowired UserService userService;`，不关心 UserService 内部的依赖树
- 测试时用 `@MockBean` 替换某个节点，其余节点不受影响
- 全局单例（默认 Singleton）由容器保证，不用自己写双检锁

---

## 二、Bean 的注册方式

Spring 容器里的一切都是 **Bean**（托管对象）。把类变成 Bean 有三种主要方式。

### 2.1 注解扫描（最常用）

```java
@Service          // 标记为 Service 层 Bean
public class ProductService { ... }

@Repository       // 标记为数据访问层 Bean
public class ProductRepository { ... }

@Component        // 通用，无语义
public class IdGenerator { ... }

@Controller / @RestController  // Web 层
```

这些注解本质都是 `@Component` 的派生，语义区别是给开发者看的，Spring 处理方式相同。

`@SpringBootApplication` → `@ComponentScan` → 扫描当前包及子包 → 找到所有 `@Component` → 注册为 Bean。

### 2.2 `@Bean` 方法（配置类）

用于第三方库（你改不了源码，加不了注解）或需要定制构造逻辑的场景：

```java
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:mysql://localhost:3306/mall");
        ds.setMaximumPoolSize(20);
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {  // 参数自动注入
        return new JdbcTemplate(dataSource);
    }
}
```

> **`@Configuration` vs `@Component`**
> `@Configuration` 类里的 `@Bean` 方法通过 CGLIB 代理确保多次调用返回同一个对象；
> `@Component` 没有这个保证（每次调用 `@Bean` 方法都是真实调用，会 new 新对象）。
> 写配置类用 `@Configuration`，不要用 `@Component` 替代。

### 2.3 `@Import`

直接把指定类注册为 Bean，或引入整个配置类：

```java
@Import({SecurityConfig.class, MetricsConfig.class})
@SpringBootApplication
public class App { ... }
```

自动配置底层大量使用 `@Import`，通过 `AutoConfigurationImportSelector` 批量导入配置类。

---

## 三、Bean 的生命周期

```
1. 实例化（Constructor）
        ↓
2. 属性注入（@Autowired / setter）
        ↓
3. Aware 接口回调（BeanNameAware、ApplicationContextAware...）
        ↓
4. BeanPostProcessor.postProcessBeforeInitialization()
        ↓
5. 初始化（@PostConstruct / InitializingBean.afterPropertiesSet() / init-method）
        ↓
6. BeanPostProcessor.postProcessAfterInitialization()
        ↓
7. Bean 可用（放入容器，等待注入给其他 Bean）
        ↓
8. 容器关闭 → 销毁（@PreDestroy / DisposableBean.destroy()）
```

**最常用的两个钩子：**

```java
@Component
public class CacheWarmup {

    @PostConstruct          // 注入完成后立刻执行，适合初始化数据
    public void init() {
        System.out.println("缓存预热...");
    }

    @PreDestroy             // 容器关闭前执行，适合释放资源
    public void cleanup() {
        System.out.println("清理资源...");
    }
}
```

**避坑：`@PostConstruct` 里不能调用还未初始化的 Bean**
如果 A 依赖 B，B 的 `@PostConstruct` 在 A 的 `@PostConstruct` 之后执行，A 在自己的 `@PostConstruct` 里用 B 可能出问题。

---

## 四、Bean 的作用域（Scope）

| 作用域 | 说明 | 使用场景 |
|--------|------|---------|
| `singleton`（默认） | 容器里只有一个实例，所有注入共享 | 无状态 Service、Repository |
| `prototype` | 每次获取都 new 一个 | 有状态的组件，如命令对象 |
| `request` | 每次 HTTP 请求一个实例（Web 环境） | 存放请求级别的数据 |
| `session` | 每次 HTTP Session 一个实例 | 用户会话数据 |

```java
@Component
@Scope("prototype")
public class ReportBuilder {
    private List<String> lines = new ArrayList<>();
    // 每次注入都是全新实例，lines 不会相互污染
}
```

**常见坑：Singleton 注入 Prototype**

```java
@Service  // singleton
public class OrderService {

    @Autowired
    private ReportBuilder builder;  // prototype，但只注入了一次！
    // 之后每次用的都是同一个 builder，prototype 的意义消失了
}
```

解决方案：
- 用 `ApplicationContext.getBean(ReportBuilder.class)` 每次手动取
- 用 `@Lookup` 注解
- 用 `ObjectProvider<ReportBuilder>`（推荐）

```java
@Service
public class OrderService {

    @Autowired
    private ObjectProvider<ReportBuilder> builderProvider;

    public void generateReport() {
        ReportBuilder builder = builderProvider.getObject();  // 每次拿新的
        // ...
    }
}
```

---

## 五、依赖注入的三种方式

### 5.1 字段注入（Field Injection）——不推荐

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository repository;  // ❌ 不推荐
}
```

缺点：
- 无法在不启动 Spring 容器的情况下测试（必须用反射注入，或者启动容器）
- 字段是 `private`，从外部看不出这个类依赖什么
- 容易产生循环依赖，而且 Spring 会"偷偷"帮你解掉，掩盖设计问题

### 5.2 Setter 注入

```java
@Service
public class ProductService {

    private ProductRepository repository;

    @Autowired
    public void setRepository(ProductRepository repository) {
        this.repository = repository;
    }
}
```

适合**可选依赖**（`@Autowired(required = false)`），但强制依赖还是用构造器。

### 5.3 构造器注入（推荐）✅

```java
@Service
public class ProductService {

    private final ProductRepository repository;    // final，不可变
    private final PriceCalculator calculator;

    // Spring 4.3+ 单构造器可省略 @Autowired
    public ProductService(ProductRepository repository,
                          PriceCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }
}
```

优点：
- **依赖在类外清晰可见**（构造器参数就是依赖清单）
- **可以用 final**，保证注入后不被篡改
- **单元测试友好**：`new ProductService(mockRepo, mockCalc)` 直接测，不依赖容器
- **循环依赖会立即报错**，而不是被悄悄处理

> Lombok 配合使用更简洁：
> ```java
> @Service
> @RequiredArgsConstructor  // 自动生成包含所有 final 字段的构造器
> public class ProductService {
>     private final ProductRepository repository;
>     private final PriceCalculator calculator;
> }
> ```

---

## 六、多 Bean 冲突处理

当同一个接口有多个实现时，Spring 不知道注入哪个：

```java
public interface MessageSender { void send(String msg); }

@Component public class EmailSender implements MessageSender { ... }
@Component public class SmsSender implements MessageSender { ... }
```

**方案一：`@Primary`** — 标记默认首选

```java
@Component
@Primary
public class EmailSender implements MessageSender { ... }
```

**方案二：`@Qualifier`** — 按名字精确指定

```java
@Service
public class NotificationService {

    @Autowired
    @Qualifier("smsSender")
    private MessageSender sender;
}
```

**方案三：注入 List / Map**（全部要）

```java
@Service
public class NotificationService {

    private final List<MessageSender> senders;  // 两个实现都注入进来

    public NotificationService(List<MessageSender> senders) {
        this.senders = senders;
    }

    public void broadcast(String msg) {
        senders.forEach(s -> s.send(msg));
    }
}
```

---

## 七、条件装配

`@Conditional` 系列注解控制 Bean 是否被注册，是自动配置的核心机制。

### 7.1 常用注解速查

| 注解 | 生效条件 |
|------|---------|
| `@ConditionalOnProperty` | 配置文件中某个属性存在且满足值 |
| `@ConditionalOnClass` | classpath 上存在某个类 |
| `@ConditionalOnMissingBean` | 容器中不存在指定 Bean |
| `@ConditionalOnBean` | 容器中存在指定 Bean |
| `@ConditionalOnWebApplication` | 是 Web 应用 |
| `@ConditionalOnExpression` | SpEL 表达式为 true |

### 7.2 实际例子

```java
@Configuration
public class CacheConfig {

    // 只有配置了 cache.type=redis 才用 Redis 缓存
    @Bean
    @ConditionalOnProperty(name = "cache.type", havingValue = "redis")
    public CacheManager redisCacheManager() {
        return new RedisCacheManager(...);
    }

    // 没有其他 CacheManager 时，使用内存缓存兜底
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager localCacheManager() {
        return new ConcurrentMapCacheManager();
    }
}
```

这就是 Spring Boot 自动配置的核心逻辑：先看你有没有自己配，没有的话再按条件给你一个默认配置。

### 7.3 自定义 Condition

```java
public class MacOsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String os = System.getProperty("os.name");
        return os != null && os.toLowerCase().contains("mac");
    }
}

@Bean
@Conditional(MacOsCondition.class)
public DevelopmentTool devTool() { ... }
```

---

## 八、循环依赖

### 8.1 什么是循环依赖

```
A 依赖 B → B 依赖 C → C 依赖 A  （死循环）
```

### 8.2 Spring 如何处理（三级缓存）

Spring 对 **Singleton + Setter/字段注入** 的循环依赖做了特殊处理：
1. 创建 A 的早期引用，放入三级缓存
2. 注入 B 时，发现 B 依赖 A，从缓存取出 A 的早期引用
3. B 创建完成，A 完成初始化

**但构造器注入的循环依赖无法自动处理，会直接报错（这是好事）。**

Spring Boot 2.6+ 默认禁用循环依赖，如果出现会直接报错：
```
The dependencies of some of the beans in the application context form a cycle
```

**正确的解决方式：重新设计，而不是绕过**
- 提取公共逻辑到第三个类
- 用事件机制（`ApplicationEvent`）解耦
- 用 `@Lazy` 延迟初始化（临时方案，治标不治本）

---

## 九、`ApplicationContext` 与 `BeanFactory`

```
BeanFactory（基础接口）
    └── ApplicationContext（扩展接口，项目里用这个）
            ├── ClassPathXmlApplicationContext（XML 配置，旧）
            └── AnnotationConfigApplicationContext（注解配置）
                    └── SpringApplication 最终用这个
```

`ApplicationContext` 在 `BeanFactory` 基础上额外提供：
- 国际化（`MessageSource`）
- 事件发布（`ApplicationEventPublisher`）
- 资源加载（`ResourceLoader`）
- AOP 自动代理

项目中直接注入 `ApplicationContext` 可以手动获取 Bean（但要谨慎，过度使用会让依赖关系变得隐式）：

```java
@Service
@RequiredArgsConstructor
public class PluginRegistry {

    private final ApplicationContext ctx;

    public <T> T getPlugin(Class<T> type) {
        return ctx.getBean(type);
    }
}
```

---

## 十、常见报错速查

| 报错信息 | 原因 | 解决方向 |
|---------|------|---------|
| `NoSuchBeanDefinitionException` | 容器里没有这个 Bean | 检查是否加了 `@Component` / 是否在扫描包路径下 |
| `NoUniqueBeanDefinitionException` | 同类型有多个 Bean | 加 `@Primary` 或 `@Qualifier` |
| `UnsatisfiedDependencyException` | 注入失败（依赖缺失） | 看 cause，通常是上面两种之一 |
| `BeanCurrentlyInCreationException` | 循环依赖 | 重新设计依赖关系 |
| `@PostConstruct` 中 NPE | 依赖注入顺序问题 | 检查依赖 Bean 是否还未初始化 |

---

## 小结

| 知识点 | 记住这一句 |
|--------|-----------|
| IoC 价值 | 对象不自己造依赖，让容器组装 |
| Bean 注册 | 注解扫描 / `@Bean` 方法 / `@Import` 三种方式 |
| 生命周期 | 实例化 → 注入 → `@PostConstruct` → 使用 → `@PreDestroy` |
| 作用域 | 默认 Singleton；有状态对象用 Prototype |
| 注入方式 | 构造器注入是首选，字段注入不推荐 |
| 多 Bean 冲突 | `@Primary` 设默认，`@Qualifier` 指定名字 |
| 条件装配 | `@ConditionalOnMissingBean` 是自动配置兜底的关键 |
| 循环依赖 | 用构造器注入会直接暴露，应重新设计而非绕过 |
