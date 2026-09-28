package com.nlp2sql.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("audit_logs")
public class AuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long datasourceId;

    private String nlInput;

    private String generatedSql;

    private Boolean validationPassed;

    private Boolean executionSuccess;

    private Integer rowCount;

    private Long executionTimeMs;

    private String errorMessage;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
