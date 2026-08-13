/**
 * Java 17 新特性学习文件
 * 重点：LTS 版本，现代 Java 的分水岭
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

// ---- 1. Records（正式版，复习 + 深化）----
//
// • Java 16 正式，Java 17 可放心使用。
// • record 是不可变数据载体，编译器自动生成样板代码。
// • 支持实现接口，不能继承其他类（隐式继承 java.lang.Record）。
// • 可以有静态工厂方法，常见模式：of(...)。
//
// 示例：
//   record Money(BigDecimal amount, String currency) {
//       // 紧凑构造方法：校验参数
//       Money {
//           Objects.requireNonNull(currency, "currency 不能为 null");
//           if (amount.compareTo(BigDecimal.ZERO) < 0)
//               throw new IllegalArgumentException("金额不能为负");
//       }
//       // 静态工厂方法
//       static Money of(double amount, String currency) {
//           return new Money(BigDecimal.valueOf(amount), currency);
//       }
//       // 自定义实例方法
//       Money add(Money other) {
//           return new Money(this.amount.add(other.amount), this.currency);
//       }
//   }


// ---- 2. Sealed Classes（正式版）----
//
// • 用 sealed 关键字限制继承层次，只有 permits 列出的类才能继承。
// • 子类必须声明为：final（不可再继承）/ sealed（继续限制）/ non-sealed（开放继承）。
// • 与模式匹配的 switch 搭配，编译器可穷举所有子类，无需 default。
// • 常用于表示"有限状态"或"代数数据类型"（ADT）。
//
// 示例：
//   sealed interface Result<T> permits Success, Failure {}
//
//   record Success<T>(T value) implements Result<T> {}
//   record Failure<T>(String reason) implements Result<T> {}
//
//   // 配合 switch 使用（Java 21 模式匹配 switch，此处先理解概念）
//   static <T> String describe(Result<T> result) {
//       return switch (result) {
//           case Success<T> s -> "成功: " + s.value();
//           case Failure<T> f -> "失败: " + f.reason();
//       };
//   }


// ---- 3. Pattern Matching for instanceof（正式版）----
//
// • Java 16 正式，Java 17 可放心使用（已在 java12to16.java 中学过，此处深化）。
// • 可以在 else 分支利用绑定变量的否定语义。
//
// 示例：
//   Object obj = "hello";
//
//   // 绑定变量 s 在 if 块内有效
//   if (obj instanceof String s) {
//       System.out.println(s.toUpperCase());
//   }
//
//   // 否定模式：!instanceof 时在 else 中处理
//   if (!(obj instanceof String s)) {
//       System.out.println("不是字符串");
//   } else {
//       System.out.println(s); // s 在这里可用
//   }


// ---- 4. Text Blocks（正式版，复习）----
//
// • Java 15 正式，Java 17 标准可用。
// • 三个常用转义序列：
//   \s  — 强制保留尾部空格
//   \   — 续行符，消除换行（将多行合并为一行）
//
// 示例：
//   String query = """
//           SELECT id, name \
//           FROM users \
//           WHERE active = true
//           """;
//   // 等价于一行 SQL


// ---- 5. 其他 Java 17 重要变化 ----
//
// • 强封装 JDK 内部 API（--illegal-access 被移除）
// • 随机数 API 增强：RandomGenerator 接口，支持多种算法
// • 伪随机数生成器（PRNG）统一接口
//
// 示例（新随机数 API）：
//   RandomGenerator rng = RandomGeneratorFactory.of("Xoshiro256PlusPlus").create();
//   int randomInt = rng.nextInt(100);


// ============================================================
// 【练习留空区】
// ============================================================

import java.math.BigDecimal;
import java.util.*;

// ---- 练习 2 需要的 sealed 类定义 ----
// TODO: 定义 sealed interface Shape，permits Circle, Rectangle, Triangle
// TODO: 定义 record Circle(double radius) implements Shape
// TODO: 定义 record Rectangle(double width, double height) implements Shape
// TODO: 定义 record Triangle(double base, double height) implements Shape


public class java17 {

    // ---- 练习 1 需要的 record 定义 ----
    // TODO: 定义 record Point(double x, double y)，添加：
    //       - 计算到原点距离的方法 distanceTo()，返回 double
    //       - 静态工厂方法 origin() 返回 (0, 0) 点


    public static void main(String[] args) {

        // ---- 练习 1：Records 深化 ----
        // 要求：
        //   (1) 创建两个 Point 对象，计算并打印各自到原点的距离。
        //   (2) 用 Point.origin() 静态工厂方法创建原点，打印其 toString。
        //   (3) 创建两个相同坐标的 Point，用 equals 比较（应为 true）。

        // TODO: 练习 1 在此处实现


        // ---- 练习 2：Sealed Classes ----
        // 要求：
        //   (1) 分别实例化 Circle、Rectangle、Triangle。
        //   (2) 写一个方法 area(Shape shape)，用 switch 表达式计算面积（
        //       圆：π*r²，矩形：w*h，三角形：0.5*base*height）。
        //   (3) 在 main 中调用 area，打印三种形状的面积。
        //   (4) 注释说明：为什么 switch 中不需要写 default 分支？

        // TODO: 练习 2 在此处实现


        // ---- 练习 3：instanceof 模式匹配综合 ----
        // 要求：
        //   (1) 有一个 List<Object>，混入了 String、Integer、Double 类型的元素。
        //   (2) 遍历列表，用模式匹配分别处理：
        //       - String：打印 "字符串，长度=" + 长度
        //       - Integer：打印 "整数，平方=" + 平方值
        //       - Double：打印 "小数，取整=" + 取整值
        //       - 其他：打印 "未知类型"

        // TODO: 练习 3 在此处实现


        // ---- 练习 4：Text Blocks 进阶 ----
        // 要求：
        //   (1) 用文本块 + 续行符 \ 写一条完整的 SQL（SELECT ... FROM ... WHERE ... ORDER BY ...）
        //       要求：SQL 最终是单行输出，但源码是多行书写。
        //   (2) 打印该 SQL，验证是单行。

        // TODO: 练习 4 在此处实现

    }


    // TODO: 在此处定义 area 方法（练习 2 用）

}


// ============================================================
// 【AI Review 区】
// ============================================================
//
// 当你完成练习后，把代码发给 Copilot 并说："请 review 我的 java17 练习"
//
// 期望检查维度：
//   1. record 的紧凑构造方法与普通构造方法区别是否理解
//   2. sealed + record 组合是否正确（record 实现 sealed interface）
//   3. switch 对 sealed 类型的穷举是否编译通过（无需 default）
//   4. instanceof 模式匹配在循环中的写法是否规范
//   5. 文本块续行符 \ 是否正确使用（\后不能有空格）
