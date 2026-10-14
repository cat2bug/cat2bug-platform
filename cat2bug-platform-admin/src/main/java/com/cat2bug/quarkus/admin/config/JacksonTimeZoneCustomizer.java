package com.cat2bug.quarkus.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

import java.util.TimeZone;

/**
 * JSON 日期按 JVM 默认时区输出（Quarkus 默认 UTC）；须在运行期读取，Native 才能跟随容器 TZ。
 */
@Singleton
public class JacksonTimeZoneCustomizer implements ObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setTimeZone(TimeZone.getDefault());
    }
}
