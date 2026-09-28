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

    public record ValidationResult(boolean valid, String errorMessage) {
        public static ValidationResult ok() {
            return new ValidationResult(true, null);
        }
        public static ValidationResult fail(String message) {
            return new ValidationResult(false, message);
        }
    }
}
