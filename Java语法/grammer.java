// Java 语法示例入口
public class grammer {
    public static void main(String[] args) {
        System.out.println("Java grammar demo placeholder");

        // ====================
        // 1. Java 基础语法复习清单
        // ====================
        // 1.1 语言基础
        // - 变量、数据类型、字面量
        // - 运算符、类型转换、自动装箱拆箱
        // - 流程控制：if / switch / for / while / do-while
        // - 数组、可变参数
        // - 方法：参数传递、重载、返回值
        //
        // 1.2 面向对象
        // - 类与对象
        // - 构造方法、初始化顺序
        // - 封装、继承、多态
        // - this、super
        // - static、final
        // - 抽象类、接口
        // - 内部类、匿名类、lambda 基础
        //
        // 1.3 常用核心 API
        // - Object、String、StringBuilder
        // - Math、BigDecimal
        // - Date / Calendar / java.time
        // - 包装类、枚举
        // - 正则表达式
        //
        // 1.4 集合与泛型
        // - List / Set / Map
        // - Iterator、Comparable、Comparator
        // - 泛型类、泛型方法、通配符
        // - 集合常见实现与使用场景
        //
        // 1.5 异常与 IO
        // - 异常体系、受检异常 / 非受检异常
        // - try-catch-finally、try-with-resources
        // - 字节流、字符流、缓冲流
        // - 文件操作、序列化基础
        //
        // 1.6 并发基础
        // - Thread、Runnable、Callable
        // - 线程安全、synchronized、volatile
        // - 线程池、并发工具类
        // - Future、CompletableFuture 基础
        //
        // 1.7 注解与反射
        // - 自定义注解
        // - 元注解
        // - 反射基础与常见应用
        // - 动态代理基础

        // ====================
        // 2. Java 后续版本新语法 / 新特性
        // ====================
        // 2.1 Java 9
        // - JPMS 模块系统（module-info.java）
        // - List.of / Set.of / Map.of
        // - Optional、Stream 小增强
        // - 私有接口方法
        //
        // 2.2 Java 10
        // - var 局部变量类型推断
        // - copyOf 系列 API
        //
        // 2.3 Java 11
        // - 字符串 API 增强：isBlank、lines、strip、repeat
        // - var 在 lambda 参数中的使用
        // - Files.readString / writeString
        // - HTTP Client 标准化
        //
        // 2.4 Java 12 ~ 16
        // - switch 表达式
        // - 文本块（Text Blocks）
        // - 记录类前置演进
        // - 密封类前置演进
        // - 模式匹配相关特性逐步落地
        //
        // 2.5 Java 17
        // - Records
        // - Sealed Classes
        // - Pattern Matching for instanceof
        // - Text Blocks 正式可用
        //
        // 2.6 Java 18 ~ 20
        // - 更完整的模式匹配演进
        // - switch 模式匹配增强
        // - 轻量并发和预览特性跟进
        //
        // 2.7 Java 21
        // - Virtual Threads（虚拟线程）
        // - Structured Concurrency（结构化并发）
        // - switch 模式匹配继续增强

        // ====================
        // 3. 练习题：Java 基础语法
        // ====================
        // 【练习 1：变量与类型转换】
        // 题目要求：
        // 1) 定义 int、double、String 三种变量
        // 2) 完成基本类型到字符串、字符串到数值的转换
        // 3) 输出转换后的结果
        // 参考知识点：变量、数据类型、类型转换、自动装箱拆箱
        // 实现思路：先定义变量，再使用强制转换或包装类方法进行转换
        // 代码实现：
        int age = 18;
        double score = 98.5;
        String message = "age=" + age + ", score=" + score;
        System.out.println(message);

        // 【练习 2：流程控制】
        // 题目要求：打印 1~10 中的偶数
        // 参考知识点：if、for、取模运算
        for (int i = 1; i <= 10; i++) {
            if (i % 2 == 0) {
                System.out.print(i + " ");
            }
        }
        System.out.println();

        // 【练习 3：数组与可变参数】
        // 题目要求：计算多个整数的总和
        // 参考知识点：数组、可变参数、方法调用
        int sum = sum(1, 2, 3, 4, 5);
        System.out.println("sum=" + sum);

        // ====================
        // 4. 练习题：面向对象
        // ====================
        // 【练习 4：类与对象】
        // 题目要求：定义一个 Student 类，包含 name 和 age，并输出对象信息
        // 参考知识点：类、对象、构造方法、this
        Student student = new Student("Tom", 20);
        System.out.println(student);

        // 【练习 5：继承与多态】
        // 题目要求：定义 Animal 父类和 Cat 子类，演示方法重写
        // 参考知识点：继承、多态、super
        Animal cat = new Cat();
        cat.say();

        // 【练习 6：static 与 final】
        // 题目要求：演示静态成员与常量
        // 参考知识点：static、final
        System.out.println("school=" + Student.SCHOOL_NAME);

        // ====================
        // 5. 练习题：集合与泛型
        // ====================
        // 【练习 7：List 遍历】
        // 题目要求：创建一个学生名单并遍历输出
        // 参考知识点：List、Iterator、增强 for
        java.util.List<String> names = java.util.Arrays.asList("Alice", "Bob", "Cindy");
        for (String name : names) {
            System.out.println(name);
        }

        // 【练习 8：Map 统计】
        // 题目要求：统计字符串中每个字符出现次数
        // 参考知识点：Map、泛型、循环
        java.util.Map<Character, Integer> countMap = new java.util.HashMap<>();
        String text = "hello";
        for (char c : text.toCharArray()) {
            countMap.put(c, countMap.getOrDefault(c, 0) + 1);
        }
        System.out.println(countMap);

        // ====================
        // 6. 练习题：异常与 IO
        // ====================
        // 【练习 9：异常处理】
        // 题目要求：处理除零异常
        // 参考知识点：try-catch-finally、异常体系
        try {
            int result = divide(10, 0);
            System.out.println(result);
        } catch (ArithmeticException e) {
            System.out.println("发生异常：" + e.getMessage());
        }

        // ====================
        // 7. 练习题：Java 新特性
        // ====================
        // 【练习 10：记录类思维】
        // 题目要求：用更少代码表达一个只存数据的对象
        // 参考知识点：Records
        // 说明：这里用普通类模拟，等学习 Java 17 后可替换成 record
        Point point = new Point(3, 5);
        System.out.println(point);

        // 【练习 11：模式匹配】
        // 题目要求：判断对象类型并简化代码
        // 参考知识点：Pattern Matching for instanceof
        printLength("Java 21");
        printLength(123);

        // 【练习 12：虚拟线程思路】
        // 题目要求：写一个并发任务执行示例，后续学习 Java 21 时再替换成虚拟线程
        // 参考知识点：Thread、Runnable、线程池、Virtual Threads
        Runnable task = () -> System.out.println("task running");
        new Thread(task).start();
    }

    private static int sum(int... nums) {
        int result = 0;
        for (int num : nums) {
            result += num;
        }
        return result;
    }

    private static int divide(int a, int b) {
        return a / b;
    }

    private static void printLength(Object obj) {
        if (obj instanceof String str) {
            System.out.println("字符串长度=" + str.length());
        } else {
            System.out.println("不是字符串：" + obj);
        }
    }

    static class Student {
        static final String SCHOOL_NAME = "Java Study School";
        private final String name;
        private final int age;

        Student(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Student{name='" + name + "', age=" + age + "}";
        }
    }

    static class Animal {
        void say() {
            System.out.println("animal say");
        }
    }

    static class Cat extends Animal {
        @Override
        void say() {
            System.out.println("cat say");
        }
    }

    static class Point {
        private final int x;
        private final int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "Point{x=" + x + ", y=" + y + "}";
        }
    }
}
