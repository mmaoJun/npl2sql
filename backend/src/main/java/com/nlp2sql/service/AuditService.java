package com.nlp2sql.service;

import com.nlp2sql.model.AuditLog;
import com.nlp2sql.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务。
 *
 * <p>记录每次 NL2SQL 查询的完整执行过程，包括输入、生成 SQL、校验结果、执行结果和耗时。
 * 由 {@link NL2SQLEngine} 在 finally 块中调用，确保无论成功或失败均写入审计记录。
 *
 * @see com.nlp2sql.model.AuditLog
 */
@Service
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    /**
     * 写入审计日志。
     *
     * @param userId            操作用户 ID，未认证时为 null
     * @param datasourceId      数据源 ID
     * @param nlInput           用户输入的自然语言
     * @param generatedSql      模型生成的 SQL
     * @param validationPassed  SQL 安全校验是否通过
     * @param executionSuccess  SQL 执行是否成功
     * @param rowCount          查询返回行数
     * @param executionTimeMs   总执行耗时（毫秒）
     * @param errorMessage      错误信息，成功时为 null
     */
    public void log(Long userId, Long datasourceId, String nlInput, String generatedSql,
                    boolean validationPassed, boolean executionSuccess,
                    int rowCount, long executionTimeMs, String errorMessage) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setDatasourceId(datasourceId);
        log.setNlInput(nlInput);
        log.setGeneratedSql(generatedSql);
        log.setValidationPassed(validationPassed);
        log.setExecutionSuccess(executionSuccess);
        log.setRowCount(rowCount);
        log.setExecutionTimeMs(executionTimeMs);
        log.setErrorMessage(errorMessage);
        auditLogMapper.insert(log);
    }
}
