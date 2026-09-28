package com.nlp2sql.service;

import com.nlp2sql.model.TableInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilder {

    private final SchemaCompressor schemaCompressor;
    private final QueryClassifier queryClassifier;

    public PromptBuilder(SchemaCompressor schemaCompressor, QueryClassifier queryClassifier) {
        this.schemaCompressor = schemaCompressor;
        this.queryClassifier = queryClassifier;
    }

    public String build(String nlInput, List<TableInfo> tables, String dialect) {
        QueryClassifier.QueryType queryType = queryClassifier.classify(nlInput);

        StringBuilder prompt = new StringBuilder();

        // System prompt
        prompt.append("""
                你是一个SQL专家。根据提供的数据库表结构，将用户的自然语言问题转换为SQL查询语句。

                规则：
                1. 只生成SELECT语句，禁止INSERT/UPDATE/DELETE/DROP/ALTER
                2. 使用标准SQL语法，数据库类型为: %s
                3. 只使用提供的表结构中的表和字段
                4. 如果无法生成SQL，回复"无法理解该查询"
                5. 只输出SQL语句，不要解释，不要输出markdown标记

                """.formatted(dialect));

        // Schema context
        String schema = schemaCompressor.compress(tables, nlInput);
        prompt.append(schema).append("\n\n");

        // Few-shot examples
        List<String> examples = queryClassifier.getFewShotExamples(queryType);
        if (!examples.isEmpty()) {
            prompt.append("示例:\n");
            int count = Math.min(examples.size(), 3);
            for (int i = 0; i < count; i++) {
                prompt.append(examples.get(i)).append("\n\n");
            }
        }

        // User input
        prompt.append("问题: ").append(nlInput).append("\n");
        prompt.append("SQL:");

        return prompt.toString();
    }

    public String buildWithRetry(String nlInput, List<TableInfo> tables, String dialect,
                                  String previousSql, String errorMessage) {
        QueryClassifier.QueryType queryType = queryClassifier.classify(nlInput);

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                你是一个SQL专家。根据提供的数据库表结构，将用户的自然语言问题转换为SQL查询语句。

                规则：
                1. 只生成SELECT语句，禁止INSERT/UPDATE/DELETE/DROP/ALTER
                2. 使用标准SQL语法，数据库类型为: %s
                3. 只使用提供的表结构中的表和字段
                4. 如果无法生成SQL，回复"无法理解该查询"
                5. 只输出SQL语句，不要解释，不要输出markdown标记

                """.formatted(dialect));

        String schema = schemaCompressor.compress(tables, nlInput);
        prompt.append(schema).append("\n\n");

        List<String> examples = queryClassifier.getFewShotExamples(queryType);
        if (!examples.isEmpty()) {
            prompt.append("示例:\n");
            int count = Math.min(examples.size(), 2);
            for (int i = 0; i < count; i++) {
                prompt.append(examples.get(i)).append("\n\n");
            }
        }

        // 加入错误反馈
        prompt.append("之前生成的SQL有误:\n");
        prompt.append("SQL: ").append(previousSql).append("\n");
        prompt.append("错误: ").append(errorMessage).append("\n\n");
        prompt.append("请修正上述SQL。注意: ").append(errorMessage).append("\n\n");

        prompt.append("问题: ").append(nlInput).append("\n");
        prompt.append("SQL:");

        return prompt.toString();
    }
}
