# 作业 04 · Web 层实战

> **对应讲义**：[M01-04 · Web 层](../学习/04-Web层.md)  
> **预计耗时**：4～6 小时  
> **难度**：中等  
> **正式交付位置**：`homework/delivered/product-service/`  
> **Review 模式**：默认"只指出错误，不直接给答案"

> 在现有 `product-service` 上继续迭代，不要另起工程。  
> 本章的核心产出是能跑通的商品 CRUD，而不是堆代码量。  
> 每完成一个任务都要能启动服务、用 curl 或 HTTPie 验证，不要等到全部写完再跑。

---

## 一、背景设定

商品服务需要对外提供 REST API，让前端和下游服务能管理商品数据。  
本章不接数据库，用内存存储验证完整流程；M02 换 MyBatis-Plus 时只改 Repository 层。

当前代码已有：
- `ProductConfig`（配置对象）
- `Result<T>`（雏形，需要升级）
- `PingController`、`CurrentProductConfigController`
- 空目录：`domain/entity`、`domain/dto`、`domain/vo`、`repository`、`service`

任务是把这个骨架填充成可用的商品服务。

---

## 二、必做任务

### 任务一：升级 `Result<T>` ⭐

当前 `Result<T>` 存在几个问题：
- `fail(T data)` 签名设计模糊，错误不应该是泛型数据；
- `code` 直接用整数，与 HTTP 状态码混淆；
- 没有无数据成功场景的工厂方法；
- 没有 `@JsonInclude`，`data` 为 null 时也会出现在 JSON 里。

要求：

1. 保留 `code`、`message`、`data` 三个字段，调整类型：`code` 改为 `String`；
2. 提供三个工厂方法：
   - `Result.ok(T data)`：有数据的成功，`code` 为 `"SUCCESS"`；
   - `Result.ok()`：无数据的成功；
   - `Result.fail(String code, String message)`：业务失败，无 `data`；
3. `data` 为 null 时不序列化进 JSON（使用 `@JsonInclude`）；
4. 改完后确认 `PingController` 和 `CurrentProductConfigController` 仍然正常编译和运行。

**验收标准：**

- `GET /api/ping` 响应：`{"code":"SUCCESS","message":"ok","data":"pong"}`
- `GET /api/config-info` 响应中 `code` 字段为字符串
- 无 `data` 的响应 JSON 里不出现 `"data":null`

---

### 任务二：建立 Entity、VO 和 Request DTO

为商品建立三层对象，每层只出现在对应的层，不跨越：

**`ProductEntity`**（`domain/entity/`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 唯一标识，由 Repository 生成 |
| `name` | `String` | 商品名称 |
| `sku` | `String` | SKU，全局唯一 |
| `price` | `BigDecimal` | 价格 |
| `category` | `String` | 分类 |
| `createdAt` | `LocalDateTime` | 创建时间 |
| `updatedAt` | `LocalDateTime` | 最后更新时间 |

**`ProductVO`**（`domain/vo/`）：面向客户端的响应对象，不含 `updatedAt`（内部字段）。

**`CreateProductRequest`**（`domain/dto/`）：创建商品的入参，不含 `id`、`createdAt`、`updatedAt`，至少包含：
- `name`：商品名称
- `sku`：SKU
- `price`：价格，`BigDecimal`
- `category`：分类

**要求：**

1. 三个类都使用 `record`；
2. `CreateProductRequest` 上所有字段加校验注解，至少：
   - `name` 非空且长度 1～100；
   - `sku` 非空且符合格式（如字母数字，长度限制自定）；
   - `price` 非 null，且大于 0；
   - `category` 非空；
3. `ProductEntity` 不加 Bean Validation 注解，那是数据库映射对象，不是校验边界；
4. `ProductVO` 字段全部只读，不加任何校验注解。

**验收标准：**

- 三个类编译通过，职责不混用；
- `CreateProductRequest` 校验注解齐全，`message` 用中文；
- `ProductEntity` 干净，无框架注解。

---

### 任务三：实现 InMemoryProductRepository ⭐

在 `repository/` 下实现内存存储，用 `ConcurrentHashMap` 和 `AtomicLong` 模拟数据库。

必须实现的方法：

```
save(ProductEntity)          → ProductEntity（带自增 id）
findById(Long)               → Optional<ProductEntity>
findAll()                    → Collection<ProductEntity>
update(Long, ProductEntity)  → Optional<ProductEntity>（id 不存在时返回 empty）
deleteById(Long)             → boolean（存在并删除返回 true）
existsBySku(String)          → boolean
```

要求：

1. 类加 `@Repository` 注解；
2. `save` 必须生成并填充 `id`，原 Entity 上的 `id` 字段忽略；
3. `findAll` 返回的是**快照**，不是对底层 Map 的视图引用（防止外部修改内部状态）；
4. `update` 接收完整替换的 Entity，直接用 `store.put` 替换，不做字段级 merge；
5. 不在 Repository 里写任何业务逻辑（SKU 重复检查等属于 Service）。

**验收标准：**

- Repository 可以被 Service 正常注入；
- 并发写入不会出现 ID 冲突（AtomicLong 保证）；
- 没有把业务判断放进 Repository。

---

### 任务四：实现 ProductService ⭐

在 `service/` 下实现业务逻辑层，通过构造器注入 Repository。

必须实现：

| 方法签名 | 行为 |
|---|---|
| `ProductVO create(CreateProductRequest)` | SKU 重复时抛异常；创建成功返回 VO |
| `Optional<ProductVO> findById(Long)` | 找不到返回 empty |
| `List<ProductVO> findAll()` | 返回所有商品 VO 列表 |
| `ProductVO update(Long, UpdateProductRequest)` | 找不到时抛异常；更新并返回新 VO |
| `void deleteById(Long)` | 直接删除，不关心是否存在（幂等行为） |

`UpdateProductRequest`（在 `domain/dto/` 下新建）：
- 只包含可更新的字段：`name`、`price`、`category`（SKU 不允许改）；
- 每个字段加校验注解；
- 使用 `record`。

要求：

1. 只通过构造器注入，不用字段注入；
2. SKU 重复时抛 `IllegalArgumentException`，message 包含重复的 SKU 值；
3. 找不到资源时抛 `NoSuchElementException`（`java.util` 自带），message 包含 id；
4. Service 不直接操作 HTTP，不引入任何 `jakarta.servlet` / Spring Web 包；
5. Entity 到 VO 的转换写在 Service 内，不要写进 Entity 或 VO 里。

**验收标准：**

- 创建重复 SKU 时会抛异常；
- 所有方法都有明确的返回类型，无 `void` 返回却靠副作用传值的设计；
- Service 里没有 `@RequestBody`、`HttpServletRequest` 等 Web 注解。

---

### 任务五：实现 ProductController ⭐

在 `controller/` 下新建 `ProductController`，通过构造器注入 `ProductService`。

实现以下接口：

| HTTP 方法 | 路径 | 成功状态码 | 功能 |
|---|---|---|---|
| GET | `/api/v1/products` | 200 | 查询所有商品 |
| GET | `/api/v1/products/{id}` | 200 | 查询单个商品 |
| POST | `/api/v1/products` | **201** | 创建商品 |
| PUT | `/api/v1/products/{id}` | 200 | 更新商品 |
| DELETE | `/api/v1/products/{id}` | **204** | 删除商品 |

要求：

1. `POST` 必须返回 201，不是 200；
2. `DELETE` 返回 204，无响应体，方法返回 `void`；
3. 查询单个时，找不到返回 404 状态码（本章暂时手动处理，下一章用全局异常处理器统一）；
4. 创建和更新的入参加 `@Validated`；
5. 所有成功响应包裹在 `Result<T>` 里（DELETE 除外）；
6. Controller 不写业务逻辑，只做：参数接收 → 调用 Service → 包装响应。

处理 404 的临时方案（第 05 章会替换）：

```java
@GetMapping("/{id}")
public ResponseEntity<Result<ProductVO>> findById(@PathVariable Long id) {
    return productService.findById(id)
            .map(vo -> ResponseEntity.ok(Result.ok(vo)))
            .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Result.fail("PRODUCT_NOT_FOUND", "商品不存在：" + id)));
}
```

**验收标准：**

- 5 个接口均可通过 curl 或 HTTPie 手动验证；
- POST 响应状态码是 201；
- DELETE 响应状态码是 204，Body 为空；
- 查询不存在的 ID 返回 404；
- 创建时传入非法参数（如 `price: -1`）返回 400（Spring 默认行为，暂不要求格式化错误信息）。

---

### 任务六：端到端验证 ⭐

启动服务（激活 dev Profile），依次执行以下操作并记录响应：

**场景一：正常 CRUD 流程**

```bash
# 1. 创建商品 A
POST /api/v1/products
{"name":"iPhone 16","sku":"PHONE-001","price":6999.00,"category":"手机"}

# 2. 创建商品 B
POST /api/v1/products
{"name":"MacBook Pro","sku":"LAPTOP-001","price":12999.00,"category":"笔记本"}

# 3. 查询所有
GET /api/v1/products

# 4. 查询商品 A
GET /api/v1/products/1

# 5. 更新商品 A 价格
PUT /api/v1/products/1
{"name":"iPhone 16","price":6599.00,"category":"手机"}

# 6. 删除商品 B
DELETE /api/v1/products/2

# 7. 再次查询所有，确认只剩商品 A
GET /api/v1/products
```

**场景二：边界与异常**

```bash
# 8. 重复 SKU 创建
POST /api/v1/products
{"name":"iPhone 16 Plus","sku":"PHONE-001","price":7999.00,"category":"手机"}
# 预期：4xx（本章暂时可能是 500，下章修复）

# 9. 查询不存在的 ID
GET /api/v1/products/999
# 预期：404

# 10. 创建时传入空名称
POST /api/v1/products
{"name":"","sku":"TEST-001","price":100.00,"category":"测试"}
# 预期：400

# 11. 创建时价格为负数
POST /api/v1/products
{"name":"测试商品","sku":"TEST-002","price":-1.00,"category":"测试"}
# 预期：400
```

在交付说明中填写每个场景的实际响应状态码和关键响应体。

**验收标准：**

- 场景一的 7 步全部按预期执行；
- 场景二的 9、10、11 步状态码符合预期；
- 场景二的第 8 步如果返回 500，记录原因并说明第 05 章如何修复。

---

## 三、进阶任务（按需选做）

### 任务七：分页查询

为 `GET /api/v1/products` 增加分页和关键字过滤支持：

```
GET /api/v1/products?page=0&size=10&keyword=iPhone
```

要求：

1. 新建 `Page<T>` 通用分页包装类（`common/result/` 下），字段：`items`、`total`、`page`、`size`、`totalPages`；
2. `page` 从 0 开始，`size` 默认 20，最大值从 `ProductConfig` 读取（已有 `maxSize` 配置）；
3. `keyword` 为空时返回所有；不为空时按 `name` 或 `category` 包含匹配（`String.contains`）；
4. `size` 超过配置最大值时返回 400；
5. `ProductService` 增加 `Page<ProductVO> list(int page, int size, String keyword)` 方法；
6. 分页逻辑写在 Service 而不是 Repository。

**验收标准：**

- `GET /api/v1/products?page=0&size=1` 返回第一页，`totalPages` 正确；
- `GET /api/v1/products?keyword=iPhone` 只返回名称含 `iPhone` 的商品；
- `size` 超限时返回 400。

### 任务八：`ProductQuery` 封装查询条件

当查询条件增多时，多个 `@RequestParam` 会让方法签名很长。新建 `ProductQuery`：

```java
// 使用 Spring 的 @ParameterObject（或直接让 Spring 绑定 Query 对象）
public record ProductQuery(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category
) {}
```

要求：

- 研究 Spring MVC 如何将查询参数绑定到对象（hint：`@ModelAttribute` 或无注解直接绑定）；
- 说明 `record` 作为查询对象绑定时需要什么条件；
- 将 Controller 中的多个 `@RequestParam` 替换为 `ProductQuery`。

---

## 四、挑战任务（面试 / 答辩级）

### 任务九：自定义校验注解

标准注解无法表达所有业务规则，例如"SKU 只允许大写字母、数字和连字符"。

实现一个自定义校验注解 `@ValidSku`：

1. 创建注解 `@ValidSku`；
2. 创建对应的 `ConstraintValidator` 实现；
3. 把 `@Pattern` 替换为 `@ValidSku`；
4. 解释为什么不直接用 `@Pattern(regexp="...")` 而要封装成注解（可维护性/语义）。

### 任务十：响应字段命名风格

Spring Boot 默认用 camelCase 序列化字段（`createdAt`）。研究如何统一配置为 snake_case（`created_at`）：

1. 找到全局 Jackson 命名策略配置方式；
2. 切换为 `SNAKE_CASE` 并验证响应；
3. 解释这个改动对现有 `ProductConfig` 配置绑定是否有影响，为什么。

---

## 五、提交检查清单

### 代码结构

- [ ] `ProductEntity`、`CreateProductRequest`、`UpdateProductRequest`、`ProductVO` 均已实现
- [ ] 三类对象职责不混用：Entity 不出现在 Controller 返回值；Request DTO 不流入 Repository
- [ ] `CreateProductRequest` 和 `UpdateProductRequest` 上校验注解齐全，`message` 为中文
- [ ] `InMemoryProductRepository` 实现了所有必要方法，无业务逻辑
- [ ] `ProductService` 只通过构造器注入，无 Web 层依赖
- [ ] `ProductController` 实现 5 个接口，路径前缀 `/api/v1/products`

### HTTP 语义

- [ ] POST 成功返回 201
- [ ] DELETE 成功返回 204，Body 为空
- [ ] 查询不存在资源返回 404
- [ ] 入参校验失败返回 400

### `Result<T>`

- [ ] `code` 字段是语义字符串而非整数
- [ ] `data` 为 null 时不序列化出来
- [ ] `PingController` 和 `CurrentProductConfigController` 仍然正常工作

### 构建

在 `homework/delivered/product-service/` 下执行：

```bash
./mvnw package -DskipTests
```

- [ ] 构建成功，无编译错误
- [ ] 启动后 5 个接口均可访问

---

## 六、交付说明（完成后填写）

```markdown
完成时间：
完成任务：必做 1 / 2 / 3 / 4 / 5 / 6；进阶：；挑战：

### 接口清单
- GET    /api/v1/products
- GET    /api/v1/products/{id}
- POST   /api/v1/products
- PUT    /api/v1/products/{id}
- DELETE /api/v1/products/{id}

### 端到端验证结果（场景一）
1. 创建商品 A：状态码 = ，Body =
2. 创建商品 B：状态码 = ，Body =
3. 查询所有：状态码 = ，数量 =
4. 查询商品 A：状态码 = ，Body =
5. 更新商品 A：状态码 = ，新价格 =
6. 删除商品 B：状态码 =
7. 再次查询所有：数量 =

### 端到端验证结果（场景二）
8. 重复 SKU：状态码 = ，说明 =
9. 不存在 ID：状态码 =
10. 空名称：状态码 =
11. 负价格：状态码 =

### Result<T> 变更说明
- code 字段变更：
- 是否影响现有接口：

### 构建结果
- ./mvnw package -DskipTests：

遇到的问题：
尚未完成或需要 review 的部分：
```

---

## 七、提交后如何请求 Review

完成后直接说：

> Review 作业 04，只指出错误，不直接给答案。

我会按固定格式检查：结论、错误清单、改进建议、架构视角、可选优化、下一步。
