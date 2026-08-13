/**
 * Java 18~20 新特性学习文件
 * 重点：为 Java 21 做准备，模式匹配演进
 *
 * 涉及版本：Java 18 / 19 / 20
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

// ---- 1. switch 模式匹配（Java 21 正式，18~20 为预览）----
//
// • switch 不仅能匹配值，还能匹配类型（配合 instanceof 模式）。
// • 与 sealed class 结合，switch 可穷举所有子类型，编译器检查完整性。
// • 支持 guard（守卫条件）：case Type t when condition
//
// 示例：
//   Object obj = "hello";
//   String result = switch (obj) {
//       case Integer i -> "整数: " + i;
//       case String s when s.length() > 3 -> "长字符串: " + s;
//       case String s -> "短字符串: " + s;
//       case null -> "空值";
//       default -> "其他类型";
//   };
//
// 注意事项：
// • case null 要放在前面，避免 NullPointerException。
// • 带 when 守卫的 case 要放在同类型不带守卫的 case 前面。
// • 对 sealed 类型，编译器要求穷举，不需要 default。


// ---- 2. Record Patterns（Java 21 正式，19~20 为预览）----
//
// • 允许在 instanceof 或 switch 的 case 中直接解构 record 的字段。
// • 无需先获取 record 实例，再调用 getter，可以直接绑定字段变量。
//
// 示例（record 定义）：
//   record Point(int x, int y) {}
//   record Line(Point start, Point end) {}
//
// 示例（instanceof 解构）：
//   Object obj = new Point(3, 4);
//   if (obj instanceof Point(int x, int y)) {
//       System.out.println("x=" + x + ", y=" + y);
//   }
//
// 示例（嵌套解构）：
//   Object line = new Line(new Point(0, 0), new Point(3, 4));
//   if (line instanceof Line(Point(int x1, int y1), Point(int x2, int y2))) {
//       System.out.println("起点: (" + x1 + "," + y1 + ")");
//   }


// ---- 3. Virtual Threads 预览（Java 19/20 预览，21 正式）----
//
// • 虚拟线程是轻量级线程，由 JVM 管理而非 OS 管理。
// • 创建成本极低，可以同时存在数百万个虚拟线程。
// • 核心思想：用同步风格的代码写出高并发程序（不需要 reactive/async）。
//
// 预览 API（Java 19/20）：
//   Thread.ofVirtual().start(() -> System.out.println("虚拟线程"));
//   // 或通过 Executors
//   try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
//       executor.submit(() -> System.out.println("虚拟线程任务"));
//   }
//
// 注：Java 19/20 需加 --enable-preview 参数，Java 21 正式可用。


// ---- 4. 其他 Java 18~20 值得了解的特性 ----
//
// Java 18：
// • UTF-8 成为 Java 的默认字符集（以前因平台而异）
// • Simple Web Server（jwebserver 命令行工具，用于本地测试）
// • @snippet 标签用于 Javadoc 代码片段
//
// Java 19：
// • Structured Concurrency（结构化并发，预览）——把多个并发任务组织为一个工作单元
// • Foreign Function & Memory API（预览）——替代 JNI 调用本地代码
//
// Java 20：
// • Scoped Values（预览）——替代 ThreadLocal 的更安全方案
// • 上述预览特性继续演进


// ============================================================
// 【练习留空区】
// ============================================================

import java.util.List;

// ---- 练习需要的类型定义 ----
// TODO: 定义 sealed interface Expr permits Num, Add, Mul
// TODO: 定义 record Num(int value) implements Expr
// TODO: 定义 record Add(Expr left, Expr right) implements Expr
// TODO: 定义 record Mul(Expr left, Expr right) implements Expr


public class java18to20 {

    public static void main(String[] args) {

        // ---- 练习 1：switch 模式匹配（类型匹配 + 守卫）----
        // 要求：
        //   (1) 有一个 List<Object>，包含 Integer、String、Double、null 等元素。
        //   (2) 遍历列表，用 switch 模式匹配处理每个元素：
        //       - null         → 打印 "空值"
        //       - Integer i when i < 0 → 打印 "负整数: " + i
        //       - Integer i    → 打印 "正整数: " + i
        //       - String s     → 打印 "字符串: " + s
        //       - Double d     → 打印 "小数: " + d
        //       - default      → 打印 "其他"

        // TODO: 练习 1 在此处实现


        // ---- 练习 2：Record Patterns 解构 ----
        // 要求：
        //   (1) 创建几个 Num、Add、Mul 的实例（构造一棵表达式树，如 (2+3)*4）。
        //   (2) 写一个方法 eval(Expr expr)，用 switch + record patterns 递归求值。
        //       - Num(int v)     → 返回 v
        //       - Add(Expr l, Expr r) → 返回 eval(l) + eval(r)
        //       - Mul(Expr l, Expr r) → 返回 eval(l) * eval(r)
        //   (3) 在 main 中求值并打印结果，验证 (2+3)*4 = 20。

        // TODO: 练习 2 在此处实现


        // ---- 练习 3：Virtual Threads 初体验（Java 21 语法，此处先感受）----
        // 要求：
        //   (1) 用 Thread.ofVirtual().start() 启动 5 个虚拟线程，每个线程打印当前线程名和序号。
        //   (2) 在 main 中等待所有虚拟线程完成（可用 Thread.sleep 或 join）。
        //   (3) 用注释说明：虚拟线程与普通线程在创建方式上的区别。
        // 提示：Java 19/20 需要 --enable-preview，Java 21 可直接运行。

        // TODO: 练习 3 在此处实现

    }


    // TODO: 在此处定义 eval 方法（练习 2 用）

}


// ============================================================
// 【AI Review 区】
// ============================================================
//
// 当你完成练习后，把代码发给 Copilot 并说："请 review 我的 java18to20 练习"
//
// 期望检查维度：
//   1. switch 模式匹配中 null case 是否正确处理（放在前面）
//   2. 带 when 守卫的 case 是否放在同类型无守卫的 case 前面
//   3. record patterns 的递归解构是否正确（嵌套 record 解构语法）
//   4. eval 方法的递归逻辑是否正确，有无缺少 case 分支
//   5. 虚拟线程的创建方式是否正确，join 等待是否完整
