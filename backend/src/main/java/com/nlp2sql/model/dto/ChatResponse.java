package com.nlp2sql.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private boolean success;

    private String nlInput;

    private String generatedSql;

    private List<String> columns;

    private List<Map<String, Object>> rows;

    private int rowCount;

    private long executionTimeMs;

    private String errorMessage;

    public static ChatResponse error(String nlInput, String errorMessage) {
        return ChatResponse.builder()
                .success(false)
                .nlInput(nlInput)
                .errorMessage(errorMessage)
                .build();
    }

    public static ChatResponse success(String nlInput, String sql,
                                        List<String> columns,
                                        List<Map<String, Object>> rows,
                                        long executionTimeMs) {
        return ChatResponse.builder()
                .success(true)
                .nlInput(nlInput)
                .generatedSql(sql)
                .columns(columns)
                .rows(rows)
                .rowCount(rows.size())
                .executionTimeMs(executionTimeMs)
                .build();
    }
}
