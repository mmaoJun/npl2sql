---
name: nlp2sql-skills
description: NL2SQL 项目知识库。涵盖 Spring Boot 3.x 分层架构、通用基础组件（BusinessException/ApiResponse/TraceIdFilter）、Spring AI 双 Provider 模型切换、多数据源适配器模式。在编写、审查或设计 Java/Spring Boot 后端代码时使用。
---

# NL2SQL 开发标准

## 规范索引

| 文件 | 说明 | 关键约束 |
|------|------|----------|
| [rules.md](./rules.md) | 编码核心原则 | 先思考再动手、最小改动、目标驱动 |
| [developer-guide.md](./developer-guide.md) | 架构与开发指南 | 三层架构、DTO/VO 分离、通用组件用法、错误码规范 |
| [quality-guide.md](./quality-guide.md) | 代码质量红线 | 圈复杂度≤10、重复率≤5%、注释纪律、依赖方向、禁止模式 |
| [spring-ai-guide.md](./spring-ai-guide.md) | Spring AI 集成指南 | 双 Provider 切换、ChatModel 注入、Prompt 模板管理 |
| [common-issues.md](./common-issues.md) | 常见问题排查 | 编译错误、运行时异常、模型调用故障 |

---

## 1. 技术栈

**后端**：Java 17、Spring Boot 3.3.4、MyBatis-Plus 3.5.7、MySQL 8.x、Redis（Lettuce）、Spring Security + JWT。
**AI 集成**：Spring AI 1.0.0 + Spring AI Alibaba 1.0.0.3，双 Provider（DashScope 云端 + Ollama 本地），通过 `BeanDefinitionRegistryPostProcessor` 解决双 ChatModel bean 冲突。
**SQL 处理**：JSqlParser 4.9 做 SQL 解析与安全校验。
**前端**：Vue 3 + Vite + Element Plus + Pinia + Axios。
**文档**：springdoc-openapi 2.6.0（OpenAPI 3 + Swagger UI）。
**工具**：Lombok 1.18.36、Maven。

## 2. 项目结构

```
backend/src/main/java/com/nlp2sql/
├── common/              # 通用基础组件（可跨项目复用）
│   ├── BusinessException.java   # 业务异常，携带错误码
│   ├── ErrorCode.java           # 错误码常量，按域分段
│   └── TraceIdFilter.java       # 链路追踪过滤器，MDC + X-Trace-Id
├── config/              # 配置类
│   ├── ChatModelPrimarySelector.java  # 双 ChatModel Primary 选择器
│   ├── GlobalExceptionHandler.java    # 全局异常处理
│   ├── ModelProperties.java           # 模型配置属性
│   ├── SecurityConfig.java            # Spring Security 配置
│   └── WebConfig.java                 # CORS 配置
├── controller/          # Controller 层（纯转发，不含业务逻辑）
├── service/             # Service 层（业务逻辑与事务边界）
├── mapper/              # MyBatis-Plus Mapper 接口
├── model/               # Entity
│   ├── dto/             # 入参 DTO + ApiResponse + ChatRequest/Response
│   └── vo/              # 出参 VO（Entity 禁止直出）
├── adapter/             # 多数据源适配器
│   ├── BaseDataSourceAdapter.java     # 抽象适配器
│   ├── MySQLAdapter.java              # MySQL 实现
│   └── DataSourceFactory.java         # 适配器工厂 + 缓存
└── security/            # 安全相关（JWT、SQL 校验）
```

## 3. 分层规则

| 层 | 职责 | 禁止 |
|---|---|---|
| Controller | 接参 → 调 Service → 返回 `ApiResponse<T>` | 直接操作 Mapper、写业务逻辑、Entity 直出 |
| Service | 业务逻辑、事务边界、DTO↔Entity 转换 | 直接操作 HttpServletRequest/Response |
| Mapper | 继承 `BaseMapper<T>`，数据访问 | 写业务逻辑、XML（用 LambdaQueryWrapper 或注解） |

## 4. 通用组件速查

| 组件 | 包路径 | 用途 |
|------|--------|------|
| `BusinessException` | `com.nlp2sql.common` | 抛出业务异常，携带 `code` + `message` |
| `ErrorCode` | `com.nlp2sql.common` | 错误码常量：4xx 通用 / 1xxx 数据源 / 2xxx NL2SQL / 9xxx 系统 |
| `ApiResponse<T>` | `com.nlp2sql.model.dto` | 统一返回体 `{code, message, data}`，成功 `code=200` |
| `TraceIdFilter` | `com.nlp2sql.common` | 生成/透传 traceId → MDC → 日志 + 响应头 |
| `DataSourceVO` | `com.nlp2sql.model.vo` | 数据源出参 VO，密码自动脱敏为 `******` |

## 5. 错误码规范

错误码集中定义在 `ErrorCode` 常量类，按业务域分段，禁止散落魔法数字：

| 段 | 域 | 示例 |
|---|---|---|
| 4xx | 通用 HTTP 语义 | `BAD_REQUEST=400`, `UNAUTHORIZED=401`, `NOT_FOUND=404` |
| 1xxx | 数据源 | `DATASOURCE_NOT_FOUND=1001`, `DATASOURCE_CONNECTION_FAILED=1002` |
| 2xxx | NL2SQL 引擎 | `SQL_GENERATION_FAILED=2001`, `SQL_VALIDATION_FAILED=2002` |
| 9xxx | 系统级 | `SYSTEM_ERROR=9999` |

新增业务域错误码时，取下一个千位段（如 3xxx）。

## 6. 返回契约

| 场景 | HTTP 状态 | body code | 说明 |
|------|-----------|-----------|------|
| 成功 | 200 | 200 | `ApiResponse.ok(data)` |
| 业务异常 | 200 | 非 200 | `throw new BusinessException(code, msg)` |
| 参数校验失败 | 400 | 400 | `@Valid` 自动触发 |
| 未捕获异常 | 500 | 9999 | 消息掩码为「服务器内部错误」 |

## 7. 常见陷阱

- **DashScope 模型名**：必须使用平台真实模型名（`qwen-plus`、`qwen-turbo`、`qwen-max`），不能用版本号格式如 `qwen3.7-flash`。
- **双 ChatModel 冲突**：DashScope 和 Ollama 同时引入时，必须通过 `ChatModelPrimarySelector` 设置 Primary，否则 Spring 注入报错。
- **Entity 禁止直出**：所有 API 返回必须经过 VO 转换，密码等敏感字段在 VO 中脱敏。
- **Controller 不含逻辑**：Controller 只做接参→调 Service→返回，数据加载、DTO 转换、缓存清理等全部在 Service 层。
- **MDC traceId 不跨线程**：`@Async` 和线程池任务不自动继承 MDC，需要业务手动传递。

## 8. 代码质量红线

详细规则见 [quality-guide.md](./quality-guide.md)。

| 指标 | 阈值 |
|------|------|
| 圈复杂度 | ≤ 10 / 方法 |
| 重复代码率 | ≤ 5% 项目级 |
| 文件行数 | < 600 行 |
| 方法长度 | ≤ 100 行 |
| 方法参数 | ≤ 5 个 |
| 嵌套层数 | ≤ 3 层 |
| 注释纪律 | 零禁止类注释（仅描述业务意图） |
| 依赖方向 | 自上而下，禁止反向/循环 |
| 死代码 | 零容忍，不做历史兼容 |
| 构造器注入 | `@RequiredArgsConstructor`，禁止字段注入 |
| 日志 | SLF4J 占位符，禁止字符串拼接 |
| public 方法 | 必须有 Javadoc |
