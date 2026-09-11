package com.study.lab.testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 方式一：纯单元测试（不启动 Spring）。
 * 直接 new 出被测类，调用方法断言。观察它能测到什么、测不到什么。
 *
 * 提示：GreetingService 没有依赖，可直接 new。若被测类有依赖，则用 Mockito 造假。
 */
class GreetingServiceUnitTest {

    @Test
    @DisplayName("greet 正常返回问候语")
    void greet_returnsMessage() {
        // TODO: new GreetingService()，调用 greet("Kiro")，用 AssertJ 断言返回 "Hello, Kiro"
    }

    @Test
    @DisplayName("greet 传入空白 name 抛 IllegalArgumentException")
    void greet_blankName_throws() {
        // TODO: 用 assertThatThrownBy 断言 greet("") 抛 IllegalArgumentException
    }
}
