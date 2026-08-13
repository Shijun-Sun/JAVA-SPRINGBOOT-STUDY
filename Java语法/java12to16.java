/**
 * Java 12~16 新特性学习文件
 * 重点：表达力提升
 *
 * 涉及版本：Java 12 / 13 / 14 / 15 / 16
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

// ---- 1. switch 表达式（Java 14 正式） ----
//
// 传统 switch 是语句（statement），Java 14 起 switch 可以作为表达式（expression）使用。
//
// 新语法要点：
// • 使用 -> 代替 case xxx:，省去 break，不会贯穿（fall-through）。
// • switch 表达式必须覆盖所有情况（编译器检查），或提供 default。
// • 可以用 yield 在 case 块中返回值（多语句时使用）。
//
// 示例（旧式 switch 语句）：
//   int day = 3;
//   String name;
//   switch (day) {
//       case 1: name = "Monday"; break;
//       case 2: name = "Tuesday"; break;
//       default: name = "Other";
//   }
//
// 示例（新式 switch 表达式）：
//   String name = switch (day) {
//       case 1 -> "Monday";
//       case 2 -> "Tuesday";
//       default -> "Other";
//   };
//
// 示例（yield 多语句）：
//   int result = switch (day) {
//       case 1, 7 -> 0;       // 合并多个 case
//       default -> {
//           int v = day * 2;
//           yield v;          // 用 yield 返回值
//       }
//   };


// ---- 2. 文本块 Text Blocks（Java 15 正式） ----
//
// • 用 """ 包裹多行字符串，不需要 \n 和字符串拼接。
// • 编译器自动处理缩进（以结尾 """ 的列位置为基准去除公共缩进）。
// • 常用于 SQL、JSON、HTML 等多行内容。
//
// 示例：
//   String json = """
//           {
//               "name": "Alice",
//               "age": 30
//           }
//           """;
//
//   String sql = """
//           SELECT *
//           FROM users
//           WHERE age > 18
//           """;
//
// • 文本块末尾 """ 的位置决定是否包含最后一个换行符。


// ---- 3. Records（Java 16 正式） ----
//
// • record 是一种特殊的类，专门用来承载不可变数据。
// • 编译器自动生成：构造方法、getter、equals、hashCode、toString。
// • 字段默认 private final，不能添加实例字段（可加静态字段）。
// • 常用于替代 DTO / VO / 值对象。
//
// 语法：
//   record Point(int x, int y) {}
//
// 等价于：
//   final class Point {
//       private final int x;
//       private final int y;
//       public Point(int x, int y) { this.x = x; this.y = y; }
//       public int x() { return x; }   // getter 方法名与字段名相同（无 get 前缀）
//       public int y() { return y; }
//       // 还自动生成 equals / hashCode / toString
//   }
//
// 可以在 record 中添加：
//   - 自定义紧凑构造方法（compact constructor）用于参数校验
//   - 静态方法、静态字段
//   - 实例方法


// ---- 4. instanceof 模式匹配（Java 16 正式） ----
//
// • 传统写法：先 instanceof 判断，再强制转型，啰嗦且易错。
// • 新写法：instanceof 直接绑定变量，无需再强制转型。
//
// 旧写法：
//   if (obj instanceof String) {
//       String s = (String) obj;
//       System.out.println(s.length());
//   }
//
// 新写法：
//   if (obj instanceof String s) {
//       System.out.println(s.length()); // s 已经是 String 类型
//   }
//
// 可以结合条件：
//   if (obj instanceof String s && s.length() > 5) {
//       System.out.println("长字符串: " + s);
//   }


// ---- 5. Sealed Classes 预览（Java 17 正式，此处先了解概念）----
//
// • sealed 关键字限制哪些类可以继承/实现当前类/接口。
// • 用 permits 列出允许的子类。
// • 子类必须是 final / sealed / non-sealed 之一。
// • 与 switch 模式匹配配合，实现穷举检查。
//
// 示例（Java 17+ 可运行）：
//   sealed interface Shape permits Circle, Rectangle {}
//   record Circle(double radius) implements Shape {}
//   record Rectangle(double w, double h) implements Shape {}


// ============================================================
// 【练习留空区】
// ============================================================

public class java12to16 {

    // ---- 支持练习 3 的 record 定义 ----
    // TODO: 在此处定义一个 record Student，包含字段 name(String) 和 score(int)
    //       并在紧凑构造方法中校验 score 必须在 0~100 之间，否则抛出 IllegalArgumentException


    public static void main(String[] args) {

        // ---- 练习 1：switch 表达式 ----
        // 要求：
        //   (1) 用新式 switch 表达式，根据月份数字（1~12）返回所属季节
        //       （1~3 春，4~6 夏，7~9 秋，10~12 冬），并打印。
        //   (2) 用 yield 在 default 分支中打印一行提示日志后返回 "未知季节"。

        // TODO: 练习 1 在此处实现


        // ---- 练习 2：文本块 ----
        // 要求：
        //   (1) 用文本块定义一个 JSON 字符串，包含 name、age 两个字段，并打印。
        //   (2) 用文本块定义一段 SQL：SELECT name, age FROM students WHERE score > 90，并打印。
        //   (3) 观察并用注释说明：结尾 """ 不同缩进对输出内容的影响。

        // TODO: 练习 2 在此处实现


        // ---- 练习 3：Records ----
        // 要求：
        //   (1) 实例化上方定义的 Student record，score 给一个合法值，打印该对象。
        //   (2) 打印 name() 和 score() getter 的返回值。
        //   (3) 创建两个相同内容的 Student，用 equals 比较并打印结果。
        //   (4) 尝试传入 score = 150，用注释说明结果。

        // TODO: 练习 3 在此处实现


        // ---- 练习 4：instanceof 模式匹配 ----
        // 要求：
        //   (1) 有一个 Object 类型的变量，分别赋值为字符串、整数、List<String>。
        //   (2) 写一个方法 describe(Object obj)，用模式匹配分别处理三种类型并打印描述。
        //   (3) 在 main 中调用 describe，分别传入上面三种值。

        // TODO: 练习 4 在此处实现

    }


    // TODO: 在此处定义 describe 方法（练习 4 用）

}


// ============================================================
// 【AI Review 区】
// ============================================================
//
// 当你完成练习后，把代码发给 Copilot 并说："请 review 我的 java12to16 练习"
//
// 期望检查维度：
//   1. switch 表达式是否覆盖了所有 case，有无遗漏导致编译错误
//   2. yield 是否只在多语句 case 块中使用
//   3. 文本块缩进是否理解正确（公共缩进去除规则）
//   4. record 的 getter 命名是否用的是字段名（无 get 前缀）
//   5. record 紧凑构造方法校验逻辑是否正确
//   6. instanceof 模式匹配绑定变量的作用域是否清晰
