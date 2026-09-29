package com.nlp2sql.security;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 安全校验器。
 *
 * <p>三层防御机制：
 * <ul>
 *   <li>L1 — 关键词黑名单：拒绝 INSERT/UPDATE/DELETE/DROP 等危险操作</li>
 *   <li>L2 — AST 分析：使用 JSqlParser 验证语句必须是 SELECT</li>
 *   <li>L3 — 注入模式检测：拦截堆叠查询和 UNION 注入</li>
 * </ul>
 * 另提供 {@link #enforceLimit} 方法强制添加 LIMIT 子句。
 *
 * @see com.nlp2sql.service.NL2SQLEngine
 */
@Component
public class SqlSecurityChecker {

    private static final Logger log = LoggerFactory.getLogger(SqlSecurityChecker.class);

    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE",
            "TRUNCATE", "REPLACE", "MERGE", "GRANT", "REVOKE",
            "EXEC", "EXECUTE", "CALL", "SHUTDOWN", "LOAD_FILE"
    );

    private static final Pattern STACKED_QUERIES = Pattern.compile(";\\s*(?!\\s*$)");
    private static final Pattern COMMENT_INJECTION = Pattern.compile("(/\\*|--|#)");
    private static final Pattern UNION_INJECTION = Pattern.compile("\\bUNION\\b\\s+(ALL\\s+)?\\bSELECT\\b", Pattern.CASE_INSENSITIVE);

    /**
     * 校验 SQL 安全性。
     *
     * <p>依次执行关键词黑名单、AST 类型验证和注入模式检测，任一失败即返回失败结果。
     *
     * @param sql 待校验的 SQL 语句
     * @return 校验结果，包含是否通过和错误消息
     */
    public ValidationResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return ValidationResult.fail("SQL 不能为空");
        }

        String trimmed = sql.trim();

        // L1: 关键词黑名单
        String upperSql = trimmed.toUpperCase();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (upperSql.contains(keyword)) {
                log.warn("SQL 包含禁止关键词: {}", keyword);
                return ValidationResult.fail("包含禁止的SQL关键字: " + keyword);
            }
        }

        // L2: AST 分析 - 验证是 SELECT
        try {
            Statement stmt = CCJSqlParserUtil.parse(trimmed);
            if (!(stmt instanceof Select)) {
                return ValidationResult.fail("只允许 SELECT 查询语句");
            }
        } catch (JSQLParserException e) {
            log.warn("SQL 解析失败: {}", e.getMessage());
            return ValidationResult.fail("SQL 语法错误: " + e.getMessage());
        }

        // L3: 注入模式检测
        if (STACKED_QUERIES.matcher(trimmed).find()) {
            return ValidationResult.fail("禁止堆叠查询");
        }
        if (UNION_INJECTION.matcher(trimmed).find()) {
            log.warn("检测到 UNION 注入模式");
            return ValidationResult.fail("检测到可疑的 UNION 查询");
        }

        return ValidationResult.ok();
    }

    /**
     * 强制为 SQL 添加 LIMIT 子句，防止全表扫描。
     *
     * <p>去除末尾分号后，若 SQL 中无 LIMIT 关键字则追加 {@code LIMIT maxLimit}。
     *
     * @param sql      待处理的 SQL
     * @param maxLimit 最大返回行数
     * @return 处理后的 SQL
     */
    public String enforceLimit(String sql, int maxLimit) {
        String cleaned = sql.trim();
        if (cleaned.endsWith(";")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        String upperSql = cleaned.toUpperCase();
        if (!upperSql.contains("LIMIT")) {
            return cleaned + " LIMIT " + maxLimit;
        }
        return cleaned;
    }

    /**
     * SQL 校验结果。
     *
     * @param valid        是否通过校验
     * @param errorMessage 校验失败时的错误消息，通过时为 null
     */
    public record ValidationResult(boolean valid, String errorMessage) {

        /** 校验通过。 */
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }
        /** 校验失败。 */
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }
}
