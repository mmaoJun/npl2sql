package com.nlp2sql.model;

import lombok.Data;

@Data
public class ColumnInfo {

    private String columnName;

    private String dataType;

    private String columnType;

    private String comment;

    private String columnKey;

    private String isNullable;

    private String defaultValue;
}
