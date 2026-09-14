package com.svtu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        // 1. 创建 CORS 配置对象
        CorsConfiguration config = new CorsConfiguration();

        // 2. 允许所有域名访问（生产环境可替换为具体前端域名）
        config.addAllowedOriginPattern("*");

        // 3. 允许携带凭证（如 Cookie、Authorization 头）
        config.setAllowCredentials(true);

        // 4. 允许所有请求头
        config.addAllowedHeader("*");

        // 5. 允许所有请求方法（GET、POST、PUT、DELETE、OPTIONS 等）
        config.addAllowedMethod("*");

        // 6. 注册配置，拦截所有路径
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}