# 作业 02 · IoC 容器与依赖注入实战

> 在第 01 章搭好的 `product-service` 骨架上继续，不要另起新工程。
> 完成后把代码放到 `homework/delivered/product-service/`，并在本文件末尾填写【交付说明】。

---

## 任务一：改造注入方式（必做）

当前代码里可能用的是字段注入，把所有 `@Autowired` 字段注入改为**构造器注入**。

要求：
- 所有 `@Service` / `@Component` 类的依赖用构造器注入，字段加 `final`
- 使用 Lombok `@RequiredArgsConstructor` 简化代码（pom.xml 里加 Lombok 依赖）
- 改完后启动服务，确认功能正常

验收点：代码里没有 `@Autowired` 标注在字段上。

---

## 任务二：多实现 + `@Primary` / `@Qualifier`（必做）

**场景**：商品图片存储，目前有两种策略，本地存储和对象存储（模拟）。

步骤：
1. 新建接口 `StorageService`，方法 `String upload(String filename)`
2. 新建 `LocalStorageService`（实现返回 `"local://" + filename`）
3. 新建 `OssStorageService`（实现返回 `"oss://" + filename`）
4. 用 `@Primary` 把 `LocalStorageService` 设为默认
5. 在 `ProductService` 里注入 `StorageService`，上传商品图片
6. 新建一个 `BackupService`，用 `@Qualifier` 明确注入 `OssStorageService` 做备份上传
7. 写一个 `/api/products/{id}/upload?filename=xxx` 的接口演示两种上传

验收点：两个接口分别调用了不同的 `StorageService` 实现。

---

## 任务三：生命周期钩子（必做）

**场景**：服务启动时预加载商品分类缓存，服务关闭时打印缓存清理日志。

步骤：
1. 新建 `CategoryCacheService`（`@Service`）
2. 用 `@PostConstruct` 初始化一个 `Map<Integer, String>` 内存缓存（写死几条数据即可）
3. 用 `@PreDestroy` 打印 `"商品分类缓存已清理"` 日志
4. 新建 `/api/categories` 接口，返回缓存里的分类列表
5. 启动服务，观察日志中 `@PostConstruct` 的执行时机；`Ctrl+C` 停服，观察 `@PreDestroy` 的输出

验收点：
- 日志中能看到 `@PostConstruct` 在接受请求之前执行
- 正常关闭服务（`Ctrl+C` 或 SIGTERM）时能看到 `@PreDestroy` 的日志

---

## 任务四：条件装配（必做）

**场景**：根据配置文件决定用哪种通知发送器。

步骤：
1. 新建接口 `NotificationSender`，方法 `void send(String message)`
2. 新建 `EmailNotificationSender`，实现打印 `"[Email] " + message`
3. 新建 `SmsNotificationSender`，实现打印 `"[SMS] " + message`
4. 在 `NotificationConfig`（`@Configuration`）里：
   - `@ConditionalOnProperty(name = "notification.channel", havingValue = "email")` 时注册 Email 实现
   - `@ConditionalOnProperty(name = "notification.channel", havingValue = "sms")` 时注册 SMS 实现
   - `@ConditionalOnMissingBean(NotificationSender.class)` 时注册一个 `NoopNotificationSender`（什么也不做）
5. `application.properties` 先不加 `notification.channel`，启动后验证走的是 Noop
6. 加上 `notification.channel=email`，重启后验证走的是 Email

验收点：能演示切换配置让不同 Bean 生效，日志中能体现。

---

## 任务五（选做）：Prototype + ObjectProvider

**场景**：每次生成报价单时需要一个干净的 `QuoteBuilder` 对象（有状态，不能共用）。

步骤：
1. 新建 `QuoteBuilder`（`@Component @Scope("prototype")`），内部有 `List<String> items`
2. 在 `QuoteService`（Singleton）里，用 `ObjectProvider<QuoteBuilder>` 每次请求都取新的实例
3. 提供 `/api/quotes/build` 接口，连续调用两次，证明每次 `QuoteBuilder` 都是新对象（可以打印 `System.identityHashCode`）

验收点：两次请求打出的 `QuoteBuilder` 的 hashCode 不同。

---

## 交付说明（完成后填写）

```
完成时间：
完成的任务：□ 任务一  □ 任务二  □ 任务三  □ 任务四  □ 任务五（选做）
遇到的问题：

验证结果（粘贴关键日志或接口响应）：
```
