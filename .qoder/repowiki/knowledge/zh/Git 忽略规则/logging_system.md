## 1. 使用的框架与工具

后端采用 **SLF4J + Logback**（Spring Boot 默认绑定）作为日志门面，Logger 实例由 Lombok `@Slf4j` 注解生成；SLF4J 本身未显式声明依赖，由 `spring-boot-starter-web` 传递引入。MyBatis-Plus 的 SQL 输出通过 `log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl` 桥接到 SLF4J。前端使用浏览器原生 `console.log`，Python 测试脚本使用 `print`，无专用日志库。

## 2. 关键文件

- `backend/src/main/resources/application.yml`：全局日志级别与 console 输出格式配置
- `backend/src/main/java/com/nlp2sql/config/GlobalExceptionHandler.java`：全局异常处理中的 `warn` / `error` 调用点
- `backend/src/main/java/com/nlp2sql/service/AuditService.java`：将 NL2SQL 执行过程持久化为 `AuditLog` 记录
- `backend/src/main/java/com/nlp2sql/service/NL2SQLEngine.java`：核心业务链路中记录错误与重试信息
- `backend/src/main/java/com/nlp2sql/security/SqlSecurityChecker.java`、`DataSourceService.java`、`MetadataService.java` 等：各模块以相同方式引入 SLF4J

## 3. 架构与约定

### 3.1 Logger 初始化模式
所有需要日志的 Java 类统一在类上标注 Lombok `@Slf4j`，由注解处理器生成 `private static final org.slf4j.Logger log` 字段，类内直接以 `log.info(...)` 使用：
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class NL2SQLEngine { ... }
```
不再手写 `LoggerFactory.getLogger(XXX.class)`，也不 import `org.slf4j.Logger`。未见自定义 Logger 工厂或 AOP 切面。

注意继承场景：`@Slf4j` 生成的字段是 `private` 的，子类看不到父类的 `log`。因此 `JdbcDataSourceAdapter` 与其子类 `MySQLAdapter` 各自标注 `@Slf4j`（改造前基类用的是 `protected static final Logger`，子类日志会以基类名输出）。

### 3.2 日志级别使用
- `info`：正常流程节点（如 `NL2SQLEngine` 中“SQL 校验失败，第 X 次重试”）
- `warn`：可预期的业务异常（如 `GlobalExceptionHandler` 对 `BusinessException` 的处理）
- `error`：未处理异常及查询处理失败（附带异常堆栈）
- 未见 `debug` / `trace` 的业务调用，仅 MyBatis-Plus 通过 `Slf4jImpl` 输出 SQL

### 3.3 结构化字段
日志消息采用 SLF4J 参数化占位符（`"... code={}, message={}"`），而非字符串拼接。但日志本身是纯文本行，没有统一的 JSON 结构化输出格式。

### 3.4 TraceId 关联
console 输出 pattern 中包含 `%X{traceId}`，说明期望通过 MDC 注入 traceId；项目中有 `TraceIdFilter`（位于 `com.nlp2sql.common`）负责设置该上下文值，使日志行能串联同一请求。

### 3.5 审计日志与运行日志分离
`AuditService.log(...)` 不是 SLF4J 输出，而是把用户 ID、数据源 ID、NL 输入、生成 SQL、校验结果、执行成功标志、行数、耗时、错误信息等写入 `AuditLog` 表，属于**业务审计记录**，与面向运维的 SLF4J 日志并行存在。

### 3.6 日志输出位置
`application.yml` 中只定义了 `pattern.console`，未定义 file appender，因此日志默认输出到控制台。未看到 rolling-file、ES/Kafka sink 等集中式收集配置。

## 4. 约定与约束

- **Logger 获取方式**：所有使用日志的类均在类上标注 `@Slf4j`，不手写 logger 字段、不 import `org.slf4j.Logger` / `LoggerFactory`（`TraceIdFilter` 只用 `org.slf4j.MDC`，不受此约束）。
- **日志级别规范**：业务异常用 `warn`，不可恢复错误用 `error`，常规流程节点用 `info`（依据现有调用分布观察到的模式）。
- **TraceId 关联**：日志 pattern 强制要求 `%X{traceId}` 存在，依赖 `TraceIdFilter` 在 MDC 中填充该字段，否则日志行中 traceId 为空。
- **MyBatis-Plus SQL 日志**：通过 `configuration.log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl` 接入 SLF4J，可在 `application.yml` 中按包调整 SQL 输出级别。
- **审计记录**：所有 NL2SQL 执行必须经 `AuditService.log` 持久化，包含 nlInput、generatedSql、validationPassed、executionSuccess、rowCount、executionTimeMs、errorMessage 等固定字段（由 `AuditLog` 实体和 `AuditService` 接口定义保证）。
- **无结构化日志 schema**：仓库中没有日志格式 schema、JSON 模板或日志采集器配置，日志均为可读文本行。
- **生产安全**：`GlobalExceptionHandler` 仅在 `error` 分支打印异常堆栈，其余异常返回标准 `ApiResponse`，避免敏感信息直接暴露给前端。