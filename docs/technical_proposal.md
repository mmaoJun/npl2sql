# NL2SQL 智能查询系统 - 技术方案文档

## 1. 项目背景

本项目为方正实习生课题，目标是基于 ≤7B 参数大模型（qwen2.5-coder:7b），构建支持多数据源的自然语言查询分析系统。用户通过自然语言提问，系统自动生成 SQL 并返回查询结果。

## 2. 技术选型

### 2.1 后端
- Java 17 + Spring Boot 3.3.4
- MyBatis-Plus 3.5.7（ORM）
- Redis 7.x（Schema 缓存）
- JWT（jjwt 0.12.6，无状态认证）
- JSqlParser 4.9（SQL AST 分析）
- OkHttp 4.12.0（调用 Ollama API）

### 2.2 前端
- Vue 3 + JavaScript（非 TypeScript）
- Element Plus 2.x（UI 组件库）
- Vite 5.x（构建工具）
- Pinia 2.x（状态管理）
- Axios（HTTP 客户端）

### 2.3 模型服务
- qwen2.5-coder:7b，通过 Ollama 本地部署
- REST API 调用，temperature=0，num_ctx=4096

## 3. 系统架构

### 3.1 分层架构
```
Controller → Service → Adapter → 数据源
                ↓
           Ollama API（模型推理）
```

- **Controller 层**: REST API 入口，JWT 校验
- **Service 层**: 核心业务逻辑（NL2SQL 引擎、Prompt 构建、SQL 校验、元数据管理）
- **Adapter 层**: 多数据源适配器（策略模式），当前实现 MySQLAdapter
- **Security 层**: JWT 认证 + SQL 安全检查

### 3.2 核心数据流
1. 用户输入自然语言问题
2. MetadataService 获取 Schema（Redis 缓存优先）
3. QueryClassifier 分类查询类型
4. PromptBuilder 构建提示词（系统指令 + Schema + Few-shot + 用户问题）
5. SQLGenerator 调用 Ollama API 生成 SQL
6. SQLValidator 校验 SQL 安全性（3 层校验）
7. Adapter 执行 SQL 查询
8. AuditService 记录审计日志
9. 返回结果

## 4. 核心模块

### 4.1 Prompt 工程
- 系统提示词（~200 tokens）：角色定义 + 规则约束
- Schema 上下文（~200-800 tokens）：三级压缩（L0 完整 DDL / L1 紧凑格式 / L2 仅表名+字段名）
- Few-shot 示例（~200 tokens）：按查询类型动态选择 2-3 个
- 总 Prompt 控制在 1500 tokens 以内

### 4.2 SQL 安全校验（纵深防御）
- L1: 关键词黑名单（INSERT/UPDATE/DELETE/DROP 等）
- L2: JSqlParser AST 分析（验证 SELECT、检查表/字段存在性）
- L3: 注入模式检测（堆叠查询、UNION 注入、注释注入）
- L4: 数据库只读用户（最终防线）

### 4.3 多数据源适配器
- BaseDataSourceAdapter 抽象基类
- MySQLAdapter 当前实现
- DataSourceFactory 工厂模式管理适配器实例
- 扩展新数据源只需继承基类并注册

## 5. 数据库设计

### 5.1 元数据库（nlp2sql_meta）
- `datasources` - 数据源配置
- `users` - 用户信息（BCrypt 加密）
- `audit_logs` - 审计日志

### 5.2 示例数据库（nlp2sql_sample）
- `departments` - 部门表
- `users` - 用户表
- `products` - 产品表
- `orders` - 订单表
- `order_items` - 订单明细表

## 6. API 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | /api/v1/auth/register | 用户注册 |
| POST | /api/v1/auth/login | 用户登录 |
| POST | /api/v1/chat | 自然语言查询 |
| GET | /api/v1/metadata/tables | 获取表列表 |
| GET | /api/v1/metadata/tables/{name}/columns | 获取字段列表 |
| POST | /api/v1/metadata/refresh | 刷新缓存 |
| CRUD | /api/v1/datasources | 数据源管理 |

## 7. 项目结构

```
nlp2sql/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue 3 前端
├── sql/              # SQL 脚本
│   ├── init_backend.sql
│   └── sample_data/mysql_sample.sql
├── docs/             # 文档
└── py_peoject/       # 原有 Python 测试代码
```

## 8. 部署说明

### 8.1 环境要求
- JDK 17+
- Maven 3.9+
- Node.js 18+
- MySQL 8.0
- Redis 7.x
- Ollama（已安装 qwen2.5-coder:7b）

### 8.2 启动步骤
1. 执行 `sql/init_backend.sql` 创建元数据库
2. 执行 `sql/sample_data/mysql_sample.sql` 创建示例数据
3. 修改 `backend/src/main/resources/application.yml` 中的数据库和 Redis 配置
4. 后端：`cd backend && mvn spring-boot:run`
5. 前端：`cd frontend && npm install && npm run dev`
6. 确保 Ollama 正在运行：`ollama serve`
