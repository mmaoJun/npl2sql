package com.nlp2sql.common;

/**
 * 业务错误码常量定义。
 *
 * <p>按域分段编码：
 * <ul>
 *   <li>4xx — 通用 HTTP 语义错误</li>
 *   <li>1xxx — 数据源相关</li>
 *   <li>2xxx — NL2SQL 引擎相关</li>
 *   <li>9xxx — 系统级错误</li>
 * </ul>
 *
 * @see BusinessException
 */
public final class ErrorCode {

    private ErrorCode() {}

    // ── 通用错误 ──

    /** 请求参数错误 */
    public static final int BAD_REQUEST = 400;
    /** 未认证或 Token 无效 */
    public static final int UNAUTHORIZED = 401;
    /** 无权限 */
    public static final int FORBIDDEN = 403;
    /** 资源不存在 */
    public static final int NOT_FOUND = 404;

    // ── 数据源 1xxx ──

    /** 数据源不存在 */
    public static final int DATASOURCE_NOT_FOUND = 1001;
    /** 数据源连接失败 */
    public static final int DATASOURCE_CONNECTION_FAILED = 1002;
    /** 数据源名称重复 */
    public static final int DATASOURCE_DUPLICATE_NAME = 1003;
    /** 不支持的数据源类型 */
    public static final int DATASOURCE_TYPE_UNSUPPORTED = 1004;

    // ── NL2SQL 引擎 2xxx ──

    /** SQL 生成失败 */
    public static final int SQL_GENERATION_FAILED = 2001;
    /** SQL 安全校验失败 */
    public static final int SQL_VALIDATION_FAILED = 2002;
    /** SQL 执行失败 */
    public static final int SQL_EXECUTION_FAILED = 2003;
    /** 未找到 Schema */
    public static final int NO_SCHEMA_FOUND = 2004;

    // ── 系统 9xxx ──

    /** 系统内部错误 */
    public static final int SYSTEM_ERROR = 9999;
}
