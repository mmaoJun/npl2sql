package com.nlp2sql.model;

import lombok.Data;
import java.util.List;

@Data
public class TableInfo {

    private String tableName;

    private String comment;

    private List<ColumnInfo> columns;
}
