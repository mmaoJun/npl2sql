package com.nlp2sql.model.dto;

import lombok.Data;

@Data
public class QueryResult {

    private java.util.List<String> columns;

    private java.util.List<java.util.Map<String, Object>> rows;

    private int rowCount;

    private long executionTimeMs;

    private boolean success;

    private String errorMessage;
}
