package com.nlp2sql.service;

import com.nlp2sql.adapter.BaseDataSourceAdapter;
import com.nlp2sql.adapter.DataSourceFactory;
import com.nlp2sql.model.DataSource;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.model.dto.ChatResponse;
import com.nlp2sql.model.dto.QueryResult;
import com.nlp2sql.security.SqlSecurityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NL2SQLEngine {

    private static final Logger log = LoggerFactory.getLogger(NL2SQLEngine.class);

    private final PromptBuilder promptBuilder;
    private final SQLGenerator sqlGenerator;
    private final SqlSecurityChecker sqlSecurityChecker;
    private final MetadataService metadataService;
    private final DataSourceFactory dataSourceFactory;
    private final AuditService auditService;

    @Value("${sql-security.max-limit:100}")
    private int maxLimit;

    @Value("${sql-security.max-retry:2}")
    private int maxRetry;

    public NL2SQLEngine(PromptBuilder promptBuilder,
                         SQLGenerator sqlGenerator,
                         SqlSecurityChecker sqlSecurityChecker,
                         MetadataService metadataService,
                         DataSourceFactory dataSourceFactory,
                         AuditService auditService) {
        this.promptBuilder = promptBuilder;
        this.sqlGenerator = sqlGenerator;
        this.sqlSecurityChecker = sqlSecurityChecker;
        this.metadataService = metadataService;
        this.dataSourceFactory = dataSourceFactory;
        this.auditService = auditService;
    }

    public ChatResponse processQuery(String nlInput, Long datasourceId, DataSource dsConfig, Long userId) {
        long startTime = System.currentTimeMillis();
        String generatedSql = null;
        boolean validationPassed = false;
        boolean executionSuccess = false;
        int rowCount = 0;
        String errorMessage = null;

        try {
            // 1. 获取 Schema
            List<TableInfo> tables = metadataService.getSchema(datasourceId, dsConfig);
            if (tables.isEmpty()) {
                errorMessage = "数据源中没有找到任何表";
                return ChatResponse.error(nlInput, errorMessage);
            }

            String dialect = dataSourceFactory.createAdapter(dsConfig.getType()).getDialect();

            // 2. 构建 Prompt 并生成 SQL
            String prompt = promptBuilder.build(nlInput, tables, dialect);
            generatedSql = sqlGenerator.generate(prompt);

            if (generatedSql.isEmpty() || generatedSql.contains("无法理解")) {
                errorMessage = "模型无法理解该查询，请尝试换一种表述";
                return ChatResponse.error(nlInput, errorMessage);
            }

            // 3. 校验 SQL（支持重试）
            SqlSecurityChecker.ValidationResult validation = sqlSecurityChecker.validate(generatedSql);
            validationPassed = validation.valid();

            if (!validationPassed) {
                // 自纠错重试
                for (int retry = 0; retry < maxRetry; retry++) {
                    log.info("SQL 校验失败，第 {} 次重试: {}", retry + 1, validation.errorMessage());
                    String retryPrompt = promptBuilder.buildWithRetry(
                            nlInput, tables, dialect, generatedSql, validation.errorMessage());
                    generatedSql = sqlGenerator.generate(retryPrompt);
                    validation = sqlSecurityChecker.validate(generatedSql);
                    validationPassed = validation.valid();
                    if (validationPassed) break;
                }

                if (!validationPassed) {
                    errorMessage = "SQL 校验失败: " + validation.errorMessage();
                    return ChatResponse.error(nlInput, errorMessage);
                }
            }

            // 4. 强制 LIMIT
            generatedSql = sqlSecurityChecker.enforceLimit(generatedSql, maxLimit);

            // 5. 执行查询
            Map<String, Object> config = buildConfig(dsConfig);
            BaseDataSourceAdapter adapter = dataSourceFactory.getAdapter(datasourceId, dsConfig.getType(), config);
            QueryResult queryResult = adapter.executeQuery(generatedSql, maxLimit);

            if (!queryResult.isSuccess()) {
                errorMessage = queryResult.getErrorMessage();
                executionSuccess = false;
                return ChatResponse.error(nlInput, errorMessage);
            }

            executionSuccess = true;
            rowCount = queryResult.getRowCount();
            long executionTimeMs = System.currentTimeMillis() - startTime;

            return ChatResponse.success(nlInput, generatedSql,
                    queryResult.getColumns(), queryResult.getRows(), executionTimeMs);

        } catch (Exception e) {
            log.error("查询处理异常: {}", e.getMessage(), e);
            errorMessage = "系统异常: " + e.getMessage();
            return ChatResponse.error(nlInput, errorMessage);
        } finally {
            // 6. 记录审计日志
            auditService.log(userId, datasourceId, nlInput, generatedSql,
                    validationPassed, executionSuccess, rowCount,
                    System.currentTimeMillis() - startTime, errorMessage);
        }
    }

    private Map<String, Object> buildConfig(DataSource ds) {
        Map<String, Object> config = new HashMap<>();
        config.put("host", ds.getHost());
        config.put("port", ds.getPort());
        config.put("databaseName", ds.getDatabaseName());
        config.put("username", ds.getUsername());
        config.put("password", ds.getPassword());
        return config;
    }
}
