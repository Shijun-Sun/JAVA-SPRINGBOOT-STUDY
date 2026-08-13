/**
 * Java 11 新特性学习文件
 * 重点：生产可用的重要增强
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

// ---- 1. 字符串 API 增强 ----
//
// Java 11 新增了多个实用字符串方法：
//
// • isBlank()       — 判断字符串是否为空或只含空白字符（比 isEmpty 更宽泛）
// • strip()         — 去除首尾空白（Unicode 感知，比 trim() 更准确）
// • stripLeading()  — 只去除开头空白
// • stripTrailing() — 只去除结尾空白
// • lines()         — 按行拆分字符串，返回 Stream<String>
// • repeat(n)       — 将字符串重复 n 次
//
// 示例：
//   "  hello  ".strip()       → "hello"
//   "  hello  ".stripLeading() → "hello  "
//   "  ".isBlank()             → true
//   "ha".repeat(3)             → "hahaha"
//   "a\nb\nc".lines().count()  → 3


// ---- 2. var 在 Lambda 参数中使用 ----
//
// • Java 11 允许在 lambda 参数中使用 var，主要用途是为参数加注解。
// • 所有参数都要用 var（不能混用）。
//
// 示例：
//   // 为 lambda 参数加 @NotNull 注解（如引入注解框架）
//   Consumer<String> printer = (@NotNull var s) -> System.out.println(s);
//
// 注：日常开发中这个特性使用频率不高，了解即可。


// ---- 3. Files 增强：readString / writeString ----
//
// • Java 11 新增了便捷的文件读写方法，无需手动处理流。
// • Files.readString(path)              — 读取整个文件为字符串
// • Files.writeString(path, content)    — 将字符串写入文件
// • 可传入 Charset 参数指定编码（默认 UTF-8）。
//
// 示例：
//   Path path = Path.of("test.txt");
//   Files.writeString(path, "Hello Java 11");
//   String content = Files.readString(path);
//   System.out.println(content); // Hello Java 11


// ---- 4. HTTP Client 标准化 ----
//
// • Java 9 引入为孵化器特性，Java 11 正式标准化为 java.net.http 包。
// • 支持 HTTP/1.1 和 HTTP/2，支持同步和异步请求。
// • 核心类：HttpClient、HttpRequest、HttpResponse
//
// 示例（同步 GET）：
//   HttpClient client = HttpClient.newHttpClient();
//   HttpRequest request = HttpRequest.newBuilder()
//       .uri(URI.create("https://httpbin.org/get"))
//       .GET()
//       .build();
//   HttpResponse<String> response = client.send(request,
//       HttpResponse.BodyHandlers.ofString());
//   System.out.println(response.statusCode()); // 200
//   System.out.println(response.body());


// ---- 5. Optional 增强：isEmpty ----
//
// • Java 11 为 Optional 新增了 isEmpty() 方法，与 isPresent() 相反。
//
// 示例：
//   Optional<String> empty = Optional.empty();
//   System.out.println(empty.isEmpty()); // true


// ============================================================
// 【练习留空区】
// ============================================================

import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class java11 {

    public static void main(String[] args) throws Exception {

        // ---- 练习 1：字符串 API 增强 ----
        // 要求：
        //   (1) 有字符串 "  \t  "，用 isBlank() 判断并打印结果（true/false）。
        //   (2) 有字符串 "  Java 11  "，分别用 strip()、stripLeading()、stripTrailing() 处理并打印。
        //   (3) 有多行字符串 "line1\nline2\nline3"，用 lines() 转为 Stream，打印每一行并加上行号。
        //   (4) 用 repeat(5) 生成 "=-" 重复 5 次的分隔线并打印。

        // TODO: 练习 1 在此处实现


        // ---- 练习 2：Files 读写 ----
        // 要求：
        //   (1) 用 Files.writeString 向当前目录写一个文件 "java11_test.txt"，内容为 "Hello Java 11"。
        //   (2) 用 Files.readString 读取该文件并打印内容。
        //   (3) 删除该临时文件（用 Files.deleteIfExists）。

        // TODO: 练习 2 在此处实现


        // ---- 练习 3：HTTP Client ----
        // 要求：
        //   (1) 用 HttpClient 发送一个 GET 请求到 "https://httpbin.org/get"。
        //   (2) 打印响应状态码和响应体。
        //   (3) 用注释说明 HttpResponse.BodyHandlers.ofString() 的作用。
        // 提示：需要网络连接，若无网络可跳过此练习。

        // TODO: 练习 3 在此处实现


        // ---- 练习 4：Optional.isEmpty ----
        // 要求：
        //   (1) 创建一个空的 Optional<Integer>，用 isEmpty() 判断并打印。
        //   (2) 创建一个有值的 Optional<Integer>，用 isEmpty() 判断并打印。

        // TODO: 练习 4 在此处实现

    }

}


// ============================================================
// 【AI Review 区】
// ============================================================
//
// 当你完成练习后，把代码发给 Copilot 并说："请 review 我的 java11 练习"
//
// 期望检查维度：
//   1. strip() 和 trim() 的区别是否理解（Unicode 空白字符处理）
//   2. lines() 返回的 Stream 是否正确使用（记得终止操作）
//   3. Files 读写是否处理了 IOException（throws 或 try-catch）
//   4. HttpClient 请求是否正确构建（Builder 模式链式调用）
//   5. isEmpty() 与 isPresent() 的关系是否清晰
