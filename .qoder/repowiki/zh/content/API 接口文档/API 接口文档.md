# API 接口文档

<cite>
**本文引用的文件**   
- [认证控制器](file://backend/src/main/java/com/nlp2sql/controller/AuthController.java)
- [对话控制器](file://backend/src/main/java/com/nlp2sql/controller/ChatController.java)
- [数据源控制器](file://backend/src/main/java/com/nlp2sql/controller/DataSourceController.java)
- [元数据控制器](file://backend/src/main/java/com/nlp2sql/controller/MetadataController.java)
- [统一响应体](file://backend/src/main/java/com/nlp2sql/model/dto/ApiResponse.java)
- [对话请求体](file://backend/src/main/java/com/nlp2sql/model/dto/ChatRequest.java)
- [对话响应体](file://backend/src/main/java/com/nlp2sql/model/dto/ChatResponse.java)
- [登录请求体](file://backend/src/main/java/com/nlp2sql/model/dto/LoginRequest.java)
- [登录响应体](file://backend/src/main/java/com/nlp2sql/model/dto/LoginResponse.java)
- [数据源请求体](file://backend/src/main/java/com/nlp2sql/model/dto/DataSourceDTO.java)
- [查询结果模型](file://backend/src/main/java/com/nlp2sql/model/dto/QueryResult.java)
- [表结构模型](file://backend/src/main/java/com/nlp2sql/model/TableInfo.java)
- [列结构模型](file://backend/src/main/java/com/nlp2sql/model/ColumnInfo.java)
- [业务异常](file://backend/src/main/java/com/nlp2sql/common/BusinessException.java)
- [错误码常量](file://backend/src/main/java/com/nlp2sql/common/ErrorCode.java)
- [全局异常处理器](file://backend/src/main/java/com/nlp2sql/config/GlobalExceptionHandler.java)
- [JWT 认证过滤器](file://backend/src/main/java/com/nlp2sql/security/JwtAuthenticationFilter.java)
- [JWT Token 提供者](file://backend/src/main/java/com/nlp2sql/security/JwtTokenProvider.java)
- [安全配置](file://backend/src/main/java/com/nlp2sql/config/SecurityConfig.java)
- [前端端点调用封装](file://frontend/src/api/endpoints.js)
</cite>

## 目录

1. [简介](#简介)
2. [API 总览与基础约定](#api-总览与基础约定)
3. [认证与安全机制](#认证与安全机制)
4. [统一响应格式与错误处理](#统一响应格式与错误处理)
5. [认证接口](#认证接口)
6. [NL2SQL 对话接口](#nl2sql-对话接口)
7. [数据源管理接口](#数据源管理接口)
8. [元数据浏览接口](#元数据浏览接口)
9. [客户端集成示例与最佳实践](#客户端集成示例与最佳实践)
10. [版本管理与向后兼容性](#版本管理与向后兼容性)
11. [故障排查指南](#故障排查指南)
12. [结论](#结论)

## 简介

本项目提供基于自然语言到 SQL 的 NL2SQL 能力，并通过一组 RESTful API 暴露给前端或其他客户端。后端使用 Spring Boot 构建，采用 JWT 进行无状态身份认证，使用统一的 `ApiResponse` 包装所有接口返回体，并通过全局异常处理器把业务异常、参数校验异常和系统异常收敛为一致的 JSON 响应。

从架构上看，REST 层由四个控制器组成：

| 控制器 | 职责 | URL 前缀 |
|---|---|---|
| 认证控制器 | 用户注册与登录，签发或校验 JWT | `/api/v1/auth` |
| NL2SQL 对话控制器 | 接收自然语言消息并生成 SQL、执行查询 | `/api/v1/chat` |
| 数据源管理控制器 | 数据源的增删改查及连接测试 | `/api/v1/datasources` |
| 元数据浏览控制器 | 查看数据库表结构与字段信息，刷新元数据缓存 | `/api/v1/metadata` |

```mermaid
graph TB
    Client["客户端"] --> Auth["认证控制器<br/>/api/v1/auth"]
    Client --> Chat["NL2SQL 对话控制器<br/>/api/v1/chat"]
    Client --> Datasource["数据源管理控制器<br/>/api/v1/datasources"]
    Client --> Metadata["元数据浏览控制器<br/>/api/v1/metadata"]
    Auth --> Security["安全配置与 JWT 过滤器"]
    Chat --> Security
    Datasource --> Security
    Metadata --> Security
    Chat --> Engine["NL2SQL 引擎与服务层"]
    Datasource --> DataSourceService["数据源服务"]
    Metadata --> MetadataService["元数据服务"]
```

**图表来源**   
- [认证控制器:1-31](file://backend/src/main/java/com/nlp2sql/controller/AuthController.java#L1-L31)
- [对话控制器:1-30](file://backend/src/main/java/com/nlp2sql/controller/ChatController.java#L1-L30)
- [数据源控制器:1-50](file://backend/src/main/java/com/nlp2sql/controller/DataSourceController.java#L1-L50)
- [元数据控制器:1-38](file://backend/src/main/java/com/nlp2sql/controller/MetadataController.java#L1-L38)
- [安全配置:1-44](file://backend/src/main/java/com/nlp2sql/config/SecurityConfig.java#L1-L44)

**章节来源**   
- [认证控制器:1-31](file://backend/src/main/java/com/nlp2sql/controller/AuthController.java#L1-L31)
- [对话控制器:1-30](file://backend/src/main/java/com/nlp2sql/controller/ChatController.java#L1-L30)
- [数据源控制器:1-50](file://backend/src/main/java/com/nlp2sql/controller/DataSourceController.java#L1-L50)
- [元数据控制器:1-38](file://backend/src/main/java/com/nlp2sql/controller/MetadataController.java#L1-L38)
- [安全配置:1-44](file://backend/src/main/java/com/nlp2sql/config/SecurityConfig.java#L1-L44)

## API 总览与基础约定

### 基础路径

所有业务接口均位于 `/api/v1` 下。当前代码中只存在 `v1` 版本，因此“版本”体现在 URL 前缀中，而不是通过 Header 或查询参数控制。

| 模块 | 基础路径 | 说明 |
|---|---|---|
| 认证 | `/api/v1/auth` | 注册、登录 |
| 对话 | `/api/v1/chat` | 自然语言转 SQL 并执行 |
| 数据源 | `/api/v1/datasources` | 数据源 CRUD 与连接测试 |
| 元数据 | `/api/v1/metadata` | 表、字段查询与缓存刷新 |

### 通用请求头

| 请求头 | 是否必需 | 说明 |
|---|---:|---|
| `Content-Type` | 是 | 通常为 `application/json` |
| `Authorization` | 按接口而定 | 需要认证的接口必须携带 `Bearer <JWT>` |

### 通用响应体

所有接口都返回统一的 `ApiResponse<T>` 结构：

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | `int` | 业务状态码；成功时一般为 `200`，失败时使用业务错误码或 HTTP 语义对应的业务码 |
| `message` | `String` | 可读的人类可读消息 |
| `data` | `T` | 具体业务数据；可为空对象、空数组或 `null` |

成功响应的典型形态为：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

失败响应的典型形态为：

```json
{
  "code": 1001,
  "message": "数据源不存在",
  "data": null
}
```

**章节来源**   
- [统一响应体:1-33](file://backend/src/main/java/com/nlp2sql/model/dto/ApiResponse.java#L1-L33)

## 认证与安全机制

### 安全策略

项目使用 Spring Security 配合自定义 JWT 过滤器实现无状态认证：

1. 用户通过 `/api/v1/auth/register` 或 `/api/v1/auth/login` 完成注册或登录。
2. 登录成功后服务端返回包含 `token`、`username`、`role` 的 `LoginResponse`。
3. 客户端在后续请求中把 `token` 放入 `Authorization` 请求头，格式为 `Bearer <token>`。
4. `JwtAuthenticationFilter` 解析该请求头，调用 `JwtTokenProvider` 校验并提取用户名。
5. 校验通过后，Spring Security 上下文中的 `Authentication` 不为空，控制器可读取当前用户名。
6. 未携带有效 Token 的请求会被拒绝。

```mermaid
sequenceDiagram
    participant Client as "客户端"
    participant AuthController as "认证控制器"
    participant AuthService as "认证服务"
    participant JwtProvider as "JWT Token 提供者"
    participant Filter as "JWT 认证过滤器"
    participant ProtectedApi as "受保护接口"

    Client->>AuthController: POST /api/v1/auth/login
    AuthController->>AuthService: 验证用户名和密码
    AuthService-->>AuthController: LoginResponse
    AuthController-->>Client: ApiResponse<LoginResponse>

    Client->>ProtectedApi: GET /api/v1/metadata/tables?datasourceId=1
    Note over ProtectedApi: 请求头 Authorization: Bearer <token>
    ProtectedApi->>Filter: 进入安全过滤链
    Filter->>JwtProvider: 校验并解析 Token
    JwtProvider-->>Filter: 用户名
    Filter-->>ProtectedApi: 设置 SecurityContext
    ProtectedApi-->>Client: ApiResponse<TableInfo[]>
```

**图表来源**   
- [认证控制器:1-31](file://backend/src/main/java/com/nlp2sql/controller/AuthController.java#L1-L31)
- [JWT 认证过滤器:1-51](file://backend/src/main/java/com/nlp2sql/security/JwtAuthenticationFilter.java#L1-L51)
- [JWT Token 提供者:1-55](file://backend/src/main/java/com/nlp2sql/security/JwtTokenProvider.java#L1-L55)
- [安全配置:1-44](file://backend/src/main/java/com/nlp2sql/config/SecurityConfig.java#L1-L44)

### 白名单与权限范围

| 路径模式 | 访问控制 | 说明 |
|---|---|---|
| `/api/v1/auth/**` | 允许匿名访问 | 注册和登录不需要 Token |
| `/api-docs/**` | 允许匿名访问 | 接口文档 |
| `/swagger-ui/**` | 允许匿名访问 | Swagger UI |
| `/swagger-ui.html` | 允许匿名访问 | Swagger HTML |
| 其他所有请求 | 需要认证 | 必须携带有效的 `Authorization: Bearer <token>` |

当前过滤器对成功认证的用户赋予固定角色 `ROLE_USER`，没有更细粒度的角色或资源权限控制。

### JWT Token 生命周期

| 配置项 | 作用 |
|---|---|
| `jwt.secret` | 用于签名和校验 JWT 的密钥 |
| `jwt.expiration` | Token 过期时间，单位为毫秒 |

`JwtTokenProvider` 会构造包含 `subject`、`role`、签发时间和过期时间的 JWT，并使用 HMAC 密钥签名。客户端需要自行存储并复用该 Token，直到其失效。

**章节来源**   
- [安全配置:1-44](file://backend/src/main/java/com/nlp2sql/config/SecurityConfig.java#L1-L44)
- [JWT 认证过滤器:1-51](file://backend/src/main/java/com/nlp2sql/security/JwtAuthenticationFilter.java#L1-L51)
- [JWT Token 提供者:1-55](file://backend/src/main/java/com/nlp2sql/security/JwtTokenProvider.java#L1-L55)

## 统一响应格式与错误处理

### 业务响应体

所有控制器方法都返回 `ApiResponse<T>`：

| 方法 | 行为 |
|---|---|
| `ok(data)` | 返回 `code=200`、`message="success"` 并携带业务数据 |
| `ok()` | 返回 `code=200`、`message="success"` 且 `data=null` |
| `error(code, message)` | 返回指定业务码和消息，`data=null` |
| `error(message)` | 返回 `code=500`、指定消息，`data=null` |

### 全局异常处理

`GlobalExceptionHandler` 将以下几类异常转换为 `ApiResponse`：

| 异常类型 | HTTP 状态码 | 业务逻辑 |
|---|---:|---|
| `BusinessException` | `200` | 直接返回异常中的 `code` 和 `message`，便于上层按业务码判断 |
| `MethodArgumentNotValidException` | `400` | 合并所有字段校验错误消息 |
| `IllegalArgumentException` | `400` | 使用异常消息作为业务错误消息 |
| 其他未捕获异常 | `500` | 返回 `SYSTEM_ERROR` 和“服务器内部错误” |

注意：业务异常虽然返回 HTTP `200`，但响应体中的 `code` 并非 `200`。客户端应优先根据 `code` 判断业务成功与否，而不是仅依赖 HTTP 状态码。

### 错误码定义

| 错误码 | 含义 | 常见触发场景 |
|---:|---|---|
| `400` | 请求参数错误 | 必填字段缺失、类型不匹配 |
| `401` | 未授权 | 未登录或 Token 无效（通常由安全框架拦截） |
| `403` | 禁止访问 | 当前用户无权访问资源 |
| `404` | 资源不存在 | 找不到对应实体 |
| `1001` | 数据源不存在 | 数据源 ID 无效 |
| `1002` | 数据源连接失败 | 数据库连接测试失败 |
| `1003` | 数据源名称重复 | 创建或更新数据源时名称冲突 |
| `2001` | SQL 生成失败 | NL2SQL 引擎无法生成 SQL |
| `2002` | SQL 校验失败 | 安全校验或语法校验失败 |
| `2003` | SQL 执行失败 | 实际数据库执行出错 |
| `2004` | 未找到可用 Schema | 无法获取数据库元数据 |
| `9999` | 系统错误 | 未预期异常 |

**章节来源**   
- [统一响应体:1-33](file://backend/src/main/java/com/nlp2sql/model/dto/ApiResponse.java#L1-L33)
- [业务异常:1-23](file://backend/src/main/java/com/nlp2sql/common/BusinessException.java#L1-L23)
- [错误码常量:1-26](file://backend/src/main/java/com/nlp2sql/common/ErrorCode.java#L1-L26)
- [全局异常处理器:1-50](file://backend/src/main/java/com/nlp2sql/config/GlobalExceptionHandler.java#L1-L50)

## 认证接口

### 用户注册

| 项目 | 值 |
|---|---|
| 方法 | `POST` |
| URL | `/api/v1/auth/register` |
| 认证 | 不需要 |
| Content-Type | `application/json` |
| 请求体 | `LoginRequest` |
| 响应体 | `ApiResponse<String>` |

#### 请求体字段

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---:|---|
| `username` | `String` | 是 | 用户名，不能为空 |
| `password` | `String` | 是 | 密码，不能为空 |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "注册成功",
  "data": "注册成功"
}
```

#### 失败响应示例

```json
{
  "code": 400,
  "message": "用户名不能为空, 密码不能为空",
  "data": null
}
```

### 用户登录

| 项目 | 值 |
|---|---|
| 方法 | `POST` |
| URL | `/api/v1/auth/login` |
| 认证 | 不需要 |
| Content-Type | `application/json` |
| 请求体 | `LoginRequest` |
| 响应体 | `ApiResponse<LoginResponse>` |

#### 请求体字段

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---:|---|
| `username` | `String` | 是 | 用户名 |
| `password` | `String` | 是 | 密码 |

#### 登录响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `token` | `String` | JWT Token，后续请求需放入 `Authorization` 头 |
| `username` | `String` | 用户名 |
| `role` | `String` | 用户角色 |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin",
    "role": "ROLE_USER"
  }
}
```

#### 失败响应示例

```json
{
  "code": 400,
  "message": "用户名不能为空, 密码不能为空",
  "data": null
}
```

**章节来源**   
- [认证控制器:1-31](file://backend/src/main/java/com/nlp2sql/controller/AuthController.java#L1-L31)
- [登录请求体:1-14](file://backend/src/main/java/com/nlp2sql/model/dto/LoginRequest.java#L1-L14)
- [登录响应体:1-15](file://backend/src/main/java/com/nlp2sql/model/dto/LoginResponse.java#L1-L15)
- [全局异常处理器:1-50](file://backend/src/main/java/com/nlp2sql/config/GlobalExceptionHandler.java#L1-L50)

## NL2SQL 对话接口

该接口接收自然语言问题，将其交给 NL2SQL 引擎，最终返回生成的 SQL、查询结果和执行信息。

### 发送自然语言查询

| 项目 | 值 |
|---|---|
| 方法 | `POST` |
| URL | `/api/v1/chat` |
| 认证 | 需要 |
| Content-Type | `application/json` |
| 请求体 | `ChatRequest` |
| 响应体 | `ApiResponse<ChatResponse>` |

#### 请求体字段

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---:|---|
| `message` | `String` | 是 | 自然语言查询语句 |
| `datasourceId` | `Long` | 是 | 目标数据源 ID |

#### 响应体字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `success` | `boolean` | 本次查询是否成功 |
| `nlInput` | `String` | 原始自然语言输入 |
| `generatedSql` | `String` | 生成的 SQL 语句 |
| `columns` | `List<String>` | 查询结果的列名列表 |
| `rows` | `List<Map<String, Object>>` | 查询结果行集合 |
| `rowCount` | `int` | 行数 |
| `executionTimeMs` | `long` | 执行耗时，单位毫秒 |
| `modelName` | `String` | 使用的模型名称 |
| `errorMessage` | `String` | 失败时的错误信息 |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "nlInput": "查询用户表中所有用户",
    "generatedSql": "SELECT * FROM user",
    "columns": ["id", "name"],
    "rows": [
      {"id": 1, "name": "张三"}
    ],
    "rowCount": 1,
    "executionTimeMs": 120,
    "modelName": "ollama-model"
  }
}
```

#### 失败响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": false,
    "nlInput": "查询用户表中所有用户",
    "generatedSql": null,
    "columns": [],
    "rows": [],
    "rowCount": 0,
    "executionTimeMs": 0,
    "modelName": null,
    "errorMessage": "未找到可用 Schema"
  }
}
```

#### 认证失败响应示例

如果未携带有效 Token，请求会被安全框架拦截，不再进入控制器。

```json
{
  "code": 401,
  "message": "未认证",
  "data": null
}
```

#### 请求参数校验失败示例

```json
{
  "code": 400,
  "message": "消息内容不能为空, 数据源ID不能为空",
  "data": null
}
```

**章节来源**   
- [对话控制器:1-30](file://backend/src/main/java/com/nlp2sql/controller/ChatController.java#L1-L30)
- [对话请求体:1-15](file://backend/src/main/java/com/nlp2sql/model/dto/ChatRequest.java#L1-L15)
- [对话响应体:1-59](file://backend/src/main/java/com/nlp2sql/model/dto/ChatResponse.java#L1-L59)
- [查询结果模型:1-19](file://backend/src/main/java/com/nlp2sql/model/dto/QueryResult.java#L1-L19)
- [全局异常处理器:1-50](file://backend/src/main/java/com/nlp2sql/config/GlobalExceptionHandler.java#L1-L50)

## 数据源管理接口

数据源接口用于管理后端连接的数据库实例，包括创建、查询、更新、删除和连接测试。

### 创建数据源

| 项目 | 值 |
|---|---|
| 方法 | `POST` |
| URL | `/api/v1/datasources` |
| 认证 | 需要 |
| Content-Type | `application/json` |
| 请求体 | `DataSourceDTO` |
| 响应体 | `ApiResponse<DataSourceVO>` |

#### 请求体字段

| 字段 | 类型 | 是否必填 | 说明 |
|---|---|---:|---|
| `id` | `Long` | 否 | 更新时使用；创建时可忽略 |
| `name` | `String` | 是 | 数据源名称 |
| `type` | `String` | 是 | 数据库类型 |
| `host` | `String` | 是 | 主机地址 |
| `port` | `Integer` | 是 | 端口号 |
| `databaseName` | `String` | 是 | 数据库名 |
| `username` | `String` | 是 | 数据库用户名 |
| `password` | `String` | 是 | 数据库密码 |
| `extraConfig` | `String` | 否 | 扩展配置 |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "name": "test-mysql",
    "type": "mysql",
    "host": "localhost",
    "port": 3306,
    "databaseName": "sample_db"
  }
}
```

### 查询数据源列表

| 项目 | 值 |
|---|---|
| 方法 | `GET` |
| URL | `/api/v1/datasources` |
| 认证 | 需要 |
| 请求体 | 无 |
| 响应体 | `ApiResponse<List<DataSourceVO>>` |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "name": "test-mysql",
      "type": "mysql",
      "host": "localhost",
      "port": 3306,
      "databaseName": "sample_db"
    }
  ]
}
```

### 更新数据源

| 项目 | 值 |
|---|---|
| 方法 | `PUT` |
| URL | `/api/v1/datasources/{id}` |
| 认证 | 需要 |
| Content-Type | `application/json` |
| 路径参数 | `id`: `Long` |
| 请求体 | `DataSourceDTO` |
| 响应体 | `ApiResponse<DataSourceVO>` |

### 删除数据源

| 项目 | 值 |
|---|---|
| 方法 | `DELETE` |
| URL | `/api/v1/datasources/{id}` |
| 认证 | 需要 |
| 路径参数 | `id`: `Long` |
| 请求体 | 无 |
| 响应体 | `ApiResponse<Void>` |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

### 测试数据源连接

| 项目 | 值 |
|---|---|
| 方法 | `POST` |
| URL | `/api/v1/datasources/{id}/test` |
| 认证 | 需要 |
| 路径参数 | `id`: `Long` |
| 请求体 | 无 |
| 响应体 | `ApiResponse<Boolean>` |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

#### 连接失败响应示例

```json
{
  "code": 500,
  "message": "连接失败",
  "data": null
}
```

**章节来源**   
- [数据源控制器:1-50](file://backend/src/main/java/com/nlp2sql/controller/DataSourceController.java#L1-L50)
- [数据源请求体:1-34](file://backend/src/main/java/com/nlp2sql/model/dto/DataSourceDTO.java#L1-L34)

## 元数据浏览接口

元数据接口用于查看某个数据源中的表结构和字段信息，也可主动刷新元数据缓存。

### 查询表列表

| 项目 | 值 |
|---|---|
| 方法 | `GET` |
| URL | `/api/v1/metadata/tables` |
| 认证 | 需要 |
| 查询参数 | `datasourceId`: `Long` |
| 响应体 | `ApiResponse<List<TableInfo>>` |

#### 查询参数

| 参数 | 类型 | 是否必填 | 说明 |
|---|---|---:|---|
| `datasourceId` | `Long` | 是 | 数据源 ID |

#### 响应模型：表信息

| 字段 | 类型 | 说明 |
|---|---|---|
| `tableName` | `String` | 表名 |
| `comment` | `String` | 表注释 |
| `columns` | `List<ColumnInfo>` | 字段列表 |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "tableName": "user",
      "comment": "用户表",
      "columns": []
    }
  ]
}
```

### 查询表字段列表

| 项目 | 值 |
|---|---|
| 方法 | `GET` |
| URL | `/api/v1/metadata/tables/{tableName}/columns` |
| 认证 | 需要 |
| 路径参数 | `tableName`: `String` |
| 查询参数 | `datasourceId`: `Long` |
| 响应体 | `ApiResponse<List<ColumnInfo>>` |

#### 路径参数

| 参数 | 类型 | 是否必填 | 说明 |
|---|---|---:|---|
| `tableName` | `String` | 是 | 表名 |

#### 查询参数

| 参数 | 类型 | 是否必填 | 说明 |
|---|---|---:|---|
| `datasourceId` | `Long` | 是 | 数据源 ID |

#### 响应模型：列信息

| 字段 | 类型 | 说明 |
|---|---|---|
| `columnName` | `String` | 列名 |
| `dataType` | `String` | 数据类型 |
| `columnType` | `String` | 列类型 |
| `comment` | `String` | 列注释 |
| `columnKey` | `String` | 键信息 |
| `isNullable` | `String` | 是否可空 |
| `defaultValue` | `String` | 默认值 |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "columnName": "id",
      "dataType": "bigint",
      "columnType": "BIGINT",
      "comment": "主键",
      "columnKey": "PRI",
      "isNullable": "NO",
      "defaultValue": null
    }
  ]
}
```

### 刷新元数据缓存

| 项目 | 值 |
|---|---|
| 方法 | `POST` |
| URL | `/api/v1/metadata/refresh` |
| 认证 | 需要 |
| 查询参数 | `datasourceId`: `Long` |
| 请求体 | 无 |
| 响应体 | `ApiResponse<Void>` |

#### 成功响应示例

```json
{
  "code": 200,
  "message": "success",
  "data": null
}
```

**章节来源**   
- [元数据控制器:1-38](file://backend/src/main/java/com/nlp2sql/controller/MetadataController.java#L1-L38)
- [表结构模型:1-14](file://backend/src/main/java/com/nlp2sql/model/TableInfo.java#L1-L14)
- [列结构模型:1-21](file://backend/src/main/java/com/nlp2sql/model/ColumnInfo.java#L1-L21)

## 客户端集成示例与最佳实践

### 前端集成方式

前端使用一个通用的 HTTP 客户端封装，并在 `endpoints.js` 中集中声明所有后端端点。当前前端封装的路径不包含 `/api/v1` 前缀，例如 `/auth/login`、`/chat`、`/metadata/tables`、`/datasources`。

| 功能 | 前端调用路径 | 后端实际路径 |
|---|---|---|
| 登录 | `/auth/login` | `/api/v1/auth/login` |
| 注册 | `/auth/register` | `/api/v1/auth/register` |
| 对话 | `/chat` | `/api/v1/chat` |
| 查询表 | `/metadata/tables` | `/api/v1/metadata/tables` |
| 查询列 | `/metadata/tables/{tableName}/columns` | `/api/v1/metadata/tables/{tableName}/columns` |
| 刷新元数据 | `/metadata/refresh` | `/api/v1/metadata/refresh` |
| 获取数据源列表 | `/datasources` | `/api/v1/datasources` |
| 创建数据源 | `/datasources` | `/api/v1/datasources` |
| 更新数据源 | `/datasources/{id}` | `/api/v1/datasources/{id}` |
| 删除数据源 | `/datasources/{id}` | `/api/v1/datasources/{id}` |
| 测试数据源 | `/datasources/{id}/test` | `/api/v1/datasources/{id}/test` |

这意味着前端 HTTP 客户端需要对请求路径做统一前缀处理，或者后端网关将不带 `/api/v1` 的请求转发到对应后端路径。

### 推荐客户端流程

1. 调用注册或登录接口，保存 `LoginResponse.token`。
2. 在所有后续请求中加入请求头：  
   `Authorization: Bearer <token>`
3. 对每个接口返回体检查 `code`：
   - `code === 200`：视为业务成功。
   - `code !== 200`：视为业务失败，展示 `message`。
4. 当收到 `401` 或 Token 相关错误时，引导用户重新登录。
5. 对 NL2SQL 接口：
   - 如果 `data.success === true`，渲染 `generatedSql`、`columns`、`rows`。
   - 如果 `data.success === false`，展示 `errorMessage`。
6. 对数据源接口：
   - 先调用测试连接接口验证配置是否正确。
   - 再调用创建或更新接口持久化配置。

### 请求示例

#### 登录

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password"
}
```

#### 对话

```http
POST /api/v1/chat
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

{
  "message": "查询用户表中的所有用户",
  "datasourceId": 1
}
```

#### 查询表

```http
GET /api/v1/metadata/tables?datasourceId=1
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### 创建数据源

```http
POST /api/v1/datasources
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

{
  "name": "test-mysql",
  "type": "mysql",
  "host": "localhost",
  "port": 3306,
  "databaseName": "sample_db",
  "username": "root",
  "password": "secret"
}
```

### 最佳实践

| 主题 | 建议 |
|---|---|
| Token 管理 | 将 Token 保存在安全存储中，避免明文日志输出 |
| 重试策略 | 对网络错误可重试，对业务错误不要盲目重试 |
| 超时控制 | 对 NL2SQL 接口设置合理超时，因为涉及 LLM 和数据库查询 |
| 分页限制 | 对于大量数据的查询结果，建议在业务层限制返回行数 |
| 敏感字段 | 不在日志中打印 `password`、`token`、`errorMessage` 中的敏感内容 |
| 跨域 | 部署时确保前端域名与后端域名之间的 CORS 已正确配置 |

**章节来源**   
- [前端端点调用封装:1-20](file://frontend/src/api/endpoints.js#L1-L20)
- [安全配置:1-44](file://backend/src/main/java/com/nlp2sql/config/SecurityConfig.java#L1-L44)

## 版本管理与向后兼容性

### 当前版本策略

当前 API 使用 URL 前缀 `/api/v1` 表示版本。控制器上直接使用 `/api/v1/xxx` 作为映射路径，没有看到动态版本解析逻辑。因此，新增版本时应新增新的前缀，例如 `/api/v2/xxx`，而不是覆盖现有路径。

### 向后兼容性建议

| 变更类型 | 兼容性建议 |
|---|---|
| 新增可选字段 | 保持兼容，客户端可忽略未知字段 |
| 新增必填字段 | 应通过新接口或新版本发布，避免破坏旧客户端 |
| 重命名字段 | 应保留旧字段一段时间并迁移，或发布新版本 |
| 修改字段类型 | 应谨慎评估影响，必要时使用新字段或新版本 |
| 删除接口 | 应标记废弃并保留过渡期，不能直接移除 |
| 修改错误码 | 不应随意改变已有错误码语义 |

### 安全相关注意事项

- 当前所有非白名单接口都需要认证。
- 鉴权粒度较粗，只有“是否认证”判断，没有基于角色的资源级权限控制。
- 如果需要多租户或管理员隔离，应在服务层增加数据源级别或用户级别的权限校验。
- JWT 密钥应通过环境变量或配置中心注入，不应硬编码。

**章节来源**   
- [认证控制器:1-31](file://backend/src/main/java/com/nlp2sql/controller/AuthController.java#L1-L31)
- [对话控制器:1-30](file://backend/src/main/java/com/nlp2sql/controller/ChatController.java#L1-L30)
- [数据源控制器:1-50](file://backend/src/main/java/com/nlp2sql/controller/DataSourceController.java#L1-L50)
- [元数据控制器:1-38](file://backend/src/main/java/com/nlp2sql/controller/MetadataController.java#L1-L38)
- [安全配置:1-44](file://backend/src/main/java/com/nlp2sql/config/SecurityConfig.java#L1-L44)

## 故障排查指南

### 常见问题

| 现象 | 可能原因 | 处理方式 |
|---|---|---|
| 登录成功但后续请求报未认证 | 缺少 `Authorization` 请求头或 Token 过期 | 检查前端是否正确携带 `Bearer <token>` |
| 参数校验失败 | 必填字段缺失或类型不正确 | 根据 `message` 中的字段错误提示修正请求体 |
| 数据源连接失败 | 数据库地址、端口、账号或密码错误 | 先调用连接测试接口，确认配置正确 |
| NL2SQL 返回失败 | 未找到 Schema、SQL 生成失败或执行失败 | 检查 `data.errorMessage`，并确认数据源元数据可用 |
| 返回 `code=9999` | 服务端出现未预期异常 | 查看服务端日志定位根因 |
| 元数据为空 | 数据源不可用或缓存未刷新 | 调用刷新接口并重新查询 |

### 调试建议

1. 优先确认 HTTP 状态码和 `ApiResponse.code`。
2. 对于参数校验错误，关注 `GlobalExceptionHandler` 合并后的字段错误消息。
3. 对于业务异常，重点看 `code` 字段是否落在 `ErrorCode` 定义范围内。
4. 对于 NL2SQL 接口，同时记录 `nlInput`、`generatedSql`、`errorMessage` 以便定位 AI 生成问题。
5. 对于数据源问题，优先调用 `/api/v1/datasources/{id}/test` 排除底层连接问题。

**章节来源**   
- [全局异常处理器:1-50](file://backend/src/main/java/com/nlp2sql/config/GlobalExceptionHandler.java#L1-L50)
- [业务异常:1-23](file://backend/src/main/java/com/nlp2sql/common/BusinessException.java#L1-L23)
- [错误码常量:1-26](file://backend/src/main/java/com/nlp2sql/common/ErrorCode.java#L1-L26)

## 结论

本项目的 REST API 以 `/api/v1` 为统一入口，围绕认证、NL2SQL 对话、数据源管理和元数据浏览四个核心领域提供服务。接口设计遵循 Spring MVC 常规风格，使用 `@RestController` 和 `@RequestMapping` 声明路径，使用 Jakarta Validation 做参数校验，使用 `ApiResponse` 统一响应结构，并通过全局异常处理器收敛错误。

对客户端而言，最关键的是正确处理 JWT 认证、统一响应体的 `code` 字段，以及针对 NL2SQL 接口的成功与失败分支。若未来引入更多安全策略、版本演进或复杂查询能力，建议在保持现有响应结构的基础上，逐步增强服务层的权限控制和错误语义。