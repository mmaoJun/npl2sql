package com.nlp2sql.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审计日志实体，映射 {@code audit_logs} 表。
 *
 * <p>记录每次 NL2SQL 查询的完整执行过程，由 {@link com.nlp2sql.service.AuditService} 写入。
 *
 * @see com.nlp2sql.service.NL2SQLEngine
 */
@Data
@TableName("audit_logs")
public class AuditLog {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作用户 ID，未认证时为 null */
    private Long userId;

    /** 数据源 ID */
    private Long datasourceId;

    /** 用户输入的自然语言 */
    private String nlInput;

    /** 模型生成的 SQL */
    private String generatedSql;

    /** SQL 安全校验是否通过 */
    private Boolean validationPassed;

    /** SQL 执行是否成功 */
    private Boolean executionSuccess;

    /** 查询返回行数 */
    private Integer rowCount;

    /** 总执行耗时（毫秒） */
    private Long executionTimeMs;

    /** 错误信息，成功时为 null */
    private String errorMessage;

    /** 创建时间，由 MyBatis-Plus 自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
