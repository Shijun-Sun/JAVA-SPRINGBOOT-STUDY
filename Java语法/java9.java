/**
 * Java 9 新特性学习文件
 * 重点：模块化与集合/Stream 小增强
 *
 * 学习顺序：
 *   1. 看知识讲解区
 *   2. 完成练习留空区（TODO）
 *   3. 发给 Copilot review
 *   4. 根据反馈修改
 */

// ============================================================
// 【知识讲解区】
// ============================================================

// ---- 1. JPMS 模块系统（module-info.java）----
//
// • Java 9 引入了模块系统（Project Jigsaw），将代码组织为"模块"。
// • 每个模块根目录放一个 module-info.java，声明模块名、依赖与导出。
// • 关键字：module / requires / exports / opens / uses / provides
//
// 最小示例（module-info.java 文件内容，不在此文件运行）：
//
//   module com.example.myapp {
//       requires java.base;          // 依赖 java.base 模块（默认隐式依赖）
//       requires java.sql;           // 依赖 java.sql 模块
//       exports com.example.service; // 对外暴露该包
//   }
//
// • 模块系统主要用于大型项目/框架，日常 Spring Boot 项目中不强制使用。
// • 了解概念即可，重点精力放在后续特性。


// ---- 2. 集合工厂方法：List.of / Set.of / Map.of ----
//
// • Java 9 之前创建不可变集合很繁琐，9 开始提供工厂方法。
// • 返回的集合是"不可变"的，不能 add / remove / set。
// • 不允许存放 null 元素。
//
// 示例：
//   List<String> list = List.of("A", "B", "C");
//   Set<Integer> set  = Set.of(1, 2, 3);
//   Map<String, Integer> map = Map.of("a", 1, "b", 2);
//   Map<String, Integer> map2 = Map.ofEntries(
//       Map.entry("a", 1),
//       Map.entry("b", 2)
//   );


// ---- 3. Optional 增强 ----
//
// Java 9 新增了三个方法：
// • ifPresentOrElse(consumer, runnable)  — 有值时执行 consumer，无值时执行 runnable
// • or(supplier)                         — 无值时返回另一个 Optional
// • stream()                             — 将 Optional 转为只含 0 或 1 个元素的 Stream
//
// 示例：
//   Optional<String> opt = Optional.of("hello");
//   opt.ifPresentOrElse(
//       v -> System.out.println("有值: " + v),
//       () -> System.out.println("无值")
//   );
//
//   Optional<String> fallback = Optional.<String>empty()
//       .or(() -> Optional.of("默认值"));


// ---- 4. Stream 增强 ----
//
// Java 9 新增了四个方法：
// • takeWhile(predicate)  — 从头开始取，遇到不满足条件的元素就停止
// • dropWhile(predicate)  — 从头开始跳过满足条件的元素，直到遇到不满足的
// • ofNullable(value)     — 允许 null，null 时返回空 Stream
// • iterate(seed, hasNext, next) — 带终止条件的迭代（三参数版本）
//
// 示例：
//   List<Integer> result = Stream.of(1, 2, 3, 4, 5)
//       .takeWhile(n -> n < 4)   // 结果：[1, 2, 3]
//       .collect(Collectors.toList());
//
//   Stream.iterate(0, n -> n < 10, n -> n + 2)
//       .forEach(System.out::println); // 0 2 4 6 8


// ---- 5. 私有接口方法 ----
//
// • Java 8 允许接口有 default / static 方法。
// • Java 9 允许接口有 private 方法，用于复用接口内部逻辑，不对外暴露。
//
// 示例：
//   interface Greeter {
//       default void greetMorning() { printGreet("早上好"); }
//       default void greetEvening() { printGreet("晚上好"); }
//       private void printGreet(String msg) {
//           System.out.println("[Greeter] " + msg);
//       }
//   }


// ============================================================
// 【练习留空区】
// ============================================================

import java.util.*;
import java.util.stream.*;

public class java9 {

    public static void main(String[] args) {

        // ---- 练习 1：集合工厂方法 ----
        // 要求：
        //   (1) 用 List.of 创建一个包含 3 个城市名称的不可变列表，并打印。
        //   (2) 用 Map.of 创建一个包含 3 个国家-首都键值对的不可变 Map，并打印。
        //   (3) 尝试向 List 中添加元素，观察并用注释说明抛出的异常类型。

        // TODO: 练习 1 在此处实现


        // ---- 练习 2：Optional 增强 ----
        // 要求：
        //   (1) 创建一个有值的 Optional<String>，用 ifPresentOrElse 打印"有值：xxx"。
        //   (2) 创建一个空的 Optional<String>，用 or() 提供默认值"未知"，并打印结果。
        //   (3) 用 stream() 将一个非空 Optional 转为 Stream，并打印元素个数。

        // TODO: 练习 2 在此处实现


        // ---- 练习 3：Stream 增强 ----
        // 要求：
        //   (1) 有一个整数列表 [1, 2, 3, 4, 5, 6]，用 takeWhile 取出所有小于 4 的元素并打印。
        //   (2) 同上列表，用 dropWhile 跳过所有小于 4 的元素并打印剩余元素。
        //   (3) 用三参数 Stream.iterate 生成 1~10 内所有奇数并打印。

        // TODO: 练习 3 在此处实现


        // ---- 练习 4：私有接口方法 ----
        // 要求：
        //   (1) 在此文件下方定义一个接口 Logger，包含：
        //       - default void logInfo(String msg)  → 打印 [INFO] msg
        //       - default void logError(String msg) → 打印 [ERROR] msg
        //       - private void log(String level, String msg) → 供上面两个方法复用
        //   (2) 写一个实现类 ConsoleLogger 实现该接口。
        //   (3) 在 main 中实例化 ConsoleLogger，分别调用 logInfo 和 logError。

        // TODO: 练习 4 在此处实现

    }

}


// ============================================================
// 【AI Review 区】
// ============================================================
//
// 当你完成练习后，把代码发给 Copilot 并说："请 review 我的 java9 练习"
//
// 期望检查维度：
//   1. 集合工厂方法返回的不可变性是否正确理解（异常类型是否写对）
//   2. Optional 三个新方法是否用法正确
//   3. takeWhile / dropWhile 语义是否理解准确（特别是有序 vs 无序流的区别）
//   4. 私有接口方法是否真正复用了逻辑，而不是在 default 方法中重复写
//   5. 命名是否清晰，中文注释是否说明了用途
