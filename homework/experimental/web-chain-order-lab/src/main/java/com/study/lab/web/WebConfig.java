package com.study.lab.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册实验用的 Filter 和 Interceptor。
 * 这些注册代码本身不是实验考点，无需改动；实验重点是三个类的方法体（TODO）。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<OrderLogFilter> orderLogFilter() {
        FilterRegistrationBean<OrderLogFilter> reg = new FilterRegistrationBean<>(new OrderLogFilter());
        reg.addUrlPatterns("/*");
        reg.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return reg;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new OrderLogInterceptor()).addPathPatterns("/**");
    }
}
