package com.study.mall.product.controller;

import com.study.mall.product.config.ProductConfig;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CurrentProductConfigController {

    private final ProductConfig productConfig;
    private final Environment environment;

    CurrentProductConfigController(final ProductConfig productConfig, final Environment environment) {
        this.productConfig = productConfig;
        this.environment = environment;
    }

    @GetMapping("/config-info")
    public Map<String, Object> configInfo() {
        return Map.of(
                "activeProfiles", environment.getActiveProfiles(),
                "name", productConfig.name(),
                "currency", productConfig.pricing().currency(),
                "maxPageSize", productConfig.pagination().maxSize(),
                "cacheTime", productConfig.pricing().cacheTime().toString(),
                "allowedImageTypes", productConfig.storage().allowedImages()
        );
    }
}
