package com.nlp2sql.service;

import com.nlp2sql.model.TableInfo;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Prompt 构建服务。
 *
 * <p>将系统指令、压缩后的 Schema、Few-Shot 示例和用户问题拼装为完整的 LLM Prompt。
 * 支持首次构建和重试构建（携带错误反馈）两种模式。
 *
 * @see SchemaCompressor
 * @see QueryClassifier
 * @see PromptTemplateService
 */
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

    /**
     * 构建首次查询的完整 Prompt。
     *
     * <p>拼装顺序：系统指令 → 压缩 Schema → Few-Shot 示例（最多 3 条）→ 用户问题。
     *
     * @param nlInput 用户自然语言输入
     * @param tables  当前数据源的表结构列表
     * @param dialect 数据库方言
     * @return 包含 SystemMessage 和 UserMessage 的结构化 Prompt
     */
    public Prompt build(String nlInput, List<TableInfo> tables, String dialect) {
        QueryClassifier.QueryType queryType = queryClassifier.classify(nlInput);

        String systemPrompt = promptTemplate.getSystemPrompt(dialect);

        StringBuilder userContent = new StringBuilder();
        String schema = schemaCompressor.compress(tables, nlInput);
        userContent.append(schema).append("\n\n");

        appendExamples(userContent, queryType, dialect, 3);

        userContent.append("问题: ").append(nlInput).append("\n");
        userContent.append("SQL:");

        return new Prompt(new SystemMessage(systemPrompt), new UserMessage(userContent.toString()));
    }

    /**
     * 构建重试 Prompt，携带上次生成的 SQL 和错误反馈。
     *
     * <p>在首次 Prompt 基础上插入重试反馈段落，引导模型修正错误 SQL。
     * Few-Shot 示例数量减少为 2 条以留出 Token 空间。
     *
     * @param nlInput      用户自然语言输入
     * @param tables       当前数据源的表结构列表
     * @param dialect      数据库方言
     * @param previousSql  上次生成的 SQL
     * @param errorMessage 校验或执行错误信息
     * @return 包含错误反馈的结构化 Prompt
     */
    public Prompt buildWithRetry(String nlInput, List<TableInfo> tables, String dialect,
                                  String previousSql, String errorMessage) {
        QueryClassifier.QueryType queryType = queryClassifier.classify(nlInput);

        String systemPrompt = promptTemplate.getSystemPrompt(dialect);

        StringBuilder userContent = new StringBuilder();
        String schema = schemaCompressor.compress(tables, nlInput);
        userContent.append(schema).append("\n\n");

        appendExamples(userContent, queryType, dialect, 2);

        userContent.append(promptTemplate.getRetryFeedback(previousSql, errorMessage)).append("\n\n");

        userContent.append("问题: ").append(nlInput).append("\n");
        userContent.append("SQL:");

        return new Prompt(new SystemMessage(systemPrompt), new UserMessage(userContent.toString()));
    }

    private void appendExamples(StringBuilder prompt, QueryClassifier.QueryType queryType,
                                String dialect, int maxCount) {
        List<Map<String, String>> examples = queryClassifier.getFewShotExamples(queryType, dialect);
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
