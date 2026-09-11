package com.study.lab.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 【实验待补全】Interceptor 是 Spring MVC 层，居于 Filter 与 AOP 之间。
 * 初始状态已能编译运行：preHandle 直接放行，其余方法为空。
 *
 * 任务：在三个方法里各打印一行日志（preHandle / postHandle / afterCompletion），
 * 观察它们之间以及相对 Filter、Aspect 的顺序。
 * 特别注意：把接口改成抛异常（/demo?fail=true）时，postHandle 与 afterCompletion
 * 谁还会执行。
 */
public class OrderLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(OrderLogInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TODO(实验): 打印 "Interceptor.preHandle"
        return true; // 放行，勿改为 false
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) {
        // TODO(实验): 打印 "Interceptor.postHandle"
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // TODO(实验): 打印 "Interceptor.afterCompletion"，并观察 ex 是否为 null
    }
}
