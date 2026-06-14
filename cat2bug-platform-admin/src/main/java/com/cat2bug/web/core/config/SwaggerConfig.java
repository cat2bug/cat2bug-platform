package com.cat2bug.web.core.config;

import com.cat2bug.common.config.Cat2BugConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 / springdoc 配置（替代 springfox）
 */
@Configuration
public class SwaggerConfig {

    @Autowired
    private Cat2BugConfig ruoyiConfig;

    @Value("${swagger.enabled:true}")
    private boolean enabled;

    @Bean
    public OpenAPI openAPI() {
        if (!enabled) {
            return new OpenAPI();
        }
        final String schemeName = "Authorization";
        return new OpenAPI()
                .info(new Info()
                        .title("Cat2Bug-Platform系统API接口文档")
                        .description("描述：用于管理Cat2Bug系统中缺陷、测试用例等模块")
                        .version("版本号:" + ruoyiConfig.getVersion())
                        .contact(new Contact().name(ruoyiConfig.getName())))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName,
                                new SecurityScheme()
                                        .name(schemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)));
    }

  @Bean
  public GroupedOpenApi apiGroup() {
    return GroupedOpenApi.builder()
        .group("api")
        .packagesToScan("com.cat2bug.api.controller")
        .build();
  }
}
