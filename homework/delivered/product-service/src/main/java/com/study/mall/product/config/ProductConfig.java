package com.study.mall.product.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;

@Validated
@ConfigurationProperties(prefix = "product")
public record ProductConfig(

        @NotBlank
        String name,

        @NotNull @Valid
        Pricing pricing,

        @NotNull @Valid
        Pagination pagination,

        @NotNull @Valid
        Storage storage

) {

    public record Pricing(
            @NotBlank
            String currency,

            @NotNull
            Duration cacheTime
    ) {}

    public record Pagination(
            @Min(1) @Max(500)
            int maxSize
    ) {}

    public record Storage(
            @NotBlank
            String endpoint,

            @NotEmpty
            List<String> allowedImages
    ) {}
}
