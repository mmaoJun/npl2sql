package com.nlp2sql.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 智能查询请求体。
 *
 * @see ChatResponse
 */
@Data
@Schema(description = "智能查询请求")
public class ChatRequest {

    /** 用户输入的自然语言文本 */
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "自然语言问题", example = "查询所有用户的数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    /** 目标数据源 ID */
    @NotNull(message = "数据源ID不能为空")
    @Schema(description = "数据源 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long datasourceId;
}
