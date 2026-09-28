package com.nlp2sql.adapter;

import com.nlp2sql.model.ColumnInfo;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.model.dto.QueryResult;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

public class MySQLAdapter extends BaseDataSourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(MySQLAdapter.class);

    private HikariDataSource dataSource;

    @Override
    public boolean connect(Map<String, Object> config) {
        try {
            String host = (String) config.get("host");
            int port = (int) config.get("port");
            String database = (String) config.get("databaseName");
            String username = (String) config.get("username");
            String password = (String) config.get("password");

            String jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                    host, port, database);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setIdleTimeout(300000);

            this.dataSource = new HikariDataSource(hikariConfig);

            // 测试连接
            try (Connection conn = dataSource.getConnection()) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            log.error("MySQL 连接失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public boolean testConnection(Map<String, Object> config) {
        HikariDataSource testDs = null;
        try {
            String host = (String) config.get("host");
            int port = (int) config.get("port");
            String database = (String) config.get("databaseName");
            String username = (String) config.get("username");
            String password = (String) config.get("password");

            String jdbcUrl = String.format(
                    "jdbc:mysql://%s:%d/%s?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true",
                    host, port, database);

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(jdbcUrl);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setConnectionTimeout(5000);

            testDs = new HikariDataSource(hikariConfig);
            try (Connection conn = testDs.getConnection()) {
                return conn.isValid(5);
            }
        } catch (Exception e) {
            log.error("MySQL 连接测试失败: {}", e.getMessage());
            return false;
        } finally {
            if (testDs != null && !testDs.isClosed()) {
                testDs.close();
            }
        }
    }

    @Override
    public List<TableInfo> getTables() {
        List<TableInfo> tables = new ArrayList<>();
        String sql = """
                SELECT TABLE_NAME, TABLE_COMMENT
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
                ORDER BY TABLE_NAME
                """;

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                TableInfo table = new TableInfo();
                table.setTableName(rs.getString("TABLE_NAME"));
                table.setComment(rs.getString("TABLE_COMMENT"));
                table.setColumns(getColumns(table.getTableName()));
                tables.add(table);
            }
        } catch (SQLException e) {
            log.error("获取表列表失败: {}", e.getMessage(), e);
        }
        return tables;
    }

    @Override
    public List<ColumnInfo> getColumns(String tableName) {
        List<ColumnInfo> columns = new ArrayList<>();
        String sql = """
                SELECT COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, COLUMN_COMMENT,
                       COLUMN_KEY, IS_NULLABLE, COLUMN_DEFAULT
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ColumnInfo col = new ColumnInfo();
                    col.setColumnName(rs.getString("COLUMN_NAME"));
                    col.setDataType(rs.getString("DATA_TYPE"));
                    col.setColumnType(rs.getString("COLUMN_TYPE"));
                    col.setComment(rs.getString("COLUMN_COMMENT"));
                    col.setColumnKey(rs.getString("COLUMN_KEY"));
                    col.setIsNullable(rs.getString("IS_NULLABLE"));
                    col.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
                    columns.add(col);
                }
            }
        } catch (SQLException e) {
            log.error("获取表 {} 字段失败: {}", tableName, e.getMessage(), e);
        }
        return columns;
    }

    @Override
    public QueryResult executeQuery(String sql, int limit) {
        QueryResult result = new QueryResult();
        long startTime = System.currentTimeMillis();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(10);
            try (ResultSet rs = stmt.executeQuery(sql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnLabel(i));
                }
                result.setColumns(columns);

                List<Map<String, Object>> rows = new ArrayList<>();
                int count = 0;
                while (rs.next() && count < limit) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columns.get(i - 1), rs.getObject(i));
                    }
                    rows.add(row);
                    count++;
                }
                result.setRows(rows);
                result.setRowCount(rows.size());
                result.setSuccess(true);
            }
        } catch (SQLException e) {
            log.error("SQL 执行失败: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setErrorMessage("查询执行失败: " + e.getMessage());
        }

        result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
        return result;
    }

    @Override
    public String getDialect() {
        return "mysql";
    }
}
