package com.nlp2sql.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class OllamaConfig {

    @Value("${ollama.base-url}")
    private String baseUrl;

    @Value("${ollama.model}")
    private String model;

    @Value("${ollama.temperature}")
    private double temperature;

    @Value("${ollama.num-ctx}")
    private int numCtx;

    @Value("${ollama.timeout}")
    private int timeout;

    @Bean
    public OkHttpClient ollamaClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout * 2, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    public String getBaseUrl() { return baseUrl; }
    public String getModel() { return model; }
    public double getTemperature() { return temperature; }
    public int getNumCtx() { return numCtx; }
    public int getTimeout() { return timeout; }
}
