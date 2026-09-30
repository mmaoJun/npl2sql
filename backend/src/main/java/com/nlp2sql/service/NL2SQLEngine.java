package com.nlp2sql.service;

import com.nlp2sql.adapter.DataSourceAdapter;
import com.nlp2sql.adapter.DataSourceFactory;
import com.nlp2sql.common.BusinessException;
import com.nlp2sql.common.ErrorCode;
import com.nlp2sql.config.ModelProperties;
import com.nlp2sql.model.DataSource;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.model.dto.ChatResponse;
import com.nlp2sql.model.dto.QueryResult;
import com.nlp2sql.security.SqlSecurityChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * NL2SQL 核心引擎服务。
 *
 * <p>编排完整的自然语言转 SQL 查询流水线：
 * Schema 获取 → Prompt 构建 → SQL 生成 → 安全校验（含重试）→ 执行查询 → 审计记录。
 * 所有外部请求均经过 {@link SqlSecurityChecker} 校验，防止危险 SQL 执行。
 *
 * @see PromptBuilder
 * @see SQLGenerator
 * @see SqlSecurityChecker
 * @see AuditService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NL2SQLEngine {

    private final PromptBuilder promptBuilder;
    private final SQLGenerator sqlGenerator;
    private final SqlSecurityChecker sqlSecurityChecker;
    private final MetadataService metadataService;
    private final DataSourceFactory dataSourceFactory;
    private final AuditService auditService;
    private final DataSourceService dataSourceService;
    private final AuthService authService;
    private final ModelProperties modelProperties;

    @Value("${sql-security.max-limit:100}")
    private int maxLimit;

    @Value("${sql-security.max-retry:2}")
    private int maxRetry;

    /**
     * 处理自然语言查询请求。
     *
     * <p>完整流水线：获取 Schema → 构建 Prompt → 生成 SQL → 安全校验（可重试）→ 强制 LIMIT → 执行查询。
     * 无论成功或失败，均在 finally 中写入审计日志。
     *
     * @param nlInput     用户输入的自然语言文本
     * @param datasourceId 目标数据源 ID
     * @param username    当前认证用户名，用于审计记录
     * @return 包含生成 SQL、查询结果和执行耗时的响应对象
     * @throws BusinessException Schema 为空、SQL 生成失败、校验不通过或执行异常时抛出
     */
    public ChatResponse processQuery(String nlInput, Long datasourceId, String username) {
        long startTime = System.currentTimeMillis();
        String generatedSql = null;
        boolean validationPassed = false;
        boolean executionSuccess = false;
        int rowCount = 0;
        String errorMessage = null;
        Long userId = authService.resolveUserId(username);

        try {
            DataSource dsConfig = dataSourceService.getById(datasourceId);
            List<TableInfo> tables = metadataService.getSchema(datasourceId);
            if (tables.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_SCHEMA_FOUND, "数据源中没有找到任何表");
            }

            String dialect = dataSourceFactory.getDialect(dsConfig.getType());

            Prompt prompt = promptBuilder.build(nlInput, tables, dialect);
            generatedSql = sqlGenerator.generate(prompt);

            if (generatedSql.isEmpty() || generatedSql.contains("无法理解")) {
                throw new BusinessException(ErrorCode.SQL_GENERATION_FAILED,
                        "模型无法理解该查询，请尝试换一种表述");
            }

            generatedSql = validateWithRetry(generatedSql, nlInput, tables, dialect);
            validationPassed = true;

            generatedSql = sqlSecurityChecker.enforceLimit(generatedSql, maxLimit);

            Map<String, Object> config = dsConfig.toAdapterConfig();
            DataSourceAdapter adapter = dataSourceFactory.getAdapter(datasourceId, dsConfig.getType(), config);
            QueryResult queryResult = adapter.executeQuery(generatedSql, maxLimit);

            if (!queryResult.isSuccess()) {
                throw new BusinessException(ErrorCode.SQL_EXECUTION_FAILED, queryResult.getErrorMessage());
            }

            executionSuccess = true;
            rowCount = queryResult.getRowCount();
            long executionTimeMs = System.currentTimeMillis() - startTime;

            return ChatResponse.success(nlInput, generatedSql,
                    queryResult.getColumns(), queryResult.getRows(), executionTimeMs,
                    modelProperties.getActiveModelName());

        } catch (BusinessException e) {
            errorMessage = e.getMessage();
            throw e;
        } catch (Exception e) {
            errorMessage = "系统异常: " + e.getMessage();
            log.error("查询处理失败: datasourceId={}, nlInput={}", datasourceId, nlInput, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "查询处理失败: " + e.getMessage());
        } finally {
            auditService.log(userId, datasourceId, nlInput, generatedSql,
                    validationPassed, executionSuccess, rowCount,
                    System.currentTimeMillis() - startTime, errorMessage);
        }
    }

    /**
     * 校验 SQL 安全性，不通过时携带错误反馈重新生成，最多重试 {@code maxRetry} 次。
     *
     * @param sql      初始生成的 SQL
     * @param nlInput  原始自然语言输入，用于重试 Prompt 构建
     * @param tables   当前 Schema 表结构列表
     * @param dialect  数据库方言（如 mysql、postgresql）
     * @return 通过安全校验的 SQL
     * @throws BusinessException 重试耗尽仍未通过校验时抛出
     */
    private String validateWithRetry(String sql, String nlInput, List<TableInfo> tables, String dialect) {
        SqlSecurityChecker.ValidationResult validation = sqlSecurityChecker.validate(sql);
        if (validation.valid()) {
            return sql;
        }

        for (int retry = 0; retry < maxRetry; retry++) {
            log.info("SQL 校验失败，第 {} 次重试: {}", retry + 1, validation.errorMessage());
            Prompt retryPrompt = promptBuilder.buildWithRetry(
                    nlInput, tables, dialect, sql, validation.errorMessage());
            sql = sqlGenerator.generate(retryPrompt);
            validation = sqlSecurityChecker.validate(sql);
            if (validation.valid()) {
                return sql;
            }
        }

        throw new BusinessException(ErrorCode.SQL_VALIDATION_FAILED,
                "SQL 校验失败（重试 " + maxRetry + " 次）: " + validation.errorMessage());
    }
}
