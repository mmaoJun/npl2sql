package com.nlp2sql.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nlp2sql.config.OllamaConfig;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class SQLGenerator {

    private static final Logger log = LoggerFactory.getLogger(SQLGenerator.class);

    private final OkHttpClient ollamaClient;
    private final OllamaConfig ollamaConfig;
    private final ObjectMapper objectMapper;

    public SQLGenerator(OkHttpClient ollamaClient, OllamaConfig ollamaConfig) {
        this.ollamaClient = ollamaClient;
        this.ollamaConfig = ollamaConfig;
        this.objectMapper = new ObjectMapper();
    }

    public String generate(String prompt) throws IOException {
        String json = objectMapper.createObjectNode()
                .put("model", ollamaConfig.getModel())
                .put("prompt", prompt)
                .put("stream", false)
                .put("temperature", ollamaConfig.getTemperature())
                .set("options", objectMapper.createObjectNode()
                        .put("num_ctx", ollamaConfig.getNumCtx()))
                .toString();

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(ollamaConfig.getBaseUrl() + "/api/generate")
                .post(body)
                .build();

        try (Response response = ollamaClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Ollama API 调用失败: " + response.code());
            }
            String responseBody = response.body().string();
            JsonNode result = objectMapper.readTree(responseBody);
            String rawOutput = result.get("response").asText();
            return extractSql(rawOutput);
        }
    }

    public void generateStream(String prompt, Consumer<String> onToken, Runnable onComplete) {
        String json;
        try {
            json = objectMapper.createObjectNode()
                    .put("model", ollamaConfig.getModel())
                    .put("prompt", prompt)
                    .put("stream", true)
                    .put("temperature", ollamaConfig.getTemperature())
                    .set("options", objectMapper.createObjectNode()
                            .put("num_ctx", ollamaConfig.getNumCtx()))
                    .toString();
        } catch (Exception e) {
            throw new RuntimeException("构建请求失败", e);
        }

        RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(ollamaConfig.getBaseUrl() + "/api/generate")
                .post(body)
                .build();

        EventSource.Factory factory = EventSources.createFactory(ollamaClient);
        factory.newEventSource(request, new EventSourceListener() {
            @Override
            public void onEvent(EventSource eventSource, String id, String type, String data) {
                try {
                    JsonNode node = objectMapper.readTree(data);
                    String token = node.get("response").asText("");
                    boolean done = node.has("done") && node.get("done").asBoolean();
                    if (!token.isEmpty()) {
                        onToken.accept(token);
                    }
                    if (done) {
                        onComplete.run();
                    }
                } catch (Exception e) {
                    log.error("解析流式响应失败: {}", e.getMessage());
                }
            }

            @Override
            public void onFailure(EventSource eventSource, Throwable t, Response response) {
                log.error("流式调用失败: {}", t != null ? t.getMessage() : "unknown");
                onComplete.run();
            }
        });
    }

    public String extractSql(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return "";
        }

        String sql = rawOutput.trim();

        // 去除 markdown 代码块
        if (sql.startsWith("```")) {
            int firstNewline = sql.indexOf('\n');
            if (firstNewline > 0) {
                sql = sql.substring(firstNewline + 1);
            }
            int lastBacktick = sql.lastIndexOf("```");
            if (lastBacktick > 0) {
                sql = sql.substring(0, lastBacktick);
            }
            sql = sql.trim();
        }

        // 提取第一个完整的 SQL 语句（去除分号）
        int semicolonIdx = sql.indexOf(';');
        if (semicolonIdx > 0) {
            sql = sql.substring(0, semicolonIdx);
        }

        return sql.trim();
    }
}
