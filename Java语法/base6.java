// ====================
// 学习文件：Java 基础语法第六章
// 章节名称：并发基础
// 知识点：Thread、Runnable、Callable、线程安全、synchronized、volatile、线程池、Future、CompletableFuture
// 使用方式：先看"知识讲解区"，再完成"练习留空区"，最后交给 AI review
// ====================

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class base6 {

    // ====================
    // 1. 知识讲解区
    // ====================

    // ── 1.1 创建线程的三种方式 ──────────────────────────────
    //
    // 方式一：继承 Thread 类，重写 run() 方法
    //   优点：写法简单
    //   缺点：Java 单继承，继承了 Thread 就不能再继承其他类
    //
    // 方式二：实现 Runnable 接口，传入 Thread 构造器
    //   优点：解耦任务与线程；可以同时实现其他接口
    //   推荐度：★★★☆
    //
    // 方式三：实现 Callable 接口 + FutureTask
    //   优点：可以有返回值，可以抛出受检异常
    //   推荐度：★★★★

    // 示例 —— 继承 Thread
    static class MyThread extends Thread {
        @Override
        public void run() {
            System.out.println("Thread 方式，线程名：" + Thread.currentThread().getName());
        }
    }

    // 示例 —— 实现 Runnable
    static class MyRunnable implements Runnable {
        @Override
        public void run() {
            System.out.println("Runnable 方式，线程名：" + Thread.currentThread().getName());
        }
    }

    // 示例 —— 实现 Callable（有返回值）
    static class MyCallable implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("Callable 方式，线程名：" + Thread.currentThread().getName());
            return 42; // 可以返回计算结果
        }
    }

    // ── 1.2 线程安全 ────────────────────────────────────────
    //
    // 线程安全问题：多个线程同时读写同一个共享变量，可能导致结果不正确。
    //
    // 常见解决方案：
    // 1. synchronized 关键字：同一时刻只允许一个线程进入临界区
    // 2. volatile 关键字：保证可见性（修改立即刷新到主内存），但不保证原子性
    // 3. 原子类（AtomicInteger 等）：基于 CAS，性能优于 synchronized，适合简单计数

    // 示例 —— synchronized 的三种用法
    //
    // ① 修饰实例方法（最常见）
    //   锁是"调用这个方法的那个对象实例"，即 this
    //   同一个对象的两个 synchronized 方法，同一时刻只能有一个线程进入
    //   不同对象的 synchronized 方法互不影响（因为锁不是同一把）
    //
    // ② 修饰静态方法
    //   锁是"这个类的 Class 对象"（全局唯一），即 Counter.class
    //   所有线程共享同一把锁，不管 new 了多少个对象实例
    //   写法：public static synchronized void someStaticMethod() { ... }
    //
    // ③ synchronized 代码块（粒度更细，推荐）
    //   可以自己指定锁对象，只锁住必要的代码段，减少锁的持有时间
    //   写法：synchronized (锁对象) { 临界区代码 }
    //   常见用法：synchronized (this) { ... }  或  synchronized (someSharedLock) { ... }
    //
    // ── "锁是当前对象" 是什么意思？────────────────────────────
    //   Java 中每个对象都有一把内置锁（也叫 monitor / 监视器锁）
    //   当线程调用 synchronized 实例方法时，必须先拿到"调用该方法的那个对象"的锁
    //   拿到锁的线程才能执行，其他线程会在外面排队等待
    //   方法执行完毕后自动释放锁，下一个等待的线程才能进入
    //
    //   举例：Counter c1 = new Counter();  Counter c2 = new Counter();
    //   线程A 调用 c1.increment() → 拿的是 c1 的锁
    //   线程B 调用 c2.increment() → 拿的是 c2 的锁  ← 两把不同的锁，互不阻塞！
    //   线程C 调用 c1.increment() → 也要拿 c1 的锁  ← 和线程A 抢同一把锁，需要排队等待
    static class Counter {
        private int count = 0;

        // ① 修饰实例方法：锁是 this（即调用该方法的 Counter 对象）
        public synchronized void increment() {
            count++;
        }

        public synchronized int getCount() {
            return count;
        }

        // ③ 等价的代码块写法（与上面 increment() 效果完全相同）
        public void incrementBlock() {
            synchronized (this) {   // 显式指定锁对象为 this
                count++;
            }
            // synchronized 块之外的代码不受锁保护，可以并发执行
            // 粒度更细 → 锁持有时间更短 → 性能更好
        }

        // ② 静态方法示例：锁是 Counter.class，与实例无关
        public static synchronized void staticMethod() {
            System.out.println("静态 synchronized，锁是 Counter.class");
        }
    }

    // 示例 —— volatile（可见性，不保证原子性）
    static volatile boolean stopFlag = false; // 多线程间的标志位，用 volatile 保证可见性

    // 示例 —— AtomicInteger（原子操作，适合计数场景）
    static AtomicInteger atomicCount = new AtomicInteger(0);

    // ── 1.3 线程池 ──────────────────────────────────────────
    //
    // 直接 new Thread() 的问题：频繁创建/销毁线程代价高，且难以控制线程数量。
    //
    // 线程池优点：
    // - 复用线程，减少创建销毁开销
    // - 可以控制最大并发数
    // - 提供任务队列、拒绝策略等管理能力
    //
    // 常用工厂方法（Executors）：
    //   Executors.newFixedThreadPool(n)    —— 固定大小线程池
    //   Executors.newCachedThreadPool()    —— 弹性线程池（空闲线程会被回收）
    //   Executors.newSingleThreadExecutor()—— 单线程顺序执行
    //
    // 注意：生产环境推荐直接使用 ThreadPoolExecutor 自定义参数，避免 OOM 风险。

    // ── 1.4 Future 与 CompletableFuture ─────────────────────
    //
    // Future：代表异步任务的结果
    //   future.get()        —— 阻塞等待结果
    //   future.isDone()     —— 是否完成
    //   future.cancel(true) —— 尝试取消
    //
    // CompletableFuture（Java 8+）：更强大的异步编排工具
    //   supplyAsync(supplier)          —— 异步执行有返回值的任务
    //   thenApply(fn)                  —— 对结果做转换（同步）
    //   thenAccept(consumer)           —— 消费结果，无返回值
    //   thenCombine(other, fn)         —— 合并两个 Future 的结果
    //   exceptionally(fn)              —— 异常处理兜底


    public static void main(String[] args) throws Exception {

        System.out.println("===== 1.1 三种创建线程方式 =====");

        // 方式一：继承 Thread
        MyThread t1 = new MyThread();
        t1.start(); // 注意：调用 start() 启动线程，不要直接调用 run()

        // 方式二：实现 Runnable（推荐用 lambda 简化）
        Thread t2 = new Thread(new MyRunnable());
        t2.start();

        // lambda 写法（等同于方式二）
        Thread t3 = new Thread(() -> System.out.println("Lambda Runnable，线程名：" + Thread.currentThread().getName()));
        t3.start();

        // 方式三：Callable + FutureTask
        FutureTask<Integer> futureTask = new FutureTask<>(new MyCallable());
        Thread t4 = new Thread(futureTask);
        t4.start();
        Integer result = futureTask.get(); // 阻塞，直到任务完成
        System.out.println("Callable 返回值：" + result);

        System.out.println("\n===== 1.2 线程安全示例 =====");

        // synchronized 示例
        Counter counter = new Counter();
        Thread ta = new Thread(() -> { for (int i = 0; i < 1000; i++) counter.increment(); });
        Thread tb = new Thread(() -> { for (int i = 0; i < 1000; i++) counter.increment(); });
        ta.start(); tb.start();
        ta.join(); tb.join(); // 等待两个线程执行完毕
        System.out.println("synchronized Counter 结果（期望 2000）：" + counter.getCount());

        // AtomicInteger 示例
        atomicCount.set(0);
        Thread tc = new Thread(() -> { for (int i = 0; i < 1000; i++) atomicCount.incrementAndGet(); });
        Thread td = new Thread(() -> { for (int i = 0; i < 1000; i++) atomicCount.incrementAndGet(); });
        tc.start(); td.start();
        tc.join(); td.join();
        System.out.println("AtomicInteger 结果（期望 2000）：" + atomicCount.get());

        System.out.println("\n===== 1.3 线程池示例 =====");

        // 使用固定大小线程池执行任务
        ExecutorService pool = Executors.newFixedThreadPool(3);
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            pool.submit(() -> System.out.println("线程池任务 " + taskId + "，执行线程：" + Thread.currentThread().getName()));
        }
        pool.shutdown(); // 关闭线程池（等待已提交任务完成）
        pool.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("\n===== 1.4 Future 与 CompletableFuture =====");

        // Future 示例
        ExecutorService pool2 = Executors.newSingleThreadExecutor();
        Future<String> future = pool2.submit(() -> {
            Thread.sleep(100);
            return "Future 任务完成";
        });
        System.out.println("任务是否完成：" + future.isDone());
        System.out.println(future.get()); // 阻塞直到完成
        pool2.shutdown();

        // CompletableFuture 示例
        CompletableFuture<Integer> cf = CompletableFuture
                .supplyAsync(() -> {
                    System.out.println("异步计算中，线程：" + Thread.currentThread().getName());
                    return 10;
                })
                .thenApply(n -> n * 2)             // 结果转换：10 -> 20
                .thenApply(n -> n + 5);            // 再转换：20 -> 25

        System.out.println("CompletableFuture 最终结果：" + cf.get());

        // 异常处理
        CompletableFuture<String> cfWithError = CompletableFuture
                .supplyAsync(() -> {
                    if (true) throw new RuntimeException("模拟异常");
                    return "正常结果";
                })
                .exceptionally(ex -> "捕获到异常：" + ex.getMessage());

        System.out.println(cfWithError.get());
    }


    // ====================
    // 2. 练习留空区
    // ====================
    //
    // 【题目 1】实现 Runnable，打印 1~5，用线程池执行
    // 要求：
    //   - 创建一个实现 Runnable 的类 PrintTask，构造器接收一个 int 参数（表示要打印的数字）
    //   - 用 newFixedThreadPool(2) 提交 5 个任务
    //   - 关闭线程池并等待所有任务完成
    //
    // TODO: 在这里实现 PrintTask 类及主方法逻辑
    static class PrintTask implements Runnable {
        private final int number;
        PrintTask(int number) {
           this.number = number;
        }
        @Override
        public void run() {
           System.out.println(number);
        }
    }

    static void doExcise1() throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (int i = 1; i <=5 ; i++) {
            pool.submit(new PrintTask(i));
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }


    // 【题目 2】线程安全计数器
    // 要求：
    //   - 定义一个 SafeCounter 类，内部用 AtomicInteger 保存计数
    //   - 提供 increment() 和 getCount() 方法
    //   - 开启 3 个线程，每个线程对计数器累加 500 次
    //   - 最终打印结果，期望值为 1500
    //
    // TODO: 在这里实现 SafeCounter 及多线程逻辑
    static class SafeCounter {
        private AtomicInteger count = new AtomicInteger(0);

        public void increment() {
            count.getAndIncrement();
        }
        public int getCount() {
            return count.get();
        }
    }

    static void doExercise2() throws InterruptedException {
        SafeCounter sc = new SafeCounter();
        Thread t1 = new Thread(() -> {
            for (int i =0 ; i < 500; i++) {
                sc.increment();
            }
        });
        Thread t2 = new Thread(() -> {
            for (int i =0 ; i < 500; i++) {
                sc.increment();
            }
        });
        Thread t3 = new Thread(() -> {
            for (int i =0 ; i < 500; i++) {
                sc.increment();
            }
        });

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println(sc.getCount());
    }








    // 【题目 3】CompletableFuture 链式调用
    // 要求：
    //   - 用 supplyAsync 异步生成字符串 "hello"
    //   - 用 thenApply 将字符串转大写
    //   - 用 thenApply 拼接 " WORLD"
    //   - 打印最终结果
    //
    // TODO: 在这里实现 CompletableFuture 链式调用

    static void doExercise3() throws ExecutionException, InterruptedException {
        CompletableFuture<String> cf =
                CompletableFuture.supplyAsync(() -> {
                    return "hello";
                }).thenApply(s -> {
                    return s.toUpperCase();
                }).thenApply(s -> {
                    return s + "WORLD";
                });

        System.out.println(cf.get());


    }


    // ====================
    // 3. AI Review 区
    // ====================
    //
    // 完成练习后，请将代码发给 AI，按以下维度检查：
    // 1. 线程是否正确启动（start vs run）
    // 2. 线程安全方案是否与使用场景匹配
    // 3. 线程池是否正确关闭（shutdown / awaitTermination）
    // 4. CompletableFuture 链式调用的顺序和类型是否正确
    // 5. 是否存在可能的死锁或资源泄漏隐患
    //
    // 可用 Review 口令：
    //   - 只指出错误，不直接给答案
    //   - 按初学者角度讲解
    //   - 直接给标准答案
}
