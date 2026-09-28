-- NL2SQL 后端元数据库初始化脚本
-- 数据库: nlp2sql_meta

CREATE DATABASE IF NOT EXISTS nlp2sql_meta DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE nlp2sql_meta;

-- 数据源配置表
CREATE TABLE IF NOT EXISTS datasources (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    type VARCHAR(50) NOT NULL COMMENT '类型: mysql/postgresql/hive/hdfs/rest_api/graphql',
    host VARCHAR(200) COMMENT '主机地址',
    port INT COMMENT '端口',
    database_name VARCHAR(100) COMMENT '数据库名',
    username VARCHAR(100) COMMENT '用户名',
    password VARCHAR(200) COMMENT '密码(加密)',
    extra_config JSON COMMENT '额外配置(JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '数据源配置';

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(200) NOT NULL COMMENT 'BCrypt加密',
    role VARCHAR(20) DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '用户信息';

-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    datasource_id BIGINT,
    nl_input TEXT NOT NULL COMMENT '自然语言输入',
    generated_sql TEXT COMMENT '生成的SQL',
    validation_passed BOOLEAN COMMENT '校验是否通过',
    execution_success BOOLEAN COMMENT '执行是否成功',
    row_count INT COMMENT '结果行数',
    execution_time_ms BIGINT COMMENT '执行耗时(毫秒)',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) COMMENT '审计日志';
