# web-chain-order-lab · Filter / Interceptor / AOP 执行顺序实验

> 知识点实验，**非正式交付**。对应[作业 07](../../../理论学习/M01-SpringBoot核心/07-AOP-Filter-Interceptor/作业/07-AOP-Filter-Interceptor实战.md)任务四、任务五。
> 随意改、随意删，不影响 `product-service`。做完保留还是删除由你决定。

## 这个工程是什么

一个最小 Spring Boot 工程，只有一个 `/demo` 接口。请求会依次穿过：

```
OrderLogFilter → OrderLogInterceptor → ChainOrderAspect → DemoController → DemoService
```

`Filter`、`Interceptor`、`Aspect` 三个类**已经接好线、能编译能启动**，但方法体里的日志是 `TODO`——初始状态跑起来不会打印顺序日志。你的任务就是补全这些日志，然后观察顺序。

## 初始状态验证

先确认脚手架本身能跑（不是你的问题）：

```bash
mvn -q compile          # 或 mvn -o -q compile（离线）
mvn spring-boot:run     # 启动在 8082 端口
curl http://localhost:8082/demo
# 预期返回：hello from DemoService
```

能返回就说明脚手架正常，接下来开始改。

## 任务四：观察执行顺序

1. 在 `OrderLogFilter` 的 `chain.doFilter` 前后各打一行日志；
2. 在 `OrderLogInterceptor` 的 `preHandle` / `postHandle` / `afterCompletion` 各打一行；
3. 在 `ChainOrderAspect` 的 `proceed` 前后各打一行；
4. 重启，`curl http://localhost:8082/demo`，看日志顺序；
5. 再 `curl "http://localhost:8082/demo?fail=true"`（接口抛异常），对比哪些环节不再执行。

回答：
- 进入顺序、返回顺序分别是什么？
- `postHandle` 和 `afterCompletion` 谁先？抛异常时谁不执行？
- 日志能否印证"Filter 最外、Interceptor 居中、AOP 最内"？

## 任务五：AOP 统计耗时

1. 在 `ChainOrderAspect` 里用 `proceed` 前后的时间差打印方法耗时；
2. 观察切点 `execution(* com.study.lab.service..*(..))` 切到了哪些方法；
3. 试着让 `DemoController` 调用本类另一个方法，看切面是否生效（自调用问题）；
4. 把 `proceed()` 注释掉，看接口返回什么（体会 `@Around` 必须 `proceed`）。

## 验收（自查）

- [ ] 补全后一次 `/demo` 请求，日志按 `Filter进 → preHandle → Aspect进 → (Controller/Service) → Aspect出 → postHandle → afterCompletion → Filter出` 顺序出现；
- [ ] `/demo?fail=true` 时 `postHandle` 不打印、`afterCompletion` 打印；
- [ ] 能说清切面为何切不到自调用。

## 说明

- AOP 依赖：本工程离线环境只缓存了 `aspectjweaver`，故 `pom.xml` 直接引入它并在启动类上 `@EnableAspectJAutoProxy`。联网环境更推荐换成 `spring-boot-starter-aop`。
