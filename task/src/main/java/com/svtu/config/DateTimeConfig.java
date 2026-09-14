package com.svtu.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 专门适配：MySQL datetime → Java Date
 * 全局时区：中国上海
 * 全局格式：yyyy-MM-dd HH:mm:ss
 */
@Configuration
public class DateTimeConfig implements WebMvcConfigurer {

    // 标准格式（和数据库 datetime 完全匹配）
    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    // 中国时区（核心！）
    private static final String TIME_ZONE = "Asia/Shanghai";

    // ====================== 1. 启动就强制设置 JVM 时区为上海 ======================
    // 解决：数据库 datetime 读取时差问题
    @Bean
    public void setDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(TIME_ZONE));
    }

    // ====================== 2. JSON 日期格式化（出参）======================
    // 解决：返回给前端时 Date 自动转为北京时间字符串
    @Bean
    public Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));

        builder.dateFormat(sdf);
        builder.timeZone(TIME_ZONE);
        builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return builder;
    }

    // ====================== 3. 前端参数转 Date（入参）======================
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, Date>() {
            @Override
            public Date convert(String source) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT);
                    sdf.setTimeZone(TimeZone.getTimeZone(TIME_ZONE));
                    return sdf.parse(source.trim());
                } catch (Exception e) {
                    throw new IllegalArgumentException("日期格式错误，请使用：" + DATE_TIME_FORMAT);
                }
            }
        });
    }
}
