# testing-lab · 测试方式对比实验（知识点实验，非正式交付）

> 对应作业 08 任务五 / 任务六。这里的代码是实验用途，可自由增删，不迁入 `delivered`。

## 这个脚手架有什么

- `GreetingService`：一段带分支的业务逻辑（name 为空抛异常）。
- `GreetingController`：`GET /api/greeting?name=xxx` 返回 `{"message":"Hello, xxx"}`。
- `src/test` 下两个**测试骨架**，方法体是 TODO，等你补：
  - `GreetingServiceUnitTest`：方式一，纯单元测试，不启动 Spring。
  - `GreetingControllerSliceTest`：方式二，`@WebMvcTest` + `MockMvc` + `@MockitoBean` 切片测试。

初始状态可编译、可运行（TODO 的空测试会直接通过）。补全 TODO 后再观察两种方式的差异。

## 怎么做

```bash
# 编译 + 跑测试（离线；首次联网可去掉 -o）
mvn -o test        # 或 mvn test（联网首次拉依赖）
```

补全两个测试骨架的 TODO，然后回答：

1. 纯单元测试能测到什么？测不到什么（路径映射？参数绑定 400？JSON 序列化）？
2. `@WebMvcTest` 相比纯单元，多测到了哪些"框架的活"？代价是什么（启动上下文、变慢）？
3. `@MockitoBean` 把 mock 放进 Spring 上下文，纯单元的 `mock()` 没进上下文——这个区别导致了什么？
4. 什么场景纯单元就够、什么场景必须上切片？

## 版本提醒

Spring Boot 4 已移除 `@MockBean`，本实验用 `@MockitoBean`
（`org.springframework.test.context.bean.override.mockito.MockitoBean`）。

## 进阶（任务六）

- 用 AssertJ 的 `extracting` / `satisfies` / 集合断言改写单元测试，体会可读性。
- 有余力可试 Spring 6.2+ 的 `MockMvcTester`（AssertJ 风格 MockMvc）改写切片用例，对比经典 `andExpect` 链。

完成后保留还是删除由你决定，不会自动并入正式工程。
