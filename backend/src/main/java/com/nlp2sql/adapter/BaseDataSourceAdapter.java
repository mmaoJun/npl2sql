package com.nlp2sql.adapter;

import com.nlp2sql.model.ColumnInfo;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.model.dto.QueryResult;

import java.util.List;
import java.util.Map;

public abstract class BaseDataSourceAdapter {

    public abstract boolean connect(Map<String, Object> config);

    public abstract void disconnect();

    public abstract boolean testConnection(Map<String, Object> config);

    public abstract List<TableInfo> getTables();

    public abstract List<ColumnInfo> getColumns(String tableName);

    public abstract QueryResult executeQuery(String sql, int limit);

    public abstract String getDialect();
}
