package com.nlp2sql.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QueryClassifier {

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

    public List<Map<String, String>> getFewShotExamples(QueryType type) {
        return promptTemplate.getExamples(type.name());
    }
}
