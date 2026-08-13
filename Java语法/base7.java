// ====================
// 学习文件：Java 基础语法第七章
// 章节名称：注解与反射
// 适用对象：已有 Java 7/8 基础，系统复习注解与反射核心机制
// 使用方式：先看"知识讲解区"，再完成"练习留空区"，最后交给 AI review
// ====================

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.Arrays;

public class base7 {

    public static void main(String[] args) throws Exception {

        // ====================
        // 1. 知识讲解区
        // ====================

        // ----------------------------------------
        // 1.1 注解（Annotation）是什么
        // ----------------------------------------
        // 注解是附加在代码元素（类、方法、字段等）上的"元数据标签"。
        // 注解本身不影响逻辑，但可以被编译器或框架在运行时读取并处理。
        // 常见内置注解：
        //   @Override       - 表示方法重写父类方法，编译器会验证
        //   @Deprecated     - 标记已过时的 API
        //   @SuppressWarnings - 压制编译器警告
        //   @FunctionalInterface - 标记函数式接口

        // 示例：@Override 的作用
        // class Animal {
        //     void speak() {}
        // }
        // class Dog extends Animal {
        //     @Override           // 如果方法名拼错，编译器会报错
        //     void speak() { System.out.println("汪汪"); }
        // }


        // ----------------------------------------
        // 1.2 元注解（Meta-Annotation）
        // ----------------------------------------
        // 元注解是"注解的注解"，用来修饰自定义注解的行为。
        //
        // @Target    - 指定注解可以作用在哪些元素上
        //              常用值：ElementType.TYPE（类）、METHOD（方法）、FIELD（字段）
        //
        // @Retention - 指定注解的保留阶段
        //              SOURCE：只在源码，编译后丢弃（如 @Override）
        //              CLASS：编译进 .class，运行时不可见（默认）
        //              RUNTIME：运行时可见，可通过反射读取（最常用于框架）
        //
        // @Documented - 注解会出现在 Javadoc 中
        //
        // @Inherited  - 子类会继承父类的该注解

        // 记忆口诀：想让反射读到注解，必须加 @Retention(RUNTIME)


        // ----------------------------------------
        // 1.3 自定义注解
        // ----------------------------------------
        // 语法：
        //   @interface 注解名 {
        //       类型 属性名() default 默认值;
        //   }
        //
        // 注解的属性类型只能是：基本类型、String、Class、枚举、注解、以上类型的数组
        //
        // 示例：见文件底部的 @MyTag 注解定义

        // 使用自定义注解的演示：
        Class<AnnotatedClass> clazz = AnnotatedClass.class;
        MyTag tag = clazz.getAnnotation(MyTag.class);
        if (tag != null) {
            System.out.println("注解 author = " + tag.author());
            System.out.println("注解 version = " + tag.version());
        }


        // ----------------------------------------
        // 1.4 反射（Reflection）基础
        // ----------------------------------------
        // 反射允许在"运行时"动态获取类的结构信息，并操作对象。
        // 核心类：
        //   java.lang.Class         - 代表一个类的元信息
        //   java.lang.reflect.Field  - 代表字段
        //   java.lang.reflect.Method - 代表方法
        //   java.lang.reflect.Constructor - 代表构造方法
        //
        // 获取 Class 对象的三种方式：
        //   1. 类名.class                     // 编译期已知类型
        //   2. 对象.getClass()                // 已有对象实例
        //   3. Class.forName("全限定类名")    // 运行时按字符串加载（最灵活）

        // 示例：获取 Person 类的 Class 对象
        Class<?> personClass = Class.forName("base7$Person");

        // 获取所有公开方法
        System.out.println("\n--- Person 的公开方法 ---");
        for (Method method : personClass.getMethods()) {
            System.out.println(method.getName());
        }

        // 获取所有声明字段（含私有）
        System.out.println("\n--- Person 的所有字段 ---");
        for (Field field : personClass.getDeclaredFields()) {
            System.out.println(field.getType().getSimpleName() + " " + field.getName());
        }


        // ----------------------------------------
        // 1.5 反射常见应用
        // ----------------------------------------
        // 场景 1：动态创建对象
        Constructor<?> constructor = personClass.getDeclaredConstructor(String.class, int.class);
        Object person = constructor.newInstance("Alice", 25);
        System.out.println("\n--- 反射创建对象 ---");
        System.out.println(person);

        // 场景 2：访问私有字段（框架中常用）
        Field nameField = personClass.getDeclaredField("name");
        nameField.setAccessible(true); // 打破私有访问限制
        String nameValue = (String) nameField.get(person);
        System.out.println("私有字段 name = " + nameValue);

        // 场景 3：动态调用方法
        Method greetMethod = personClass.getMethod("greet");
        greetMethod.invoke(person); // 等同于 person.greet()


        // ----------------------------------------
        // 1.6 动态代理基础
        // ----------------------------------------
        // 动态代理允许在不修改原有类的情况下，在方法调用前/后增加逻辑。
        // Java 内置两种方式：
        //   - JDK 动态代理：基于接口，使用 Proxy.newProxyInstance()
        //   - CGLIB 代理：基于继承，Spring 等框架在底层使用（非 JDK 标准库）
        //
        // JDK 动态代理三要素：
        //   1. 目标接口
        //   2. 目标实现类
        //   3. InvocationHandler（拦截逻辑）

        // 示例：用动态代理给 Greeter 接口加日志
        Greeter realGreeter = new RealGreeter();
        Greeter proxy = (Greeter) Proxy.newProxyInstance(
                realGreeter.getClass().getClassLoader(),    // 类加载器
                new Class[]{Greeter.class},                  // 目标接口列表
                (proxyObj, method, args2) -> {              // InvocationHandler（lambda 简写）
                    System.out.println("[代理] 方法调用前：" + method.getName());
                    Object result = method.invoke(realGreeter, args2);
                    System.out.println("[代理] 方法调用后：" + method.getName());
                    return result;
                }
        );

        System.out.println("\n--- 动态代理演示 ---");
        proxy.sayHello("Bob");
    }


    // ====================
    // 2. 练习留空区
    // ====================

    // 练习 1：自定义注解
    // 要求：
    //   - 定义一个注解 @Column，包含两个属性：name (String) 和 nullable (boolean，默认 true)
    //   - 用 @Target 限定它只能作用在字段上
    //   - 用 @Retention 确保运行时可见
    // TODO: 在此文件末尾写出 @Column 注解的定义


    // 练习 2：反射读取注解
    // 要求：
    //   - 创建一个 UserDO 类，有两个字段：username 和 email，都加上 @Column 注解
    //   - 在 main 方法中（或新建方法），用反射遍历 UserDO 的所有字段，
    //     打印出每个字段名以及对应 @Column 的 name 和 nullable 属性
    // TODO: 实现 UserDO 类和反射读取逻辑

    static class UserDO {

        @Column(name = "userName", nullable = false)
        private String username;
        @Column()
        private String email;
    }

    static void printFiledForUserDO() {
        Class<UserDO> userDOClazz = UserDO.class;
        for(Field field: userDOClazz.getDeclaredFields()) {
            // 原始属性名
            System.out.println(field.getName());
            Column columnTag = field.getAnnotation(Column.class);

            if (columnTag != null) {
                System.out.println(columnTag.name() + columnTag.nullable());
            } else {
                System.out.println("该字段没有@Column注解");
            }
        }
    }



    // 练习 3：动态代理
    // 要求：
    //   - 定义一个接口 Calculator，包含方法 int add(int a, int b)
    //   - 实现类 SimpleCalculator 正常实现 add
    //   - 用 JDK 动态代理包装 SimpleCalculator，在每次调用 add 时：
    //       调用前打印：[LOG] 即将调用 add，参数：a, b
    //       调用后打印：[LOG] add 结果：result
    // TODO: 实现上述接口、实现类与代理逻辑


    // ====================
    // 3. AI Review 区
    // ====================
    // 完成练习后，请将代码发给 AI，按以下维度 review：
    //   1. 注解定义是否完整（元注解是否正确、属性类型是否合法）
    //   2. 反射代码中 setAccessible 的使用是否必要且安全
    //   3. 动态代理的 InvocationHandler 逻辑是否正确处理了返回值
    //   4. 命名与代码可读性
    //   5. 下一步建议：可以尝试用 CGLIB 代理对比，或研究 Spring AOP 的底层原理


    // ====================
    // 辅助类定义区（供知识讲解区示例使用，不要修改）
    // ====================

    /** 演示自定义注解用的目标类 */
    @MyTag(author = "Tom", version = "1.0")
    static class AnnotatedClass {}

    /** 演示反射用的 Person 类 */
    static class Person {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public void greet() {
            System.out.println("Hi, I'm " + name + ", age " + age);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
    }

    /** 演示动态代理用的接口 */
    interface Greeter {
        void sayHello(String name);
    }

    /** Greeter 的真实实现 */
    static class RealGreeter implements Greeter {
        @Override
        public void sayHello(String name) {
            System.out.println("Hello, " + name + "!");
        }
    }
}


// ====================
// 自定义注解定义（文件底部）
// ====================

/**
 * 示例自定义注解：@MyTag
 * 作用位置：类
 * 保留策略：运行时可见
 */
@Target(ElementType.TYPE)       // 只能标注在类上
@Retention(RetentionPolicy.RUNTIME) // 运行时可见（反射可读取）
@Documented
@interface MyTag {
    String author() default "unknown"; // 作者，默认 unknown
    String version() default "1.0";    // 版本号，默认 1.0
}

// TODO: 在此处写出练习 1 的 @Column 注解定义


@Target(ElementType.FIELD)          // 只能标注在字段上
@Retention(RetentionPolicy.RUNTIME) // 运行时可见（反射可读取）
@interface Column {
    // 用空字符串作为"哨兵默认值"：
    //   @Column           → name 为 ""，读取时回退到字段名（如 "id"）
    //   @Column(name="uid") → name 为 "uid"，直接使用
    // 注意：注解 default 必须是编译期常量，无法直接引用字段名，
    //       因此"动态默认"需在读取注解的反射代码里手动处理，JPA / MyBatis 也是这样做的。
    String name() default "";
    boolean nullable() default true;
}
