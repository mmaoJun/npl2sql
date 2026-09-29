## 1. 采用的方案

后端基于 Spring Boot 的 `@RestControllerAdvice` + 自定义 `BusinessException` + 集中式 `ErrorCode` 常量，配合统一的 `ApiResponse<T>` 响应体，形成“业务异常上抛 → 全局处理器捕获 → 统一 JSON 返回”的链路。未使用 panic/recover（Java 无此概念），也未见 `try/catch` 在 Controller/Service 层主动吞异常，而是让异常冒泡到 `GlobalExceptionHandler`。

## 2. 关键文件与包

- `com.nlp2sql.common.BusinessException` — 业务异常基类，继承 `RuntimeException`，携带 `int code` 与可读消息。
- `com.nlp2sql.common.ErrorCode` — 不可实例化的常量类，按域分段定义错误码：4xx 通用、1xxx 数据源、2xxx NL2SQL、9xxx 系统。
- `com.nlp2sql.config.GlobalExceptionHandler` — `@RestControllerAdvice`，集中捕获 `BusinessException` / `MethodArgumentNotValidException` / `IllegalArgumentException` / `Exception`。
- `com.nlp2sql.model.dto.ApiResponse<T>` — 统一响应体 `{code, message, data}`，提供 `ok()` / `error(code,message)` 静态工厂。
- 业务侧抛出点集中在 service 层：`NL2SQLEngine.java`、`AuthService.java`、`DataSourceService.java`。

## 3. 架构与约定

### 3.1 异常分类与 HTTP 状态码映射

| 异常类型 | HTTP 状态码 | 行为 |
|---|---:|---|
| `BusinessException` | 200 (OK) | 记录 warn 日志，body.code 使用异常内嵌的业务码 |
| `MethodArgumentNotValidException` | 400 | 拼接所有字段校验失败信息为逗号分隔字符串 |
| `IllegalArgumentException` | 400 | 透传原始消息 |
| 其他 `Exception` | 500 | 记录 error 日志，固定返回 `SYSTEM_ERROR` (9999) |

注意：业务异常即使 code 是 4xx/5xx，HTTP 状态码仍固定为 200，由 body.code 区分成功/失败——这是该仓库的明确设计选择（见 `BusinessException` 注释：“返回 HTTP 200 + body code”）。

### 3.2 错误码分区规则

`ErrorCode` 以注释形式声明分区语义并实际落地：

- 400/401/403/404：通用 HTTP 语义
- 1001–1003：数据源域 (`DATASOURCE_NOT_FOUND`, `DATASOURCE_CONNECTION_FAILED`, `DATASOURCE_DUPLICATE_NAME`)
- 2001–2004：NL2SQL 引擎域 (`SQL_GENERATION_FAILED`, `SQL_VALIDATION_FAILED`, `SQL_EXECUTION_FAILED`, `NO_SCHEMA_FOUND`)
- 9999：兜底系统错误

### 3.3 调用链约定

- Service 层通过 `throw new BusinessException(ErrorCode.XXX, message)` 表达业务失败；Controller 层不 catch 业务异常，交由全局处理器处理。
- 参数校验失败走 Spring 的 `MethodArgumentNotValidException`，由全局处理器聚合字段错误。
- 安全认证失败（如 JWT 无效）由 Spring Security 默认机制处理，`JwtAuthenticationFilter` 本身不抛出业务异常，仅透传请求。

## 4. 观察到的约定与约束

- **业务异常必须通过 `BusinessException` 抛出**：现有代码中所有业务失败路径均使用 `throw new BusinessException(ErrorCode.*, ...)`，未在 Controller/Service 中使用 try/catch 包裹业务逻辑。
- **错误码集中维护于 `ErrorCode` 常量类**：禁止散落的魔法数字；新增错误码应添加到对应域段并遵循注释中的编号区间约定。
- **全局处理器是唯一的异常出口**：Controller 方法签名不包含 `throws`，异常统一由 `GlobalExceptionHandler` 转换为 `ApiResponse`。
- **业务异常的 HTTP 状态码固定为 200**：客户端需根据 `ApiResponse.code` 判断成功或失败，而非依赖 HTTP 状态码。
- **未处理异常兜底返回 SYSTEM_ERROR**：任何未被显式捕获的异常最终落入 `handleException(Exception)`，返回 code=9999 且消息固定为“服务器内部错误”，同时记录完整堆栈日志。
- **前端错误展示**：前端 `client.js` 直接读取 `response.data.code` 进行判断（非 HTTP status），与后端 200+body.code 的设计保持一致。

## 5. 缺失或不一致之处（供参考）

- `ErrorCode` 定义了 `DATASOURCE_CONNECTION_FAILED`，但未见 service 层使用该码的实际抛出处。
- `BusinessException` 提供了带 `Throwable cause` 的重载构造器，但当前所有 throw 处均未传入 cause，底层异常上下文丢失。
- 目前只有 `BusinessException` 被全局处理器显式处理，若未来引入新的自定义运行时异常，需要手动在 `GlobalExceptionHandler` 中添加 `@ExceptionHandler`，否则会被兜底的 `Exception` 处理器吞掉。
