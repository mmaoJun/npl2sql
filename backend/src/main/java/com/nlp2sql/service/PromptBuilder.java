package com.nlp2sql.service;

import com.nlp2sql.model.TableInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PromptBuilder {

    private final SchemaCompressor schemaCompressor;
    private final QueryClassifier queryClassifier;
    private final PromptTemplateService promptTemplate;

    public PromptBuilder(SchemaCompressor schemaCompressor,
                         QueryClassifier queryClassifier,
                         PromptTemplateService promptTemplate) {
        this.schemaCompressor = schemaCompressor;
        this.queryClassifier = queryClassifier;
        this.promptTemplate = promptTemplate;
    }

    public String build(String nlInput, List<TableInfo> tables, String dialect) {
        QueryClassifier.QueryType queryType = queryClassifier.classify(nlInput);

        StringBuilder prompt = new StringBuilder();
        prompt.append(promptTemplate.getSystemPrompt(dialect)).append("\n");

        String schema = schemaCompressor.compress(tables, nlInput);
        prompt.append(schema).append("\n\n");

        appendExamples(prompt, queryType, 3);

        prompt.append("问题: ").append(nlInput).append("\n");
        prompt.append("SQL:");

        return prompt.toString();
    }

    public String buildWithRetry(String nlInput, List<TableInfo> tables, String dialect,
                                  String previousSql, String errorMessage) {
        QueryClassifier.QueryType queryType = queryClassifier.classify(nlInput);

        StringBuilder prompt = new StringBuilder();
        prompt.append(promptTemplate.getSystemPrompt(dialect)).append("\n");

        String schema = schemaCompressor.compress(tables, nlInput);
        prompt.append(schema).append("\n\n");

        appendExamples(prompt, queryType, 2);

        prompt.append(promptTemplate.getRetryFeedback(previousSql, errorMessage)).append("\n\n");

        prompt.append("问题: ").append(nlInput).append("\n");
        prompt.append("SQL:");

        return prompt.toString();
    }

    private void appendExamples(StringBuilder prompt, QueryClassifier.QueryType queryType, int maxCount) {
        List<Map<String, String>> examples = queryClassifier.getFewShotExamples(queryType);
        if (!examples.isEmpty()) {
            prompt.append("示例:\n");
            int count = Math.min(examples.size(), maxCount);
            for (int i = 0; i < count; i++) {
                Map<String, String> ex = examples.get(i);
                prompt.append("问题: ").append(ex.get("question")).append("\n");
                prompt.append("SQL: ").append(ex.get("sql")).append("\n\n");
            }
        }
    }
}
