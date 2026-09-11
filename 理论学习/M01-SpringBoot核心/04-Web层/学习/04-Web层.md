# M01-04 · Web 层：REST API 设计、参数绑定、统一响应、Bean Validation

> **预计阅读时间**：90～120 分钟  
> **配套作业**：[作业 04 · Web 层实战](../作业/04-Web层实战.md)  
> **版本基线**：Java 21、Spring Boot 4.1.0（以当前 `product-service/pom.xml` 为准）

> **本章目标**
> - 说清 REST 的约束是什么，以及为什么 `/getProduct?id=1` 不是 REST
> - 掌握 `@RequestParam`、`@PathVariable`、`@RequestBody` 的选择依据
> - 能设计和实现统一响应结构，并理解它的局限
> - 用 Bean Validation 在 DTO 层拦截非法入参，不让 Service 层处理脏数据
> - 理解 `@RestController` 内部做了什么，以及 `@ResponseStatus` 和 HTTP 状态码的关系

---

## 一、REST 是约束，不是规范

### 1.1 REST 的核心思想

REST（Representational State Transfer）描述的是一组设计约束，不是协议，也不是规格书。它的核心思想只有两句：

- **资源导向**：URL 代表资源，不代表动作；
- **HTTP 语义对齐**：用 HTTP 动词（GET/POST/PUT/PATCH/DELETE）表达对资源的操作意图。

把操作名放进 URL 是 RPC 思维，不是 REST：

```text
❌ RPC 风格
GET /getProduct?id=1
POST /createProduct
POST /deleteProduct?id=1

✅ REST 风格
GET    /api/products/1        # 查单条
POST   /api/products          # 创建
PUT    /api/products/1        # 整体替换
PATCH  /api/products/1        # 局部更新
DELETE /api/products/1        # 删除
```

### 1.2 常见 HTTP 状态码

选对状态码是 REST 设计的基本功，不是返回 `200 + {code: 500}` 就算完事：

| 状态码 | 含义 | 典型场景 |
|---|---|---|
| 200 OK | 成功 | GET 查询、更新成功且返回数据 |
| 201 Created | 资源已创建 | POST 成功，配合 `Location` 响应头 |
| 204 No Content | 成功但无响应体 | DELETE 成功 |
| 400 Bad Request | 客户端入参非法 | 校验失败、格式错误 |
| 404 Not Found | 资源不存在 | 查询/更新/删除了不存在的资源 |
| 409 Conflict | 冲突 | 创建时 SKU 重复 |
| 422 Unprocessable Content | 语义非法 | 价格为负数，格式合法但语义错误 |
| 500 Internal Server Error | 服务端异常 | 未预期错误 |

在当前 `product-service` 阶段，关键是：

- 成功创建返回 201，不要返回 200；
- 找不到资源要返回 404，不要返回 200 + `{code: 404, data: null}`；
- 入参校验失败返回 400，不要返回 200 + `{code: 400}`。

### 1.3 URI 命名约定

- 路径段使用小写 kebab-case，例如 `/product-images` 而不是 `/productImages`；
- 集合资源用复数名词：`/products`、`/categories`；
- 嵌套关系：`/products/{id}/images`；
- 查询参数用于筛选、分页、排序：`/products?category=phone&page=0&size=20`；
- 版本放路径或 Header，两者各有权衡，本课程选路径前缀：`/api/v1/products`；
- 不要把动词藏进 URL：`/api/products/search` 通常是 `GET /api/products?keyword=…` 的过度设计。

---

## 二、`@RestController` 做了什么

### 2.1 组合注解展开

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface RestController { ... }
```

`@RestController` = `@Controller` + `@ResponseBody`。

- `@Controller`：把类注册为 Spring MVC 的处理器；
- `@ResponseBody`：把返回值直接写进响应体（经过 `HttpMessageConverter`），而不是解析为视图名。

去掉 `@ResponseBody` 返回 `String`，Spring 会把它当成模板名去找视图。

### 2.2 Jackson 做了什么

当返回值是 Java 对象时，默认的 `MappingJackson2HttpMessageConverter` 会：

1. 把对象序列化成 JSON；
2. 设置 `Content-Type: application/json`。

`record` 类型也能正确序列化，前提是字段名通过 getter 暴露，或者配置了合适的 Jackson 模块。Spring Boot 默认配置 Jackson，不需要手动注册。

**常见陷阱**：`record` 的 `getXxx()` 形式不是标准 getter，Jackson 可能读不到字段值。有两种解决办法：

方式一，使用 `@JsonProperty` 标注每个字段（麻烦）；  
方式二，引入 Jackson Databind 的 `ParameterNamesModule`（Spring Boot 自动配置已经包含，通常无需额外操作）。遇到 `record` 序列化为 `{}` 的情况，先确认 Spring Boot 版本，再检查是否缺少 `jackson-module-parameter-names`。

---

## 三、参数绑定：三种来源，三种注解

Spring MVC 从三个位置获取请求参数：

```text
请求 URL      →  路径变量       @PathVariable
           →  查询参数       @RequestParam
请求 Body     →  JSON/表单体   @RequestBody
请求 Header   →  头部值        @RequestHeader
```

### 3.1 `@PathVariable`：路径段中的标识符

用于唯一标识某个资源，通常是 ID：

```java
@GetMapping("/products/{id}")
public Result<ProductVO> findById(@PathVariable Long id) {
    ...
}
```

- 路径变量名和参数名相同时，注解可以省略 `value`；
- 类型会自动转换（`String` → `Long`），转换失败会得到 400；
- 不要把过滤条件放进路径变量，例如 `GET /products/phone` 里的 `phone` 是分类过滤，应该用查询参数。

### 3.2 `@RequestParam`：查询参数与表单字段

```java
@GetMapping("/products")
public Result<Page<ProductVO>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword
) {
    ...
}
```

关键属性：

| 属性 | 含义 |
|---|---|
| `name` / `value` | 参数名，默认与方法参数同名 |
| `required` | 是否必须，默认 `true` |
| `defaultValue` | 设置 `defaultValue` 后，`required` 自动变为 `false` |

分页、过滤、排序都放查询参数；创建/更新的业务数据放请求体。

### 3.3 `@RequestBody`：JSON 请求体绑定到 DTO

```java
@PostMapping("/products")
@ResponseStatus(HttpStatus.CREATED)
public Result<ProductVO> create(
        @Validated @RequestBody CreateProductRequest request) {
    ...
}
```

- `@RequestBody` 把 JSON body 反序列化为 Java 对象；
- 缺少该注解时，方法参数会从查询参数和路径变量里找，找不到得到 `null` 或报错；
- `@Validated` 触发 Bean Validation 校验；
- 当校验失败时，Spring 抛出 `MethodArgumentNotValidException`，需要全局异常处理器捕获（第 05 章展开）。

### 3.4 三种方式的选择原则

```text
操作对应的资源有唯一标识    → @PathVariable（ID、slug）
补充过滤/分页/排序         → @RequestParam
提交业务数据（创建/更新）  → @RequestBody
```

不要把创建请求的所有字段都拼到查询参数里，也不要把资源 ID 放进请求体：

```java
// ❌ 错误示范
@PostMapping("/products")
public Result<ProductVO> create(
        @RequestParam String name,
        @RequestParam BigDecimal price) { ... }

// ❌ 也不对：ID 应该在路径里
@PutMapping("/products")
public Result<ProductVO> update(
        @RequestBody UpdateProductRequest request) { // request 里含 id 字段
    ...
}

// ✅ 正确
@PutMapping("/products/{id}")
public Result<ProductVO> update(
        @PathVariable Long id,
        @RequestBody UpdateProductRequest request) {
    ...
}
```

---

## 四、DTO 是 Controller 的边界

### 4.1 Entity 不能直接作为入参或返回值

这是新手最常见的错误。直接暴露 Entity 的问题：

1. **过度暴露**：Entity 上可能有创建时间、更新时间、逻辑删除标志等字段，客户端不应该能设置这些；
2. **循环引用**：JPA Entity 有延迟加载的关联对象，序列化时触发额外查询，甚至无限递归；
3. **绑定过紧**：数据库字段改名，API 响应格式就变了，破坏接口兼容性；
4. **校验污染**：Entity 本身有 `@Column` 约束，但适合 API 层的校验逻辑不一样；
5. **安全漏洞**：客户端可能传入不应该被修改的字段（mass assignment 漏洞）。

```text
客户端发送     →  请求 DTO（CreateProductRequest）    →  Service 处理
Service 处理  →  Entity（数据库映射）
Service 返回  →  响应 VO（ProductVO）                →  客户端接收
```

### 4.2 命名约定

| 类型 | 含义 | 命名示例 |
|---|---|---|
| `Entity` | 数据库行的映射，不出现在 API 边界 | `ProductEntity` |
| `DTO` | 跨层传输的中间对象，Service 层可用 | `CreateProductRequest` |
| `VO` | 给客户端的视图对象，Controller 返回 | `ProductVO` |
| `Query` | 封装查询条件，替代多个 `@RequestParam` | `ProductQuery` |

`Request` 后缀和 `DTO` 后缀都可以用来表示请求对象，本课程使用 `Request` 后缀明确语义：

```text
CreateProductRequest  →  创建商品的入参
UpdateProductRequest  →  更新商品的入参
ProductVO             →  商品详情响应
ProductListVO         →  商品列表项响应（字段可能比详情少）
ProductQuery          →  分页/筛选条件
```

### 4.3 用 `record` 建立不可变 DTO

```java
package com.study.mall.product.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank(message = "商品名称不能为空")
        String name,

        @NotBlank(message = "SKU 不能为空")
        String sku,

        @NotNull(message = "价格不能为空")
        @DecimalMin(value = "0.01", message = "价格必须大于 0")
        BigDecimal price,

        @NotBlank(message = "分类不能为空")
        String category
) {}
```

- `record` 天然不可变，不需要手写 getter，Jackson 也能序列化；
- 校验注解直接放在参数上；
- 不需要 `@Data` 或 Lombok（Java 21 + `record` 已够用）。

---

## 五、统一响应结构

### 5.1 为什么需要统一响应

当前 `product-service` 已有一个 `Result<T>`，但有几个地方值得改进：

```java
// 当前实现
public static <T> Result<T> fail(T data) {
    return new Result<>(500, "failed", data);
}
```

问题：

- `fail` 的 `data` 类型设计模糊，错误信息应该是 `String` 还是结构化对象？
- 没有错误码体系，前端无法针对具体错误做处理；
- `code` 直接用 HTTP 状态码整数，不够语义化；
- 没有和 HTTP 状态码对齐（返回 200 但 `code` 是 500，混乱）。

### 5.2 更完善的设计方向

本章先建立一个够用的版本，第 05 章在此基础上补充全局异常处理和错误码体系：

```java
package com.study.mall.product.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Result<T>(
        String code,
        String message,
        T data
) {
    public static <T> Result<T> ok(T data) {
        return new Result<>("SUCCESS", "ok", data);
    }

    public static Result<Void> ok() {
        return new Result<>("SUCCESS", "ok", null);
    }

    public static Result<Void> fail(String code, String message) {
        return new Result<>(code, message, null);
    }
}
```

几个设计点：

- `code` 用语义字符串（`"SUCCESS"`、`"PRODUCT_NOT_FOUND"`），而不是 HTTP 状态码整数；
- `@JsonInclude(NON_NULL)` 让 `data` 为 null 时不出现在 JSON 里，更干净；
- 将 HTTP 状态码交给 HTTP 层（`@ResponseStatus` 或 `ResponseEntity`）管理，`Result` 里不重复；
- `Result<Void>` 用于无数据返回，明确表达"成功但无需数据"。

### 5.3 统一响应的局限

统一响应 `Result<T>` 解决了成功路径的一致性，但有两个问题：

1. **失败路径不统一**：每个 Controller 方法都要自己 `try-catch`，代码重复；
2. **HTTP 状态码和业务错误码脱节**：客户端同时要看 HTTP 状态码和 JSON 里的 `code`。

第 05 章用 `@ControllerAdvice` + `@ExceptionHandler` 统一解决失败路径，本章先保持职责清晰：Controller 只处理正常流程，异常交给后续章节的全局处理器。

### 5.4 `ResponseEntity`：精确控制响应

当需要精确控制 HTTP 状态码和响应头时，返回 `ResponseEntity`：

```java
@PostMapping("/products")
public ResponseEntity<Result<ProductVO>> create(
        @Validated @RequestBody CreateProductRequest request) {

    ProductVO product = productService.create(request);

    URI location = URI.create("/api/products/" + product.id());
    return ResponseEntity
            .created(location)        // 201 Created + Location 头
            .body(Result.ok(product));
}
```

不需要精确控制时，`@ResponseStatus` 更简洁：

```java
@PostMapping("/products")
@ResponseStatus(HttpStatus.CREATED)
public Result<ProductVO> create(
        @Validated @RequestBody CreateProductRequest request) {
    return Result.ok(productService.create(request));
}
```

两者的选择：

- 需要设置响应头（`Location`、自定义头）→ `ResponseEntity`；
- 只需要固定状态码 → `@ResponseStatus`；
- 默认 200 → 什么都不加。

---

## 六、Bean Validation：在 DTO 层拦截非法数据

### 6.1 常用约束注解

| 注解 | 适用类型 | 含义 |
|---|---|---|
| `@NotNull` | 任意类型 | 不能是 null |
| `@NotBlank` | 字符串 | 不能是 null、空串、纯空白 |
| `@NotEmpty` | 字符串、集合、数组 | 不能是 null 或空 |
| `@Size(min, max)` | 字符串、集合 | 长度/元素数范围 |
| `@Min(value)` | 整数类型 | 最小值（含） |
| `@Max(value)` | 整数类型 | 最大值（含） |
| `@DecimalMin(value)` | `BigDecimal` 等 | 最小值，支持小数 |
| `@DecimalMax(value)` | `BigDecimal` 等 | 最大值，支持小数 |
| `@Positive` | 数值 | 必须为正数 |
| `@PositiveOrZero` | 数值 | 必须为正数或 0 |
| `@Pattern(regexp)` | 字符串 | 正则校验 |
| `@Email` | 字符串 | 邮箱格式 |
| `@Valid` | 对象 | 触发嵌套对象的级联校验 |

### 6.2 触发校验：`@Validated` 还是 `@Valid`

```java
// Controller 方法参数上触发 DTO 校验
@PostMapping("/products")
public Result<ProductVO> create(@Validated @RequestBody CreateProductRequest request) { ... }
```

- `@Validated`（Spring 提供）：支持分组校验，通常用在 Controller 方法参数上；
- `@Valid`（Jakarta 标准）：不支持分组，但在嵌套对象上触发级联校验必须用它。

嵌套对象的级联校验：

```java
public record CreateProductRequest(
        @NotBlank String name,
        @NotNull @Valid PricingInfo pricing   // @Valid 触发 PricingInfo 内部校验
) {
    public record PricingInfo(
            @NotNull @DecimalMin("0.01") BigDecimal price,
            @NotBlank String currency
    ) {}
}
```

### 6.3 自定义 `message`

校验失败的默认 message 是英文，业务系统建议自定义：

```java
@NotBlank(message = "商品名称不能为空")
@Size(max = 100, message = "商品名称最多 {max} 个字符")
String name,

@Min(value = 1, message = "分页大小至少为 {value}")
@Max(value = 100, message = "分页大小不超过 {value}")
int size
```

`{value}`、`{min}`、`{max}` 是占位符，会被注解的属性值替换。

### 6.4 校验失败会发生什么

`@RequestBody` 配合 `@Validated` 校验失败时，Spring 抛出 `MethodArgumentNotValidException`。

`@RequestParam` / `@PathVariable` 配合 `@Validated`（加在类上）失败时，抛出 `ConstraintViolationException`。

两个不同的异常，需要在全局异常处理器里分别处理。当前阶段先知道这个结论，第 05 章实现处理器。

---

## 七、分页响应结构

分页接口是高频场景，返回格式要包含足够的翻页信息：

```java
package com.study.mall.product.common.result;

import java.util.List;

public record Page<T>(
        List<T> items,
        long total,
        int page,
        int size,
        int totalPages
) {
    public static <T> Page<T> of(List<T> items, long total, int page, int size) {
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) total / size);
        return new Page<>(items, total, page, size, totalPages);
    }
}
```

Controller 返回：

```java
@GetMapping("/products")
public Result<Page<ProductVO>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword
) {
    return Result.ok(productService.list(page, size, keyword));
}
```

响应体示例：

```json
{
  "code": "SUCCESS",
  "message": "ok",
  "data": {
    "items": [...],
    "total": 42,
    "page": 0,
    "size": 20,
    "totalPages": 3
  }
}
```

---

## 八、内存 CRUD 的完整骨架

本模块 M01 不引入数据库（M02 接入 MyBatis-Plus），但需要建立完整的 CRUD 骨架。用 `ConcurrentHashMap` 模拟存储，保证后续换数据库时只改 Repository 层：

### 8.1 Entity（数据库层的形态）

```java
package com.study.mall.product.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductEntity(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        String category,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
```

### 8.2 Repository（存储访问层）

```java
package com.study.mall.product.repository;

import com.study.mall.product.domain.entity.ProductEntity;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryProductRepository {

    private final Map<Long, ProductEntity> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public ProductEntity save(ProductEntity entity) {
        long id = idSequence.getAndIncrement();
        ProductEntity toStore = new ProductEntity(
                id, entity.name(), entity.sku(), entity.price(),
                entity.category(), entity.createdAt(), entity.updatedAt()
        );
        store.put(id, toStore);
        return toStore;
    }

    public Optional<ProductEntity> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public Collection<ProductEntity> findAll() {
        return store.values();
    }

    public Optional<ProductEntity> update(Long id, ProductEntity patch) {
        if (!store.containsKey(id)) return Optional.empty();
        store.put(id, patch);
        return Optional.of(patch);
    }

    public boolean deleteById(Long id) {
        return store.remove(id) != null;
    }

    public boolean existsBySku(String sku) {
        return store.values().stream().anyMatch(e -> e.sku().equals(sku));
    }
}
```

### 8.3 Service（业务逻辑层）

```java
package com.study.mall.product.service;

import com.study.mall.product.domain.dto.CreateProductRequest;
import com.study.mall.product.domain.entity.ProductEntity;
import com.study.mall.product.domain.vo.ProductVO;
import com.study.mall.product.repository.InMemoryProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final InMemoryProductRepository repository;

    public ProductService(InMemoryProductRepository repository) {
        this.repository = repository;
    }

    public ProductVO create(CreateProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new IllegalArgumentException("SKU 已存在：" + request.sku());
        }
        ProductEntity entity = new ProductEntity(
                null, request.name(), request.sku(),
                request.price(), request.category(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        return toVO(repository.save(entity));
    }

    public Optional<ProductVO> findById(Long id) {
        return repository.findById(id).map(this::toVO);
    }

    public List<ProductVO> findAll() {
        return repository.findAll().stream().map(this::toVO).toList();
    }

    public boolean deleteById(Long id) {
        return repository.deleteById(id);
    }

    private ProductVO toVO(ProductEntity entity) {
        return new ProductVO(
                entity.id(), entity.name(), entity.sku(),
                entity.price(), entity.category(), entity.createdAt()
        );
    }
}
```

### 8.4 VO（响应层的形态）

```java
package com.study.mall.product.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductVO(
        Long id,
        String name,
        String sku,
        BigDecimal price,
        String category,
        LocalDateTime createdAt
) {}
```

### 8.5 Controller 完整示例

```java
package com.study.mall.product.controller;

import com.study.mall.product.common.result.Result;
import com.study.mall.product.domain.dto.CreateProductRequest;
import com.study.mall.product.domain.vo.ProductVO;
import com.study.mall.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<List<ProductVO>> list() {
        return Result.ok(productService.findAll());
    }

    @GetMapping("/{id}")
    public Result<ProductVO> findById(@PathVariable Long id) {
        return productService.findById(id)
                .map(Result::ok)
                .orElse(Result.fail("PRODUCT_NOT_FOUND", "商品不存在：" + id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Result<ProductVO> create(
            @Validated @RequestBody CreateProductRequest request) {
        return Result.ok(productService.create(request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable Long id) {
        productService.deleteById(id);
    }
}
```

注意 `deleteById` 返回 `void` + `@ResponseStatus(NO_CONTENT)`，不包装进 `Result`，HTTP 层已表达了语义。

---

## 九、`@RequestMapping` 拆分与继承

### 9.1 类级别与方法级别结合

```java
@RestController
@RequestMapping("/api/v1/products")      // 公共前缀
public class ProductController {

    @GetMapping                           // GET /api/v1/products
    @GetMapping("/{id}")                  // GET /api/v1/products/{id}
    @PostMapping                          // POST /api/v1/products
    @PutMapping("/{id}")                  // PUT /api/v1/products/{id}
    @PatchMapping("/{id}")                // PATCH /api/v1/products/{id}
    @DeleteMapping("/{id}")               // DELETE /api/v1/products/{id}
}
```

### 9.2 `@RequestMapping` 的 `produces` 与 `consumes`

```java
@PostMapping(
    value = "/products",
    consumes = MediaType.APPLICATION_JSON_VALUE,  // 只接受 JSON body
    produces = MediaType.APPLICATION_JSON_VALUE   // 只返回 JSON
)
```

通常不需要显式声明，因为 Spring Boot 默认配置已经处理。只有服务同时支持 JSON 和 XML 等多种格式时才有意义。

---

## 十、常见问题与排查

### 10.1 返回值是对象但响应是空 `{}`

原因：Jackson 找不到可序列化的字段。

- 检查是否用了 `record`，Jackson Databind 版本是否支持；
- 检查字段是否有 `public` getter（或 `@JsonProperty` 标注）；
- 排查是否意外引入了 `@JsonIgnore`。

### 10.2 `@PathVariable` 传入非数字但参数类型是 `Long`

Spring 会返回 400 Bad Request，异常是 `MethodArgumentTypeMismatchException`。第 05 章统一处理。

### 10.3 `@RequestBody` 校验失败得到 500

原因：没有全局异常处理器，`MethodArgumentNotValidException` 未被捕获，Spring 默认返回 500。第 05 章修复。

### 10.4 POST 成功但状态码是 200 而不是 201

原因：没有加 `@ResponseStatus(HttpStatus.CREATED)`，或者 Handler 返回的是 `ResponseEntity.ok(...)` 而不是 `ResponseEntity.created(...)`。

### 10.5 `@RequestParam` 参数未传时得到 400

原因：`required` 默认为 `true`。需要可选时加 `required = false` 或设置 `defaultValue`。

---

## 十一、小结

| 知识点 | 记住这一句 |
|---|---|
| REST | URL 是资源，HTTP 动词是操作，状态码要对齐 |
| `@RestController` | `@Controller` + `@ResponseBody`，返回值序列化为 JSON |
| `@PathVariable` | 唯一标识符、路径段中的 ID |
| `@RequestParam` | 过滤、分页、排序等可选条件 |
| `@RequestBody` | 提交业务数据，绑定到 DTO |
| DTO 分层 | `Request` / `VO` / `Query` 各司其职，Entity 不出现在 API 边界 |
| 统一响应 | `code` 用语义字符串，HTTP 状态码交给 HTTP 层 |
| Bean Validation | `@Validated` 触发，`@Valid` 做级联，校验失败抛 `MethodArgumentNotValidException` |
| 分页响应 | 包含 `items`、`total`、`page`、`size`、`totalPages` |

---

## 十二、自检问题

不查资料，先口头回答：

1. `GET /deleteProduct?id=1` 为什么不是 REST 风格，正确写法是什么？
2. `@RestController` 去掉 `@ResponseBody` 后，返回 `String` 会发生什么？
3. 创建商品成功应该返回什么 HTTP 状态码，为什么不是 200？
4. `@PathVariable`、`@RequestParam`、`@RequestBody` 分别从请求的哪个位置取值？
5. Entity 直接用作接口返回值有哪几个问题？至少说出三个。
6. `@Validated` 和 `@Valid` 在嵌套对象校验时有什么区别？
7. `Result<T>` 的 `code` 为什么用语义字符串而不是 HTTP 状态码整数？
8. 校验 `@RequestBody` 失败会抛什么异常，校验 `@RequestParam` 失败呢？
9. DELETE 成功应该返回什么，为什么不包装进 `Result`？
10. 什么时候用 `ResponseEntity`，什么时候用 `@ResponseStatus`？

答不上 1、5、6、8 四题的，先回看对应章节再做作业。

---

## 十三、延伸阅读

- [Spring Boot 4.1 · Web MVC](https://docs.spring.io/spring-boot/4.1/reference/web/servlet.html)（英文，官方权威）
- [Spring Framework · Annotated Controllers](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller.html)（英文，官方权威）：参数绑定、返回值、响应体处理完整文档
- [Bean Validation 3.0 Specification](https://beanvalidation.org/3.0/)（英文）：约束注解完整列表与 `@Valid` / `@Validated` 差异
- [RFC 9110 · HTTP Semantics](https://www.rfc-editor.org/rfc/rfc9110)（英文）：HTTP 方法与状态码的权威定义

> 本章基于官方资料重新组织并结合当前 `product-service` 场景编写。Content was rephrased for compliance with licensing restrictions.

---

## 下一步

完成[作业 04 · Web 层实战](../作业/04-Web层实战.md)。重点产出：

1. 商品 CRUD 接口能跑通，HTTP 状态码正确；
2. DTO 层校验生效，非法入参返回 400；
3. 分层结构清晰，Entity / Request / VO 职责不混用；
4. `Result<T>` 升级完成，统一响应结构收敛。
