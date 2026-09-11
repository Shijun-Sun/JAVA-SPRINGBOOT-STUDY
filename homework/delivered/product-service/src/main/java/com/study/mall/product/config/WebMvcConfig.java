package com.study.mall.product.config;

import com.study.mall.product.common.web.AccessLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AccessLogInterceptor accessLogInterceptor;

    public WebMvcConfig(AccessLogInterceptor accessLogInterceptor) {
        this.accessLogInterceptor = accessLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry interceptorRegistry) {
        interceptorRegistry.addInterceptor(this.accessLogInterceptor).addPathPatterns("/api/**").excludePathPatterns("/actuator/**");
    }
}
