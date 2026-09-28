package com.nlp2sql.service;

import com.nlp2sql.model.AuditLog;
import com.nlp2sql.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

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
