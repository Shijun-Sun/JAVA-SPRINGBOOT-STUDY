/**
 * Java 21 新特性学习文件
 * 重点：当前最新 LTS，高并发新能力
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

// ---- 1. Virtual Threads（虚拟线程，正式版）----
//
// • 虚拟线程（Virtual Thread）是 JVM 管理的轻量级线程，不是 OS 线程。
// • 创建成本接近于零，可以同时存在数百万个。
// • 适合 I/O 密集型任务（数据库请求、HTTP 调用、文件读写）。
// • 不适合 CPU 密集型任务（计算密集型建议仍用线程池）。
//
// 三种创建方式：
//   // 方式 1：直接启动
//   Thread.ofVirtual().start(() -> System.out.println("虚拟线程"));
//
//   // 方式 2：先创建再启动
//   Thread vt = Thread.ofVirtual().unstarted(() -> doWork());
//   vt.start();
//
//   // 方式 3：通过线程池（推荐用于批量任务）
//   try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
//       for (int i = 0; i < 1000; i++) {
//           executor.submit(() -> doWork());
//       }
//   } // try-with-resources 自动 shutdown 并等待完成
//
// 与平台线程对比：
//   项目          平台线程              虚拟线程
//   创建成本      较高（OS 线程）        极低（JVM 管理）
//   数量上限      数千                  数百万
//   阻塞 I/O      线程挂起，占用 OS 资源  JVM 自动调度，不浪费 OS 线程
//   适用场景      CPU 密集              I/O 密集、高并发服务
//
// 注意事项：
// • 不要池化虚拟线程（每次任务新建即可，成本极低）。
// • synchronized 块内虚拟线程会"钉住"（pinning）OS 线程，影响性能，
//   建议改用 ReentrantLock。


// ---- 2. Structured Concurrency（结构化并发，预览）----
//
// • 把多个并发子任务组织为一个有明确生命周期的工作单元。
// • 父任务等待所有子任务完成，任意子任务失败可取消其他任务。
// • 核心类：StructuredTaskScope（java.util.concurrent 包下）
//
// 示例（任意一个失败即取消其他）：
//   try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
//       Subtask<String> userTask  = scope.fork(() -> fetchUser(id));
//       Subtask<Integer> scoreTask = scope.fork(() -> fetchScore(id));
//       scope.join().throwIfFailed(); // 等待并传播异常
//       // 两个子任务都成功才到这里
//       System.out.println(userTask.get() + ": " + scoreTask.get());
//   }
//
// • ShutdownOnFailure：任意子任务失败，取消其他子任务。
// • ShutdownOnSuccess：任意子任务成功，取消其他子任务（适合竞态）。
// • Java 21 为预览，Java 23 继续演进。


// ---- 3. switch 模式匹配（正式版）----
//
// • Java 21 正式，之前章节（java18to20.java）已学过，此处为完整总结。
// • 支持：类型模式、null 模式、守卫条件（when）、record 解构模式。
//
// 完整示例（结合 sealed + record）：
//   sealed interface Shape permits Circle, Rectangle {}
//   record Circle(double radius) implements Shape {}
//   record Rectangle(double w, double h) implements Shape {}
//
//   double area = switch (shape) {
//       case Circle c    -> Math.PI * c.radius() * c.radius();
//       case Rectangle r -> r.w() * r.h();
//   };
//
// record 解构在 switch 中：
//   switch (shape) {
//       case Circle(double r)           -> Math.PI * r * r;
//       case Rectangle(double w, double h) -> w * h;
//   }


// ---- 4. Sequenced Collections（序列化集合接口）----
//
// • Java 21 为集合框架新增了三个接口：
//   - SequencedCollection：有序集合，提供 getFirst / getLast / addFirst / addLast / reversed
//   - SequencedSet：有序 Set
//   - SequencedMap：有序 Map，提供 firstEntry / lastEntry / reversed
//
// 示例：
//   List<String> list = new ArrayList<>(List.of("A", "B", "C"));
//   System.out.println(list.getFirst()); // A
//   System.out.println(list.getLast());  // C
//   list.addFirst("Z");                  // [Z, A, B, C]
//
//   LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
//   map.put("one", 1); map.put("two", 2); map.put("three", 3);
//   System.out.println(map.firstEntry()); // one=1
//   System.out.println(map.lastEntry());  // three=3


// ---- 5. Record Patterns（正式版，在 switch 中使用）----
//
// 已在 java18to20.java 中学过，Java 21 正式版可放心使用。


// ============================================================
// 【练习留空区】
// ============================================================

import java.util.*;
import java.util.concurrent.*;

// ---- 练习需要的类型定义 ----
// TODO: 定义 sealed interface Notification permits EmailNotif, SmsNotif, PushNotif
// TODO: 定义 record EmailNotif(String to, String subject, String body) implements Notification
// TODO: 定义 record SmsNotif(String phone, String message) implements Notification
// TODO: 定义 record PushNotif(String deviceId, String title) implements Notification


public class java21 {

    public static void main(String[] args) throws Exception {

        // ---- 练习 1：Virtual Threads 批量任务 ----
        // 要求：
        //   (1) 模拟 20 个"查询任务"，每个任务用 Thread.sleep(100) 模拟 I/O 耗时。
        //   (2) 用 Executors.newVirtualThreadPerTaskExecutor() 并发执行所有任务。
        //   (3) 记录总耗时并打印（应约 100ms，而非 20*100ms=2000ms）。
        //   (4) 用注释说明：若改为 newFixedThreadPool(4)，耗时会有何变化？

        // TODO: 练习 1 在此处实现


        // ---- 练习 2：虚拟线程 vs 平台线程 对比 ----
        // 要求：
        //   (1) 分别用平台线程池（newFixedThreadPool(10)）和虚拟线程池，
        //       各执行 100 个 sleep(50ms) 的任务。
        //   (2) 分别记录两种方式的总耗时，打印对比结果。
        //   (3) 用注释分析耗时差异的原因。

        // TODO: 练习 2 在此处实现


        // ---- 练习 3：switch 模式匹配 + sealed + record 解构（综合）----
        // 要求：
        //   (1) 创建几个 EmailNotif、SmsNotif、PushNotif 实例放入列表。
        //   (2) 遍历列表，用 switch + record 解构模式分别处理：
        //       - EmailNotif：打印 "发邮件给 [to]，主题：[subject]"
        //       - SmsNotif：打印 "发短信给 [phone]：[message]"
        //       - PushNotif：打印 "推送给设备 [deviceId]：[title]"

        // TODO: 练习 3 在此处实现


        // ---- 练习 4：Sequenced Collections ----
        // 要求：
        //   (1) 创建一个 ArrayList，添加 5 个元素。
        //   (2) 用 getFirst / getLast 打印第一个和最后一个元素。
        //   (3) 用 addFirst 在开头插入一个元素，打印整个列表。
        //   (4) 用 reversed() 获取逆序视图并遍历打印。
        //   (5) 创建一个 LinkedHashMap，添加 3 个键值对，
        //       打印 firstEntry 和 lastEntry。

        // TODO: 练习 4 在此处实现

    }

}


// ============================================================
// 【AI Review 区】
// ============================================================
//
// 当你完成练习后，把代码发给 Copilot 并说："请 review 我的 java21 练习"
//
// 期望检查维度：
//   1. 虚拟线程池是否用了 try-with-resources（自动 shutdown + 等待）
//   2. 计时是否在提交任务前后，而不是包含 executor 创建时间
//   3. switch + record 解构是否直接绑定了字段变量（无需调用 getter）
//   4. sealed 类型的 switch 是否穷举了所有子类型（无需 default）
//   5. getFirst/getLast 是否理解这是 Java 21 新增的（旧代码用 get(0) / get(size-1)）
//   6. reversed() 返回的是视图还是副本（是视图，修改原集合会反映）
