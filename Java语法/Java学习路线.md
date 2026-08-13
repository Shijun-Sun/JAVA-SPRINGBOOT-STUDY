# Java 基础语法复习与后续版本新特性学习路线

> 适合对象：已经有 Java 7/8 基础，希望系统复习 Java 基础语法，并按版本学习 Java 9 ~ 21 的新语法与新特性。

---

## 1. 学习目标

### 第一阶段：复习 Java 基础语法
把 Java 7/8 时代最重要的语法和核心 API 重新梳理一遍，目标是“能写、能看懂、能重构旧代码”。

### 第二阶段：按版本学习新语法、新特性
从 Java 9 开始，按版本理解语言演进，重点掌握能提升编码质量和开发效率的特性。

### 第三阶段：和 Spring Boot 3 / 现代 Java 技术栈衔接
最终把学习成果落到 Java 17 / 21 + Spring Boot 3 的现代开发环境里。

---

## 2. Java 基础语法复习清单

### 2.1 语言基础
- 变量、数据类型、字面量
- 运算符、类型转换、自动装箱拆箱
- 流程控制：`if` / `switch` / `for` / `while` / `do-while`
- 数组、可变参数
- 方法：参数传递、重载、返回值

### 2.2 面向对象
- 类与对象
- 构造方法、初始化顺序
- 封装、继承、多态
- `this`、`super`
- `static`、`final`
- 抽象类、接口
- 内部类、匿名类、lambda 基础

### 2.3 常用核心 API
- `Object`、`String`、`StringBuilder`
- `Math`、`BigDecimal`
- `Date` / `Calendar` / `java.time`
- 包装类、枚举
- 正则表达式

### 2.4 集合与泛型
- `List` / `Set` / `Map`
- `Iterator`、`Comparable`、`Comparator`
- 泛型类、泛型方法、通配符
- 集合常见实现与使用场景

### 2.5 异常与 IO
- 异常体系、受检异常 / 非受检异常
- `try-catch-finally`、`try-with-resources`
- 字节流、字符流、缓冲流
- 文件操作、序列化基础

### 2.6 并发基础
- `Thread`、`Runnable`、`Callable`
- 线程安全、synchronized、volatile
- 线程池、并发工具类
- `Future`、`CompletableFuture` 基础

### 2.7 注解与反射
- 自定义注解
- 元注解
- 反射基础与常见应用
- 动态代理基础

---

## 3. Java 后续版本新语法 / 新特性路线

### 3.1 Java 9
重点：模块化与集合/Stream 小增强。
- JPMS 模块系统（`module-info.java`）
- `List.of` / `Set.of` / `Map.of`
- `Optional`、`Stream` 小增强
- 私有接口方法

### 3.2 Java 10
重点：局部变量类型推断。
- `var` 局部变量类型推断
- `copyOf` 系列 API

### 3.3 Java 11
重点：生产可用的重要增强。
- 字符串 API 增强：`isBlank`、`lines`、`strip`、`repeat`
- `var` 在 lambda 参数中的使用
- `Files.readString` / `Files.writeString`
- HTTP Client 标准化

### 3.4 Java 12 ~ 16
重点：表达力提升。
- `switch` 表达式（逐步演进）
- 文本块（Text Blocks）
- 记录类前置演进
- 密封类前置演进
- 模式匹配相关特性逐步落地

### 3.5 Java 17
重点：长期支持版本，现代 Java 的分水岭。
- Records
- Sealed Classes
- Pattern Matching for `instanceof`
- Text Blocks 正式可用
- 适合作为 Spring Boot 3 的标准基础版本

### 3.6 Java 18 ~ 20
重点：为 21 做准备。
- 更完整的模式匹配演进
- `switch` 模式匹配增强
- 轻量并发与预览特性跟进

### 3.7 Java 21
重点：当前非常值得掌握的 LTS。
- Virtual Threads（虚拟线程）
- Structured Concurrency（结构化并发，视版本支持情况关注）
- `switch` 模式匹配继续增强
- 适合高并发服务端开发

---

## 4. 建议学习顺序

1. 先复习 Java 基础语法与集合、异常、并发基础。
2. 再按 Java 9 / 10 / 11 快速过一遍“生产常用特性”。
3. 接着重点攻克 Java 17：Records、Sealed Classes、Pattern Matching。
4. 最后学习 Java 21：Virtual Threads 等并发新能力。

---

## 5. 每阶段建议练习

### 基础复习阶段
- 用旧写法重写 3~5 个常见小题：学生管理、订单统计、字符串处理。
- 手写一个集合工具类，练习泛型和迭代器。
- 写一个多线程小例子，观察线程安全问题。

### Java 9~11 阶段
- 把旧代码改写成 `var`、`List.of()`、`String.isBlank()`。
- 用 `HttpClient` 请求一个公开 API。
- 用 `Files.readString` / `writeString` 重构文件操作代码。

### Java 17 阶段
- 用 `record` 重写 DTO / VO。
- 用 `sealed` 表达受限继承层次。
- 用模式匹配简化 `instanceof` 判断。

### Java 21 阶段
- 写一个高并发请求模拟程序，体会虚拟线程优势。
- 比较传统线程池和虚拟线程的代码风格与资源消耗。

---

## 6. 推荐的 workspace 组织方式

你当前的目录已经适合继续扩展，建议这样使用：

- `Java语法/grammer.java`：放语法 demo、示例代码入口
- `Java语法/Java学习路线.md`：放总纲与版本路线
- `笔记/学习指导.md`：放 Spring Boot / 项目实战路线
- `笔记/学习笔记.md`：放每天学习记录、复盘、踩坑总结

如果后续内容变多，可以继续拆成：
- `Java语法/基础语法.md`
- `Java语法/Java9.md`
- `Java语法/Java17.md`
- `Java语法/Java21.md`

---

## 7. 实用建议

- 不要再把重点放在 Java 7/8 的旧式写法上，尤其是过时集合工具、老旧日期 API、匿名内部类替代 lambda 的部分。
- 把 Java 17 作为现代 Java 的主战场，把 Java 21 作为进阶目标。
- 学新特性时，优先理解“为什么出现”，再记语法。
- 每学一个特性，都配一个最小 demo，效果会比只看文章好很多。

---

## 8. 可执行学习节奏

### 2 周复习版
- 第 1 周：基础语法、面向对象、集合、异常、IO
- 第 2 周：并发、泛型、注解、Java 9~11 快速浏览、Java 17 重点学习

### 1 个月加强版
- 第 1 周：基础语法 + 面向对象
- 第 2 周：集合、异常、IO、泛型
- 第 3 周：并发 + Java 9~11
- 第 4 周：Java 17 + Java 21 重点特性

---

## 9. 下一步建议

如果你愿意，我可以继续帮你做两件事之一：

1. 直接把 `grammer.java` 补成一个“Java 基础 + 新特性 demo 集合”；
2. 把这份路线进一步拆成“30 天学习计划”，每天学什么、练什么都列出来。
