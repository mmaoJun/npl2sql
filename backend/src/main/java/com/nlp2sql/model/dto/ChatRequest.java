package com.nlp2sql.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;

    @NotNull(message = "数据源ID不能为空")
    private Long datasourceId;
}
