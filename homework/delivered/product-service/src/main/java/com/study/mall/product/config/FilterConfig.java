package com.study.mall.product.config;

import com.study.mall.product.common.web.TraceIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;


@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<TraceIdFilter> traceIdFilter() {
        FilterRegistrationBean<TraceIdFilter> frb = new FilterRegistrationBean<>();
        frb.setFilter(new TraceIdFilter());
        frb.addUrlPatterns("/*");
        frb.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return frb;
    }
}
