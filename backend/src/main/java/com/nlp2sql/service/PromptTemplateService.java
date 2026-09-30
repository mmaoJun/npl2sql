package com.nlp2sql.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

/**
 * 提示词模板服务。
 *
 * <p>从 classpath 下的 YAML 文件加载提示词模板，包含系统指令、重试反馈模板、
 * 查询分类关键词和 Few-Shot 示例。启动时通过 {@code @PostConstruct} 自动加载，
 * 支持运行时热切换模板版本。
 *
 * <p>模板文件路径格式：{@code prompts/{version}.yml}
 *
 * @see QueryClassifier
 * @see PromptBuilder
 */
@Service
@Slf4j
public class PromptTemplateService {

    @Value("${prompt.version:v1}")
    private String promptVersion;

    private Map<String, Object> config;
    private String systemPromptTemplate;
    private String retryFeedbackTemplate;
    private Map<String, List<String>> keywords;
    private Map<String, List<Map<String, String>>> examples;
    private Map<String, Map<String, List<Map<String, String>>>> dialectOverrides;

    /**
     * 启动时加载默认版本的提示词模板。
     */
    @PostConstruct
    public void init() {
        loadTemplate(promptVersion);
    }

    /**
     * 加载指定版本的提示词模板。
     *
     * @param version 模板版本号，对应 {@code prompts/} 目录下的文件名（不含后缀）
     * @throws RuntimeException 模板文件不存在或解析失败时抛出
     */
    public void loadTemplate(String version) {
        String path = "prompts/" + version + ".yml";
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            Yaml yaml = new Yaml();
            config = yaml.load(is);
            systemPromptTemplate = (String) config.get("system_prompt");
            retryFeedbackTemplate = (String) config.get("retry_feedback");
            keywords = castMap(config.get("keywords"));
            examples = castNestedMap(config.get("examples"));
            dialectOverrides = castDialectMap(config.get("dialect_overrides"));
            log.info("加载提示词模板: {} ({})", version, config.get("description"));
        } catch (Exception e) {
            log.error("加载提示词模板失败: {}", path, e);
            throw new RuntimeException("无法加载提示词模板: " + path, e);
        }
    }

    /**
     * 获取系统指令模板，替换方言占位符后返回。
     *
     * @param dialect 数据库方言（如 mysql、postgresql）
     * @return 替换 {dialect} 后的系统指令文本
     */
    public String getSystemPrompt(String dialect) {
        return new PromptTemplate(systemPromptTemplate).render(Map.of("dialect", dialect));
    }

    /**
     * 获取重试反馈文本，将上次生成的 SQL 和错误信息嵌入模板。
     *
     * @param previousSql  上次生成的 SQL
     * @param errorMessage 校验或执行错误信息
     * @return 替换占位符后的重试反馈文本
     */
    public String getRetryFeedback(String previousSql, String errorMessage) {
        return new PromptTemplate(retryFeedbackTemplate).render(Map.of(
                "previousSql", previousSql, "errorMessage", errorMessage));
    }

    /**
     * 获取指定查询类型的分类关键词列表。
     *
     * @param queryType 查询类型名称（如 AGGREGATION、JOIN）
     * @return 关键词列表；类型不存在时返回空列表
     */
    public List<String> getKeywords(String queryType) {
        return keywords.getOrDefault(queryType, List.of());
    }

    /**
     * 获取指定查询类型的 Few-Shot 示例列表。
     *
     * @param queryType 查询类型名称
     * @return 示例列表，每个示例包含 question 和 sql 字段；类型不存在时返回空列表
     */
    public List<Map<String, String>> getExamples(String queryType) {
        return examples.getOrDefault(queryType, List.of());
    }

    /**
     * 获取指定查询类型和方言的 Few-Shot 示例列表。
     *
     * <p>优先返回 {@code dialect_overrides} 中该方言的专属示例（如 PostgreSQL 的
     * {@code to_char} 时间函数示例），未配置时回退到 {@code examples} 中的通用示例。
     *
     * @param queryType 查询类型名称
     * @param dialect   数据库方言（如 mysql、postgresql）
     * @return 示例列表；均不存在时返回空列表
     */
    public List<Map<String, String>> getExamples(String queryType, String dialect) {
        if (dialect != null && dialectOverrides != null) {
            Map<String, List<Map<String, String>>> byDialect = dialectOverrides.get(dialect.toLowerCase());
            if (byDialect != null && byDialect.containsKey(queryType)) {
                return byDialect.get(queryType);
            }
        }
        return getExamples(queryType);
    }

    /**
     * 获取当前加载的模板版本号。
     *
     * @return 模板版本号
     */
    public String getVersion() {
        return promptVersion;
    }

    /**
     * 获取模板中定义的所有查询类型。
     *
     * @return 查询类型名称集合（如 [SIMPLE_SELECT, AGGREGATION, JOIN, ...]）
     */
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

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, List<Map<String, String>>>> castDialectMap(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            Map<String, Map<String, List<Map<String, String>>>> result = new LinkedHashMap<>();
            for (var entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), castNestedMap(entry.getValue()));
            }
            return result;
        }
        return Map.of();
    }
}
