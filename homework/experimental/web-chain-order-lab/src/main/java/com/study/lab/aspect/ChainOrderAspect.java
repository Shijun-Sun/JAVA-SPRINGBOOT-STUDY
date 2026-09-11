package com.study.lab.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 【实验待补全】AOP 切面是链路最内圈，包在 Controller/Service 方法外。
 * 初始状态已能编译运行：直接 proceed，不打印日志，不统计耗时。
 *
 * 任务一（执行顺序）：在 proceed 前后各打印一行日志，观察它相对
 * Interceptor.preHandle / Controller 的位置。
 *
 * 任务二（耗时统计）：把 proceed 前后的时间差算出来打印，回答切点表达式、
 * 自调用是否生效、不调用 proceed 会怎样。
 *
 * 当前切点切的是 controller 与 service 两个包下的所有方法，可按需收窄。
 */
@Aspect
@Component
public class ChainOrderAspect {

    private static final Logger log = LoggerFactory.getLogger(ChainOrderAspect.class);

    @Around("execution(* com.study.lab.controller..*(..)) || execution(* com.study.lab.service..*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        // TODO(实验): 在 proceed 之前打印 "Aspect 进入" + 方法签名

        Object result = pjp.proceed(); // 执行目标方法，勿删（删掉目标方法就不执行了）

        // TODO(实验): 在 proceed 之后打印 "Aspect 返回"，任务二在此补充耗时
        return result;
    }
}
