## 1. 使用的系统与框架

后端基于 Spring Boot，采用以下机制加载和分层配置：
- `application.yml`（主配置）+ `application-ollama.yml`（profile 覆盖）实现多环境/多模型切换。
- `@ConfigurationProperties(prefix = "nlp2sql.model")` 绑定业务配置到 Java Bean。
- `@Value("${...}")` 注入散列的细粒度配置项。
- SnakeYAML (`org.yaml.snakeyaml.Yaml`) 动态加载 `resources/prompts/v1.yml` 等外部提示词模板。
- Spring AI DashScope / Ollama 通过 `spring.ai.dashscope.*`、`spring.ai.ollama.*` 属性接入大模型。

前端使用 Vite + Vue3，通过 `frontend/src/api/client.js`、`endpoints.js` 集中管理后端 API 地址；无独立的前端配置文件。Python 侧仅有一个测试脚本，未涉及运行时配置系统。

## 2. 关键文件

**后端核心配置：**
- `backend/src/main/resources/application.yml` — 主配置，包含 server、datasource、redis、jackson、spring.ai、mybatis-plus、jwt、nlp2sql.model、sql-security、prompt、schema-cache、springdoc、logging 等全部配置段。
- `backend/src/main/resources/application-ollama.yml` — 通过 `spring.autoconfigure.exclude` 排除 DashScope 自动装配，配合 `spring.profiles.active=ollama` 启用本地 Ollama 模式。
- `backend/src/main/java/com/nlp2sql/config/ModelProperties.java` — `@ConfigurationProperties(prefix="nlp2sql.model")` 绑定的模型选择 Bean，提供 `getActiveModelName()` 根据 provider 返回 dashscope/ollama 模型名。
- `backend/src/main/resources/prompts/v1.yml` — 版本化提示词模板（system_prompt、retry_feedback、keywords、examples），由 `PromptTemplateService` 在 `@PostConstruct` 中按 `prompt.version` 动态加载。

**通过 `@Value` 读取的配置点：**
- `JwtTokenProvider`：`jwt.secret`、`jwt.expiration`
- `NL2SQLEngine`：`sql-security.max-limit:100`、`sql-security.max-retry:2`
- `MetadataService`：`schema-cache.ttl-seconds:3600`
- `PromptTemplateService`：`prompt.version:v1`

**前端 API 基地址：**
- `frontend/src/api/client.js`、`frontend/src/api/endpoints.js` — 集中声明 `/api/chat`、`/api/datasource`、`/api/auth` 等 REST 路径。

## 3. 架构与设计约定

### 3.1 配置分层策略
| 层级 | 载体 | 用途 | 示例 |
|---|---|---|---|
| 应用级 | `application.yml` | Spring Boot 默认配置（端口、数据源、Redis、日志、OpenAPI） | `server.port`, `spring.datasource.url` |
| 业务级 | `application.yml` 自定义前缀 | 项目自身运行参数 | `nlp2sql.model.provider`, `sql-security.max-limit`, `prompt.version` |
| 环境变量 | `${ENV_VAR:default}` | 敏感信息或部署期替换 | `${DASHSCOPE_API_KEY:}`, `${nlp2sql.model.dashscope-model}` |
| Profile | `application-ollama.yml` | 切换 LLM 后端（DashScope vs Ollama） | `spring.profiles.active=ollama` |
| 外部数据 | `prompts/v1.yml` | 可热更新的提示词模板，随版本演进 | `version=v1`, `description` |

### 3.2 模型选择流程
`ModelProperties.getActiveModelName()` 根据 `nlp2sql.model.provider`（`dashscope` 或 `ollama`）决定使用哪个模型名；该值同时被注入到 `spring.ai.dashscope.chat.options.model` 与 `spring.ai.ollama.chat.options.model`，再由 Spring AI 自动装配生效。

### 3.3 Profile 切换规则
- 默认激活 `dashscope` profile（`application.yml` 中 `spring.profiles.active: dashscope`）。
- 启动时指定 `--spring.profiles.active=ollama` 则 `application-ollama.yml` 生效，并通过 `spring.autoconfigure.exclude` 禁用 DashScope 所有自动装配类，避免缺少 `DASHSCOPE_API_KEY` 时报错。

### 3.4 提示词模板版本化
`PromptTemplateService` 在 `@PostConstruct` 阶段从 classpath 加载 `prompts/${prompt.version}.yml`，解析出 system_prompt、retry_feedback、keywords、examples 四个字段。新增版本只需在 `prompts/` 下增加新的 yml 文件并修改 `prompt.version`，无需重新编译代码。

## 4. 约定与约束

- **业务配置统一以 `nlp2sql.*` 为前缀**：`ModelProperties` 通过 `@ConfigurationProperties(prefix="nlp2sql.model")` 强制聚合模型相关配置（provider、dashscope-model、ollama-model）。
- **安全敏感值通过环境变量注入**：`spring.ai.dashscope.api-key` 使用 `${DASHSCOPE_API_KEY:}`，空字符串作为默认值，便于 CI/CD 注入。
- **所有 `@Value` 调用均带默认值**：`sql-security.max-limit:100`、`sql-security.max-retry:2`、`schema-cache.ttl-seconds:3600`、`prompt.version:v1`，确保即使对应 key 缺失也不会启动失败。
- **JWT 密钥硬编码在 `application.yml` 中**（`jwt.secret: nlp2sql-jwt-secret-key-2024-must-be-at-least-256-bits-long-for-hs256`），当前仓库未走环境变量——这是一个应改进的安全隐患。
- **Profile 命名遵循 `application-{name}.yml` 约定**：当前仅有 `application-ollama.yml`，通过 `spring.profiles.active` 激活。
- **提示词模板文件必须位于 `resources/prompts/` 目录，文件名格式为 `{version}.yml`**：`PromptTemplateService.loadTemplate()` 直接拼接 `prompts/${version}.yml` 路径，不存在会抛出 RuntimeException。
- **前端不内嵌后端地址**：`client.js` 中 base URL 指向相对路径 `/api`，依赖 Nginx/Gateway 反向代理转发到后端 8080 端口。
- **数据库密码与 Redis 密码明文写在 `application.yml`**（`password: "050407"`），属于开发样例配置，生产环境应迁移至环境变量或加密配置中心。

## 5. 适用范围

本配置体系仅适用于后端 Spring Boot 模块。前端通过 `vite.config.js` + JS 常量管理构建期/运行期变量；Python 测试脚本未集成配置框架。整体规模较小，尚未引入 Spring Cloud Config、Apollo、Consul 等外部配置中心。