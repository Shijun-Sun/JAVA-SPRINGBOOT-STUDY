import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;

/**
 * base5.java —— 异常与 IO
 * 对应学习路线 2.5
 * 内容：异常体系、受检/非受检异常、try-catch-finally、try-with-resources、
 *       字节流、字符流、缓冲流、文件操作、序列化基础
 */
public class base5 {

    // =========================================================
    // 一、异常体系
    // =========================================================
    /*
     * Throwable
     *   ├── Error            → JVM 级别严重错误，程序一般不处理（如 OutOfMemoryError）
     *   └── Exception
     *         ├── 受检异常（Checked Exception）  → 编译器强制要求 catch 或 throws
     *         │     如：IOException、SQLException
     *         └── 非受检异常（Unchecked Exception）→ 运行时异常，不强制处理
     *               如：NullPointerException、ArrayIndexOutOfBoundsException
     */

    // 自定义受检异常示例
    static class InsufficientBalanceException extends Exception {
        // 继承 Exception → 受检异常
        public InsufficientBalanceException(String message) {
            super(message);
        }
    }

    // 自定义非受检异常示例
    static class InvalidAgeException extends RuntimeException {
        // 继承 RuntimeException → 非受检异常
        public InvalidAgeException(String message) {
            super(message);
        }
    }

    // =========================================================
    // 二、try-catch-finally
    // =========================================================
    /*
     * 语法结构：
     *   try {
     *       // 可能抛出异常的代码
     *   } catch (ExceptionType e) {
     *       // 处理异常
     *   } finally {
     *       // 无论是否异常都会执行（常用于释放资源）
     *   }
     *
     * 要点：
     * - 可以有多个 catch，从具体到通用排列
     * - finally 块中避免使用 return（会覆盖 try/catch 的 return）
     * - catch 可以用 | 合并多个异常类型（Java 7+）
     */
    static void tryCatchDemo() {
        int[] arr = {1, 2, 3};
        try {
            System.out.println(arr[5]); // 会抛出 ArrayIndexOutOfBoundsException
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("捕获到数组越界异常：" + e.getMessage());
        } finally {
            System.out.println("finally 一定会执行");
        }
    }

    // 多异常合并捕获（Java 7+）
    static void multiCatchDemo(String input) {
        try {
            int value = Integer.parseInt(input); // 可能 NumberFormatException
            int result = 10 / value;             // 可能 ArithmeticException
            System.out.println("结果：" + result);
        } catch (NumberFormatException | ArithmeticException e) {
            // 用 | 合并两个异常类型
            System.out.println("捕获到异常：" + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    // =========================================================
    // 三、try-with-resources（Java 7+）
    // =========================================================
    /*
     * 语法：
     *   try (Resource r = new Resource()) {
     *       // 使用资源
     *   }
     * - 资源类需实现 AutoCloseable 接口
     * - 代码块结束后自动调用 r.close()，无需手写 finally
     * - 推荐用于所有 IO 操作，避免资源泄漏
     */
    static void tryWithResourcesDemo() {
        // 读取文件示例（文件不存在时会捕获异常，不影响编译）
        Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "base5_demo.txt");
        // 先写入内容
        try {
            Files.writeString(tempFile, "Hello, try-with-resources!\n第二行内容");
        } catch (IOException e) {
            System.out.println("写文件失败：" + e.getMessage());
            return;
        }

        // 用 try-with-resources 读取文件
        try (BufferedReader reader = new BufferedReader(new FileReader(tempFile.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("读到：" + line);
            }
        } catch (IOException e) {
            System.out.println("读文件失败：" + e.getMessage());
        }
        // reader.close() 由 JVM 自动调用，无需手写 finally
    }

    // =========================================================
    // 四、字节流（InputStream / OutputStream）
    // =========================================================
    /*
     * 字节流以 byte 为单位读写，适合处理二进制文件（图片、音频等）
     * 核心类：
     *   FileInputStream / FileOutputStream   → 文件字节流
     *   BufferedInputStream / BufferedOutputStream → 带缓冲，提升性能
     */
    static void byteStreamDemo() throws IOException {
        Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "base5_bytes.bin");

        // 写入字节
        try (FileOutputStream fos = new FileOutputStream(tempFile.toFile())) {
            byte[] data = "字节流写入测试".getBytes(StandardCharsets.UTF_8);
            fos.write(data);
            System.out.println("字节流写入完成，字节数：" + data.length);
        }

        // 读取字节
        try (FileInputStream fis = new FileInputStream(tempFile.toFile())) {
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            System.out.println("字节流读取内容：" + new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
        }
    }

    // =========================================================
    // 五、字符流（Reader / Writer）
    // =========================================================
    /*
     * 字符流以 char 为单位读写，适合处理文本文件
     * 核心类：
     *   FileReader / FileWriter           → 文件字符流（默认系统编码，不推荐直接用）
     *   InputStreamReader / OutputStreamWriter → 可指定编码（推荐）
     *   BufferedReader / BufferedWriter   → 带缓冲，支持按行读写
     */
    static void charStreamDemo() throws IOException {
        Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "base5_chars.txt");

        // 写入字符（指定 UTF-8 编码）
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile.toFile()), StandardCharsets.UTF_8))) {
            writer.write("第一行：字符流写入");
            writer.newLine(); // 写入换行符
            writer.write("第二行：中文不乱码");
        }

        // 读取字符
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(tempFile.toFile()), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("字符流读到：" + line);
            }
        }
    }

    // =========================================================
    // 六、文件操作（java.nio.file.Files / Path）
    // =========================================================
    /*
     * Java 7+ 推荐使用 NIO2 的 Files 工具类操作文件，更简洁
     * 常用方法：
     *   Files.writeString(path, content)        → 写文本
     *   Files.readString(path)                  → 读全部文本（Java 11+）
     *   Files.readAllLines(path)                → 读所有行
     *   Files.exists(path)                      → 判断文件是否存在
     *   Files.createFile(path)                  → 创建文件
     *   Files.delete(path)                      → 删除文件
     *   Files.copy(source, target, options)     → 复制文件
     */
    static void fileOperationDemo() throws IOException {
        Path filePath = Path.of(System.getProperty("java.io.tmpdir"), "base5_nio.txt");

        // 写入文本
        Files.writeString(filePath, "NIO2 文件操作\n第二行", StandardCharsets.UTF_8);
        System.out.println("文件是否存在：" + Files.exists(filePath));

        // 读取全部文本（Java 11+）
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        System.out.println("读取内容：\n" + content);

        // 按行读取
        java.util.List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
        System.out.println("总行数：" + lines.size());

        // 追加写入
        Files.writeString(filePath, "\n追加的第三行", StandardCharsets.UTF_8, StandardOpenOption.APPEND);

        // 删除文件
        Files.delete(filePath);
        System.out.println("文件删除后是否存在：" + Files.exists(filePath));
    }

    // =========================================================
    // 七、序列化基础
    // =========================================================
    /*
     * 序列化：把对象转换为字节流（可保存到文件或网络传输）
     * 反序列化：把字节流还原为对象
     *
     * 使用条件：
     * - 类必须实现 Serializable 接口（标记接口，无方法）
     * - 建议显式声明 serialVersionUID，防止版本不兼容
     * - 用 transient 修饰的字段不会被序列化
     */
    static class Student implements Serializable {
        private static final long serialVersionUID = 1L; // 建议显式声明

        String name;
        int age;
        transient String password; // transient：不参与序列化

        public Student(String name, int age, String password) {
            this.name = name;
            this.age = age;
            this.password = password;
        }

        @Override
        public String toString() {
            return "Student{name='" + name + "', age=" + age + ", password='" + password + "'}";
        }
    }

    static void serializationDemo() throws IOException, ClassNotFoundException {
        Path filePath = Path.of(System.getProperty("java.io.tmpdir"), "base5_student.ser");
        Student student = new Student("张三", 20, "secret123");
        System.out.println("序列化前：" + student);

        // 序列化：对象 → 文件
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath.toFile()))) {
            oos.writeObject(student);
            System.out.println("序列化完成");
        }

        // 反序列化：文件 → 对象
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filePath.toFile()))) {
            Student restored = (Student) ois.readObject();
            System.out.println("反序列化后：" + restored);
            // 注意：password 字段为 transient，反序列化后为 null
        }

        Files.deleteIfExists(filePath);
    }

    // =========================================================
    // 练习留空区
    // =========================================================
    /*
     * 【练习 1】自定义异常
     * 要求：
     * - 创建一个受检异常类 AgeOutOfRangeException，包含一个带 message 参数的构造方法
     * - 写一个方法 validateAge(int age)：
     *     如果 age < 0 或 age > 150，抛出 AgeOutOfRangeException
     *     否则打印 "年龄合法：age"
     * - 在 main 中调用并用 try-catch 捕获
     */
    // TODO: 在此处实现练习 1
    static class AgeOutOfRangeException extends Exception {
        public AgeOutOfRangeException(String message) {
            super(message);
        }
    }
    static void validateAge(int age) throws AgeOutOfRangeException {
        if (age < 0 || age > 150) {
            throw new AgeOutOfRangeException("不合法年龄" + age);
        }
        System.out.println("合法年龄：" + age);
    }

    /*
     * 【练习 2】文件读写
     * 要求：
     * - 用 Files.writeString 向临时目录写入一段多行文字（至少 3 行）
     * - 用 Files.readAllLines 逐行读取并打印，同时打印行号（从 1 开始）
     * - 操作完成后删除文件
     */
    // TODO: 在此处实现练习 2

    static void writeAndRead() throws IOException {
        Path temPath = Path.of(System.getProperty("java.io.tmpdir"), "temp.txt");

        for (int i = 0; i < 3; i++) {
            Files.writeString(temPath, "这是第" + i + "行\n");
        }




    }

    /*
     * 【练习 3】try-with-resources
     * 要求：
     * - 用 BufferedWriter（指定 UTF-8）写入若干行内容到文件
     * - 用 BufferedReader（指定 UTF-8）按行读取并打印
     * - 全程使用 try-with-resources，不手写 finally
     */
    // TODO: 在此处实现练习 3

    /*
     * 【练习 4】序列化
     * 要求：
     * - 创建一个 Product 类（实现 Serializable），包含 name(String)、price(double)、secret(String，transient）
     * - 序列化一个 Product 对象到文件
     * - 反序列化后打印，观察 secret 字段是否为 null
     */
    // TODO: 在此处实现练习 4

    // =========================================================
    // AI Review 区
    // =========================================================
    /*
     * 完成练习后，可发给 Copilot 进行 Review，期望检查维度：
     *
     * 1. 语法：代码是否能正常编译，异常类型声明是否正确
     * 2. 题意：是否满足每道题的全部要求（包括 transient、try-with-resources 等）
     * 3. 类型：流的类型选择是否合理（字节流 vs 字符流）
     * 4. 资源管理：是否正确关闭了流（有没有用 try-with-resources）
     * 5. 简洁性：是否有更简洁的 NIO2 写法可以替代
     * 6. 概念：是否混淆了受检异常和非受检异常
     */

    // =========================================================
    // main 方法（运行所有 demo）
    // =========================================================
    public static void main(String[] args) throws Exception {
        System.out.println("========== 二、try-catch-finally ==========");
        tryCatchDemo();

        System.out.println("\n========== 多异常合并捕获 ==========");
        multiCatchDemo("0");    // ArithmeticException: / by zero
        multiCatchDemo("abc");  // NumberFormatException

        System.out.println("\n========== 三、try-with-resources ==========");
        tryWithResourcesDemo();

        System.out.println("\n========== 四、字节流 ==========");
        byteStreamDemo();

        System.out.println("\n========== 五、字符流 ==========");
        charStreamDemo();

        System.out.println("\n========== 六、文件操作（NIO2）==========");
        fileOperationDemo();

        System.out.println("\n========== 七、序列化 ==========");
        serializationDemo();

        System.out.println("\n +++++++++作业++++++++++");
        validateAge(35);
        validateAge(350);
    }
}


