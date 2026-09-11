package com.study.mall.product.common.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AccessLogInterceptor.class);

    private static final String START_TIME = "start-time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long now = System.currentTimeMillis();
        request.setAttribute(START_TIME, now);
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {

        long startTime = Long.parseLong(request.getAttribute(START_TIME).toString());
        // 耗时
        Long spentTime = System.currentTimeMillis() - startTime;

        // 填日志
        log.info("{} {} -> {} ({}ms)", request.getMethod(), request.getRequestURI(), response.getStatus(), spentTime);

        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
