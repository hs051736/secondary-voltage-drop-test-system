package com.hspyx.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("二次压降检测系统 API")
                        .version("1.0.0")
                        .description("## 接口说明\n\n" +
                                "本系统采用 JWT Token 无状态认证机制。\n\n" +
                                "### 认证方式\n\n" +
                                "1. 调用 `POST /api/auth/login` 登录获取 Token\n" +
                                "2. 将 Token 添加到请求头：`Authorization: Bearer <token>`\n\n" +
                                "### 接口分组\n\n" +
                                "- **认证接口**：登录、注册（无需 Token）\n" +
                                "- **业务接口**：检测记录管理（需要 Token）")
                        .contact(new Contact()
                                .name("技术支持")
                                .email("support@hspyx.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT Token 认证\n\n" +
                                        "**获取方式**：\n" +
                                        "1. 调用登录接口 `POST /api/auth/login`\n" +
                                        "2. 在响应中获取 `token` 字段\n" +
                                        "3. 在请求头中使用：`Authorization: Bearer <token>`")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
