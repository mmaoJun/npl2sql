package com.nlp2sql.service;

import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 查询分类器。
 *
 * <p>基于关键词匹配将用户自然语言输入分类为 8 种查询类型，
 * 用于 {@link PromptBuilder} 选取对应的 Few-Shot 示例。
 * 匹配优先级：SUBQUERY > TOP_N > TREND > AGGREGATION > FUZZY > JOIN > WHERE_FILTER。
 *
 * @see PromptTemplateService
 * @see PromptBuilder
 */
@Service
public class QueryClassifier {

    /**
     * 查询类型枚举。
     *
     * <p>按 SQL 复杂度从低到高排列，每种类型对应一组关键词和 Few-Shot 示例。
     */
    public enum QueryType {
        SIMPLE_SELECT,
        WHERE_FILTER,
        AGGREGATION,
        JOIN,
        SUBQUERY,
        TOP_N,
        TREND,
        FUZZY
    }

    private final PromptTemplateService promptTemplate;

    private static final List<QueryType> PRIORITY_ORDER = List.of(
            QueryType.SUBQUERY, QueryType.TOP_N, QueryType.TREND,
            QueryType.AGGREGATION, QueryType.FUZZY, QueryType.JOIN,
            QueryType.WHERE_FILTER
    );

    public QueryClassifier(PromptTemplateService promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    /**
     * 对用户输入进行查询分类。
     *
     * <p>按优先级依次匹配关键词，首个命中即返回；无命中时默认 SIMPLE_SELECT。
     *
     * @param nlInput 用户自然语言输入
     * @return 匹配到的查询类型
     */
    public QueryType classify(String nlInput) {
        if (nlInput == null || nlInput.isBlank()) {
            return QueryType.SIMPLE_SELECT;
        }

        String input = nlInput.toLowerCase();

        for (QueryType type : PRIORITY_ORDER) {
            List<String> keywords = promptTemplate.getKeywords(type.name());
            for (String keyword : keywords) {
                if (input.contains(keyword.toLowerCase())) {
                    return type;
                }
            }
        }

        return QueryType.SIMPLE_SELECT;
    }

    /**
     * 获取指定查询类型的 Few-Shot 示例。
     *
     * @param type    查询类型
     * @param dialect 数据库方言，用于选取方言专属示例；为 null 时返回通用示例
     * @return 示例列表，每个示例包含 question 和 sql 字段
     */
    public List<Map<String, String>> getFewShotExamples(QueryType type, String dialect) {
        return promptTemplate.getExamples(type.name(), dialect);
    }
}
