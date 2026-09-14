package com.svtu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前端访问路径：/avatar/xxx.jpg
        // 映射到本地：D:/upload/avatar/
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:D:/upload/avatar/");
    }
}