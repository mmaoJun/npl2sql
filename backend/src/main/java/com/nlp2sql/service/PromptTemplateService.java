package com.nlp2sql.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    @Value("${prompt.version:v1}")
    private String promptVersion;

    private Map<String, Object> config;
    private String systemPromptTemplate;
    private String retryFeedbackTemplate;
    private Map<String, List<String>> keywords;
    private Map<String, List<Map<String, String>>> examples;

    @PostConstruct
    public void init() {
        loadTemplate(promptVersion);
    }

    public void loadTemplate(String version) {
        String path = "prompts/" + version + ".yml";
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            Yaml yaml = new Yaml();
            config = yaml.load(is);
            systemPromptTemplate = (String) config.get("system_prompt");
            retryFeedbackTemplate = (String) config.get("retry_feedback");
            keywords = castMap(config.get("keywords"));
            examples = castNestedMap(config.get("examples"));
            log.info("加载提示词模板: {} ({})", version, config.get("description"));
        } catch (Exception e) {
            log.error("加载提示词模板失败: {}", path, e);
            throw new RuntimeException("无法加载提示词模板: " + path, e);
        }
    }

    public String getSystemPrompt(String dialect) {
        return systemPromptTemplate.replace("{dialect}", dialect);
    }

    public String getRetryFeedback(String previousSql, String errorMessage) {
        return retryFeedbackTemplate
                .replace("{previousSql}", previousSql)
                .replace("{errorMessage}", errorMessage);
    }

    public List<String> getKeywords(String queryType) {
        return keywords.getOrDefault(queryType, List.of());
    }

    public List<Map<String, String>> getExamples(String queryType) {
        return examples.getOrDefault(queryType, List.of());
    }

    public String getVersion() {
        return promptVersion;
    }

    public Set<String> getQueryTypes() {
        return examples.keySet();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<String>> castMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Map<String, List<String>> result = new LinkedHashMap<>();
            for (var entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), (List<String>) entry.getValue());
            }
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<Map<String, String>>> castNestedMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Map<String, List<Map<String, String>>> result = new LinkedHashMap<>();
            for (var entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), (List<Map<String, String>>) entry.getValue());
            }
            return result;
        }
        return Map.of();
    }
}
