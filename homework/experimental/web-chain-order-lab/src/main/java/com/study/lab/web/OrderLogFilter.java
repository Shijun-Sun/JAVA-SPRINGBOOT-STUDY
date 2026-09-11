package com.study.lab.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 【实验待补全】Filter 是链路最外圈。
 * 初始状态已能编译运行：仅放行请求，不打印任何顺序日志。
 *
 * 任务：在 chain.doFilter 之前和之后各打印一行日志，
 * 例如 "1. Filter 进入" / "8. Filter 返回"，用来观察它相对
 * Interceptor、Aspect 的先后位置。
 */
public class OrderLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OrderLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // TODO(实验): 在这里打印 "Filter 进入"

        chain.doFilter(request, response); // 放行到下一环节，勿删

        // TODO(实验): 在这里打印 "Filter 返回"
    }
}
