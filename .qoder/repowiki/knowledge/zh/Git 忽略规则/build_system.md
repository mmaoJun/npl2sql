## 1. 使用的构建系统

该项目是一个前后端分离的 Java + Vue 3 工程，采用各自生态的原生工具进行编译、打包与运行：
- **后端**：基于 Spring Boot 3.3.4 的 Maven 工程，使用 `spring-boot-maven-plugin` 打包为可执行 JAR。
- **前端**：基于 Vue 3 + Vite 5 的 Node.js 工程，通过 `vite build` 生成静态资源。
- **Python 辅助脚本**：位于 `py_peoject/`，使用 Python venv，但未见独立的 `requirements.txt` / `setup.py` / `pyproject.toml` 等依赖清单。
- **根目录未提供** Makefile、Dockerfile、CI 配置文件（`.github/workflows` 不存在）、发布脚本或统一的版本管理脚本。

## 2. 关键文件

| 文件 | 作用 |
|---|---|
| `backend/pom.xml` | Maven 工程描述、依赖管理与构建插件配置 |
| `frontend/package.json` | Node 依赖、Vite 脚本入口 |
| `frontend/vite.config.js` | Vite 开发服务器与构建行为（代理等） |
| `.gitignore` | 根级忽略规则，统一排除各子模块构建产物 |

## 3. 架构与约定

### 后端（Maven / Spring Boot）
- 继承 `org.springframework.boot:spring-boot-starter-parent:3.3.4`，Java 版本固定为 17（`<java.version>17</java.version>`）。
- 通过 `<properties>` 集中声明公共依赖版本：MyBatis-Plus 3.5.7、JJWT 0.12.6、JSQlParser 4.9、SpringDoc 2.6.0、Lombok 1.8.36；Spring AI 系列依赖通过 `spring-ai-bom` 与 `spring-ai-alibaba-bom` 在 `<dependencyManagement>` 中引入，由 BOM 统一管理版本。
- Lombok 作为 annotation processor 参与编译，并通过 `spring-boot-maven-plugin` 的 `<excludes>` 将其从最终 fat jar 中排除。
- MySQL Connector/J 标记为 `runtime` scope，测试依赖使用 `spring-boot-starter-test` 与 `spring-security-test`。
- 应用包名存在异常的多层嵌套路径（如 `com/nlp2sql/backend/src/main/java/com/nlp2sql/...`），这是仓库初始化时的路径复制问题，非构建层面的约定。

### 前端（Vite）
- `package.json` 暴露三个 npm scripts：`dev`（`vite`）、`build`（`vite build`）、`preview`（`vite preview`），遵循 Vite 默认约定。
- 依赖使用语义化版本范围（`^x.y.z`），未锁定精确版本；仅 `package-lock.json` 记录实际安装树。
- 前端被标记为 `private: true`，不会发布到 npm registry。

### 根级 `.gitignore`
- 同时覆盖 Python (`__pycache__`, `*.pyc`, `.venv`)、Java/Maven (`target/`, `*.class`, `*.jar`, `*.war`, `*.ear`)、Node.js (`node_modules/`, `dist/`)、IDE (`.idea/`, `*.iml`)、Jupyter (`.ipynb_checkpoints/`) 以及 `py_peoject/` 这类“实习生课题”目录。

## 4. 观察到的约定与约束

- **Java 运行时要求**：构建与运行需要 JDK 17（由 `pom.xml` 的 `java.version` 属性决定）。
- **后端构建命令**：`mvn package`（由 Spring Boot Maven Plugin 提供）生成 `target/nlp2sql-backend-1.0.0-SNAPSHOT.jar`。
- **前端构建命令**：`npm run build` 调用 `vite build` 输出到 `frontend/dist/`。
- **版本策略**：后端为 Maven 快照版本 `1.0.0-SNAPSHOT`；前端为 `0.0.0` 且 `private`，两者均未实现统一的版本号同步机制。
- **无容器化与 CI**：仓库中不存在 `Dockerfile`、`docker-compose.*`、`.github/workflows/*` 或其他 CI 配置文件，也未见任何发布流水线脚本。
- **无根级聚合构建**：没有 Makefile、shell 脚本或 Gradle 聚合任务来一次性构建前后端，开发者需分别在 `backend/` 与 `frontend/` 目录下执行各自的构建命令。