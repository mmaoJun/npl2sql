package com.nlp2sql.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 智能查询响应体。
 *
 * <p>包含查询是否成功、生成的 SQL、查询结果和性能指标。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "智能查询响应")
public class ChatResponse {

    /** 查询是否成功 */
    @Schema(description = "是否成功")
    private boolean success;

    /** 用户输入的自然语言 */
    @Schema(description = "原始自然语言输入")
    private String nlInput;

    /** 模型生成的 SQL */
    @Schema(description = "生成的 SQL 语句")
    private String generatedSql;

    /** 查询结果列名列表 */
    @Schema(description = "结果列名")
    private List<String> columns;

    /** 查询结果数据行 */
    @Schema(description = "结果数据行")
    private List<Map<String, Object>> rows;

    /** 结果行数 */
    @Schema(description = "结果行数")
    private int rowCount;

    /** 执行耗时（毫秒） */
    @Schema(description = "执行耗时（毫秒）")
    private long executionTimeMs;

    /** 使用的模型名称 */
    @Schema(description = "使用的模型名称")
    private String modelName;

    /** 错误消息，成功时为 null */
    @Schema(description = "错误消息")
    private String errorMessage;

    /**
     * 构建错误响应。
     *
     * @param nlInput      原始自然语言输入
     * @param errorMessage 错误消息
     * @return 错误响应
     */
    public static ChatResponse error(String nlInput, String errorMessage) {
        return ChatResponse.builder()
                .success(false)
                .nlInput(nlInput)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 构建成功响应。
     *
     * @param nlInput         原始自然语言输入
     * @param sql             生成的 SQL
     * @param columns         结果列名
     * @param rows            结果数据行
     * @param executionTimeMs 执行耗时（毫秒）
     * @param modelName       使用的模型名称
     * @return 成功响应
     */
    public static ChatResponse success(String nlInput, String sql,
                                        List<String> columns,
                                        List<Map<String, Object>> rows,
                                        long executionTimeMs,
                                        String modelName) {
        return ChatResponse.builder()
                .success(true)
                .nlInput(nlInput)
                .generatedSql(sql)
                .columns(columns)
                .rows(rows)
                .rowCount(rows.size())
                .executionTimeMs(executionTimeMs)
                .modelName(modelName)
                .build();
    }
}
