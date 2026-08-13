/**
 * Java 10 新特性学习文件
 * 重点：局部变量类型推断
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

// ---- 1. var 局部变量类型推断 ----
//
// • Java 10 引入 var，让编译器根据右侧表达式自动推断局部变量类型。
// • var 只能用于局部变量，不能用于字段、方法参数、返回值。
// • var 不是动态类型，类型在编译期就已确定，只是省略了显式声明。
// • 不建议滥用：当类型不明显时，仍应写明类型，保证可读性。
//
// 示例：
//   var name = "Alice";                    // 推断为 String
//   var list = new ArrayList<String>();    // 推断为 ArrayList<String>
//   var map  = new HashMap<String, Integer>(); // 推断为 HashMap<String, Integer>
//
// 不能用 var 的场景：
//   var x;              // ❌ 没有初始化，无法推断
//   var x = null;       // ❌ null 无法推断类型
//   var x = {1, 2, 3};  // ❌ 数组字面量不能用 var


// ---- 2. 集合 copyOf 方法 ----
//
// • Java 10 新增了 List.copyOf / Set.copyOf / Map.copyOf。
// • 接收一个已有集合，返回其"不可变副本"。
// • 若原集合本身已经是不可变集合，可能直接返回同一实例（节省内存）。
// • 不允许原集合含 null 元素。
//
// 示例：
//   List<String> original = new ArrayList<>(List.of("A", "B", "C"));
//   List<String> copy = List.copyOf(original);
//   original.add("D"); // original 变了
//   System.out.println(copy); // copy 不受影响，仍是 [A, B, C]


// ---- 3. Collectors.toUnmodifiableList / Set / Map ----
//
// • Java 10 在 Collectors 中新增了三个方法，配合 Stream 收集为不可变集合。
//
// 示例：
//   List<String> unmodifiable = Stream.of("A", "B", "C")
//       .collect(Collectors.toUnmodifiableList());


// ============================================================
// 【练习留空区】
// ============================================================

import java.util.*;
import java.util.stream.*;

public class java10 {

    public static void main(String[] args) {

        // ---- 练习 1：var 基础用法 ----
        // 要求：
        //   (1) 用 var 声明一个字符串变量，值为你的名字，并打印。
        //   (2) 用 var 声明一个 ArrayList<Integer>，添加 3 个元素，并打印。
        //   (3) 用 var 在 for-each 循环中遍历上面的列表，打印每个元素。
        //   (4) 写一行注释，说明 var 推断出的实际类型是什么。

        // TODO: 练习 1 在此处实现


        // ---- 练习 2：List.copyOf ----
        // 要求：
        //   (1) 创建一个可变的 ArrayList，添加若干元素。
        //   (2) 用 List.copyOf 生成不可变副本。
        //   (3) 修改原 ArrayList（添加或删除元素），打印副本，验证副本未受影响。
        //   (4) 尝试向副本中添加元素，用注释说明结果。

        // TODO: 练习 2 在此处实现


        // ---- 练习 3：toUnmodifiableList ----
        // 要求：
        //   (1) 有一个字符串列表 ["java", "python", "go"]。
        //   (2) 用 Stream + filter 过滤出长度大于 3 的语言名。
        //   (3) 用 Collectors.toUnmodifiableList() 收集结果并打印。
        //   (4) 尝试向结果列表中添加元素，用注释说明结果。

        // TODO: 练习 3 在此处实现

    }

}


// ============================================================
// 【AI Review 区】
// ============================================================
//
// 当你完成练习后，把代码发给 Copilot 并说："请 review 我的 java10 练习"
//
// 期望检查维度：
//   1. var 是否只用在了局部变量上，有没有误用场景
//   2. var 的命名是否清晰（var 省略类型后，变量名要更有语义）
//   3. copyOf 与 List.of 的区别是否理解（一个接受已有集合，一个直接构造）
//   4. 不可变集合操作时是否正确识别了抛出 UnsupportedOperationException
//   5. 注释是否说明了推断类型，帮助初学者理解 var 本质
