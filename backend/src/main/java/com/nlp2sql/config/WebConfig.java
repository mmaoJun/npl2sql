package com.nlp2sql.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Web 配置。
 *
 * <p>配置 CORS 策略：允许前端开发服务器（localhost:5173/3000）跨域访问，
 * 暴露 {@code X-Trace-Id} 响应头供前端链路追踪。
 */
@Configuration
public class WebConfig {

    /**
     * 创建 CORS 过滤器。
     *
     * @return 配置好允许源、方法和头部的 CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("http://localhost:5173");
        config.addAllowedOrigin("http://localhost:3000");
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.addExposedHeader("X-Trace-Id");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
