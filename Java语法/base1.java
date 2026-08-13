// ====================
// AI 指导文件：Java 基础语法第一章
// 章节名称：变量、类型、运算符
// 适用对象：已有 Java 7/8 基础，准备复习基础并衔接新特性学习
// 使用方式：先看“知识讲解区”，再完成“练习留空区”，最后交给 AI review
// ====================

public class base1 {

    public static void main(String[] args) {
        // ====================
        // 1. 知识讲解区
        // ====================

        // 1.1 变量是什么
        // 变量是内存中的一个可变命名空间，用来保存数据。
        // Java 中变量需要先声明类型，再赋值。
        // 示例：
        int age = 18;
        String name = "Tom";

        // 1.2 基本数据类型
        // Java 的 8 种基本类型：
        // - 整数：byte、short、int、long
        // - 浮点数：float、double
        // - 字符：char
        // - 布尔：boolean
        // 基本类型直接存储值，适合高性能、简单数据。

        byte b = 1;
        short s = 2;
        int i = 3;
        long l = 4L;
        float f = 5.0f;
        double d = 6.0;
        char c = 'A';
        boolean flag = true;

        // 1.3 字面量
        // 字面量就是直接写在代码中的常量值。
        // 例如：123、3.14、'a'、"hello"、true、false。
        // 注意：long 需要 L，float 需要 f。

        // 1.4 运算符
        // 算术运算符：+ - * / %
        // 关系运算符：> < >= <= == !=
        // 逻辑运算符：&& || !
        // 赋值运算符：= += -= *= /= %=
        // 自增自减：++ --
        // 三元运算符：条件 ? 表达式1 : 表达式2

        int a = 10;
        int x = 3;
        int add = a + x;
        int sub = a - x;
        int mul = a * x;
        int div = a / x;
        int mod = a % x;
        boolean gt = a > x;
        boolean and = a > 0 && x > 0;
        int max = a > x ? a : x;

        System.out.println("add=" + add);
        System.out.println("sub=" + sub);
        System.out.println("mul=" + mul);
        System.out.println("div=" + div);
        System.out.println("mod=" + mod);
        System.out.println("gt=" + gt);
        System.out.println("and=" + and);
        System.out.println("max=" + max);

        // 1.5 类型转换
        // 自动类型转换：小类型 -> 大类型
        // 强制类型转换：大类型 -> 小类型，需要显式写 (类型)
        int intValue = 100;
        long longValue = intValue;
        double doubleValue = longValue;
        int narrowed = (int) doubleValue;

        System.out.println("longValue=" + longValue);
        System.out.println("doubleValue=" + doubleValue);
        System.out.println("narrowed=" + narrowed);

        // 1.6 自动装箱 / 拆箱
        // 基本类型和包装类之间可以自动转换。
        Integer boxed = 20;
        int unboxed = boxed;
        System.out.println("boxed=" + boxed + ", unboxed=" + unboxed);

        // 1.7 重点理解
        // - int / long / double 的默认类型差异
        // - 运算符优先级
        // - 整数除法会直接截断小数部分
        // - boolean 不能参与数值运算

        // ====================
        // 2. 练习留空区
        // ====================
        // 说明：下面这些题目先不要写答案，留给你完成。
        // 完成后你可以把你的代码发给我，我会按下面的 review 规则帮你检查。

        // 【练习 1：变量声明】
        // 题目：定义你的姓名、年龄、身高、是否在学习 Java 四个变量，并输出它们。
        // 要求：
        // 1) 至少包含 String、int、double、boolean 四种类型
        // 2) 输出时格式清晰
        //
        // 你的答案写在这里：
        // TODO
        String myName = "孙世军";
        int myAge = 35;
        double myHeight = 1.73;
        boolean isLearningJava = true;
        System.out.println("姓名：" + myName);
        System.out.println("年龄：" + myAge);
        System.out.println("身高：" + myHeight);
        System.out.println("是否在学习 Java：" + (isLearningJava ? "是" : "否"));

        // 【练习 2：类型转换】
        // 题目：定义一个 int 和一个 double，完成从 int 到 double、从 double 到 int 的转换。
        // 要求：
        // 1) 输出转换前后结果
        // 2) 观察小数部分丢失的现象
        //
        // 你的答案写在这里：
        int a = 14;
        double b = 3.14;
        double aToDouble = a; // 自动类型转换
        int bToInt = (int) b; // 强制类型转换
        System.out.println("int a = " + a + ", 转换为 double: " + aToDouble);
        System.out.println("double b = " + b + ", 转换为 int: " + bToInt);

        // TODO

        // 【练习 3：运算符】
        // 题目：输入两个整数（你可以直接写死变量值），计算它们的和、差、积、商、余数。
        // 要求：
        // 1) 使用算术运算符
        // 2) 使用三元运算符求较大值
        //
        // 你的答案写在这里：
        // TODO
        int a = 12;
        int b = 5;
        int sum = a +b;

        int difference = a - b;
        int product = a * b;
        double quotient = a / b;
        int remainder = a % b

        int maxNum = a > b ? a: b;



        // 【练习 4：逻辑判断】
        // 题目：判断一个整数是否在 10 到 20 之间（包含 10 和 20）。
        // 要求：
        // 1) 使用关系运算符和逻辑运算符
        // 2) 输出 true / false
        //
        // 你的答案写在这里：
        // TODO

        // ====================
        // 3. AI review 说明
        // ====================
        // 当你完成练习后，把你的答案贴给我时，我会按以下标准 review：
        // - 语法是否正确
        // - 类型是否选择合理
        // - 是否符合题目要求
        // - 是否可以写得更简洁
        // - 是否存在基础概念误用
        //
        // 你也可以直接让我按“只改错、不直接给最终答案”的方式 review。
    }
}
