## 概述

本仓库为前后端分离项目，采用多语言技术栈，依赖管理分散在各自模块中：后端 Java/Spring Boot 使用 Maven（`pom.xml`），前端 Vue 3 使用 npm（`package.json` + `package-lock.json`）。未发现 Python 侧的依赖清单文件（`py_peoject/py_peoject/model_test/model_test.py` 仅含测试脚本，无 `requirements.txt` / `pyproject.toml`）。

## 后端：Maven + Spring Boot Parent + BOM

- **构建工具**：Maven，父 POM 继承 `org.springframework.boot:spring-boot-starter-parent:3.3.4`。
- **版本集中化策略**：
  - 通过 `<properties>` 声明第三方库版本：`mybatis-plus.version=3.5.7`、`jjwt.version=0.12.6`、`jsqlparser.version=4.9`、`springdoc.version=2.6.0`、`lombok.version=1.18.36`。
  - 对大型生态引入 `<dependencyManagement>` + `<scope>import</scope>` 导入两个 BOM：`org.springframework.ai:spring-ai-bom:1.0.0` 与 `com.alibaba.cloud.ai:spring-ai-alibaba-bom:1.0.0.3`，由 BOM 统一管控 Spring AI 及其 Alibaba 扩展的传递依赖版本。
- **关键依赖分组**：
  - Web/安全/数据：`spring-boot-starter-web`、`spring-boot-starter-security`、`spring-boot-starter-data-redis`、`spring-boot-starter-validation`。
  - ORM/DB：`mybatis-plus-spring-boot3-starter`（显式版本）、`mysql-connector-j`（`runtime` scope）。
  - LLM 接入：`spring-ai-alibaba-starter-dashscope`（通义千问/DashScope）、`spring-ai-starter-model-ollama`（本地模型）——通过 BOM 管理版本，不单独声明版本号。
  - SQL 校验：`jsqlparser:4.9`。
  - JWT：`jjwt-api`/`jjwt-impl`/`jjwt-jackson`（三件套，后两者 `runtime` scope）。
  - 文档：`springdoc-openapi-starter-webmvc-ui`。
  - Lombok：标记为 `optional=true`，并在 `maven-compiler-plugin.annotationProcessorPaths` 中注册；同时在 `spring-boot-maven-plugin.excludes` 中排除打包。
- **构建产物**：未启用 `maven-dependency-plugin` 等锁定插件，也无 `vendor/` 目录；依赖从远程 Maven 仓库拉取（默认 Central，未见私有仓库配置）。

## 前端：npm + package-lock.json

- **包管理器**：npm，依赖声明位于 `frontend/package.json`。
- **运行时依赖**：`vue@^3.5.42`、`vue-router@^4.6.4`、`pinia@^4.0.3`、`axios@^1.20.0`、`element-plus@^2.14.6`、`@element-plus/icons-vue@^2.3.2`。
- **开发依赖**：`vite@^5.4.11`、`@vitejs/plugin-vue@^5.1.4`。
- **版本约束**：全部使用语义化范围的 `^` 前缀，允许兼容更新；无固定锁版本。
- **锁定文件**：`frontend/package-lock.json` 存在，用于保证 CI/生产安装可重现。
- **脚本**：仅提供 `dev` / `build` / `preview`，无升级或审计脚本（如 `npm audit`、`npm outdated`）。

## 约定与约束

- **Java 三方库版本必须通过 `<properties>` 或 BOM 统一管理**：除 MySQL Connector/J（由 parent BOM 管理）外，所有业务依赖在 `pom.xml` 中以 `${xxx.version}` 引用，禁止硬编码版本号。（依据：`pom.xml` 中各依赖均通过属性引用）
- **Spring AI 相关依赖不得直接指定版本**：必须依赖 `spring-ai-bom` 和 `spring-ai-alibaba-bom` 提供的版本对齐，避免与 Spring Boot 3.3.4 不兼容。（依据：BOM import + 依赖未显式声明 version）
- **Lombok 不参与最终打包**：声明为 `optional=true` 且被 `spring-boot-maven-plugin` 排除。（依据：`pom.xml` 两处配置）
- **前端依赖范围按 `dependencies` / `devDependencies` 严格区分**：构建期工具（Vite、Vue plugin）放入 devDependencies，运行时代码放入 dependencies。（依据：`package.json` 结构）
- **未实现私有仓库/镜像配置**：根 `.gitignore` 与 `pom.xml` 中均未出现 `<repositories>` 或 `settings.xml` 自定义源；前端也未见 `.npmrc`。依赖拉取完全依赖默认中央仓库。
- **无自动化升级流程**：仓库内未发现 GitHub Actions、CI 脚本或脚本任务来执行依赖审计、过期检查或自动 PR。

## 关键文件

- `backend/pom.xml` — Maven 依赖声明、BOM 导入、版本属性、构建插件
- `frontend/package.json` — Node.js 依赖声明
- `frontend/package-lock.json` — npm 锁定文件