package com.nlp2sql.service;

import com.nlp2sql.config.ModelProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * SQL 生成服务。
 *
 * <p>通过 Spring AI {@link ChatModel} 调用大语言模型，将 Prompt 转换为 SQL。
 * 支持同步生成和流式输出两种模式，并提供模型原始输出到纯 SQL 的提取能力。
 *
 * @see ModelProperties
 * @see ChatModel
 */
@Service
@Slf4j
public class SQLGenerator {

    private final ChatModel chatModel;
    private final ModelProperties modelProperties;

    public SQLGenerator(ChatModel chatModel, ModelProperties modelProperties) {
        this.chatModel = chatModel;
        this.modelProperties = modelProperties;
    }

    /**
     * 同步生成 SQL。
     *
     * <p>调用大语言模型生成 SQL，自动提取 Markdown 代码块和分号。
     *
     * @param prompt 包含系统指令和用户消息的结构化 Prompt
     * @return 提取后的纯 SQL 字符串
     */
    public String generate(Prompt prompt) {
        log.debug("使用模型 [{}] 生成 SQL, provider={}", modelProperties.getActiveModelName(), modelProperties.getProvider());

        ChatResponse response = chatModel.call(prompt);

        String rawOutput = response.getResult().getOutput().getText();
        return extractSql(rawOutput);
    }

    /**
     * 流式生成 SQL。
     *
     * <p>以 Reactive Stream 方式逐 Token 返回模型输出，适用于 SSE 流式响应场景。
     *
     * @param prompt 包含系统指令和用户消息的结构化 Prompt
     * @return 逐 Token 输出的文本流
     */
    public Flux<String> generateStream(Prompt prompt) {
        return chatModel.stream(prompt)
                .map(chatResponse -> {
                    if (chatResponse.getResult() != null && chatResponse.getResult().getOutput().getText() != null) {
                        return chatResponse.getResult().getOutput().getText();
                    }
                    return "";
                })
                .filter(token -> !token.isEmpty());
    }

    /**
     * 从模型原始输出中提取纯 SQL。
     *
     * <p>处理三种常见情况：Markdown 代码块包裹（```sql ... ```）、末尾分号、前后空白。
     *
     * @param rawOutput 模型返回的原始文本
     * @return 提取后的纯 SQL；输入为空时返回空字符串
     */
    public String extractSql(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            return "";
        }

        String sql = rawOutput.trim();

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

        int semicolonIdx = sql.indexOf(';');
        if (semicolonIdx > 0) {
            sql = sql.substring(0, semicolonIdx);
        }

        return sql.trim();
    }
}
