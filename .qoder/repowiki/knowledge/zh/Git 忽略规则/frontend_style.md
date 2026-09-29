## 1. 使用的系统与工具

- **框架**：Vue 3（Composition API，`<script setup>`）+ Vite 5 构建。
- **组件库**：Element Plus 2.x（`element-plus` + `@element-plus/icons-vue`），作为主要 UI 组件来源；项目未引入 Element Plus 的 SCSS/Sass 主题定制入口，也未见自定义 theme token 覆盖文件，基本使用默认主题并通过 CSS 变量与覆盖样式微调。
- **状态管理**：Pinia（无 UI 含义，但影响组件结构）。
- **路由**：vue-router 4。
- **HTTP**：axios，封装在 `src/api/client.js` / `endpoints.js`。
- **CSS 预处理**：无预处理器（package.json 中无 sass/scss/less 依赖），全部为原生 CSS。

## 2. 关键文件

- `frontend/package.json` — 声明 `element-plus`、`@element-plus/icons-vue` 等依赖。
- `frontend/src/style.css` — 全局设计令牌（design tokens）、颜色方案、排版、响应式断点。
- `frontend/src/assets/main.css` — 应用级布局（侧边栏 + 主内容区）和聊天气泡、SQL 代码块等业务样式。
- `frontend/src/App.vue` — 根组件，挂载 Element Plus 并提供全局样式容器。
- `frontend/src/components/ChatMessage.vue`、`ChatInput.vue`、`ResultTable.vue`、`SchemaExplorer.vue`、`DataSourceSelector.vue` — 业务组件内联 `<style scoped>`。
- `frontend/src/views/ChatPage.vue`、`LoginPage.vue`、`SettingsPage.vue` — 页面级样式。

## 3. 架构与约定

### 3.1 设计令牌（Design Tokens）

所有视觉常量集中在 `src/style.css` 的 `:root` 伪类下，采用 CSS 自定义属性命名：

- 文本与背景：`--text`、`--text-h`、`--bg`
- 边框与强调色：`--border`、`--accent`、`--accent-bg`、`--accent-border`
- 语义色：`--code-bg`、`--social-bg`
- 阴影：`--shadow`
- 字体族：`--sans`、`--heading`、`--mono`
- 业务布局常量：`--primary-color`、`--sidebar-width`（在 `assets/main.css` 中定义）

这些变量同时用于 Element Plus 组件覆盖（如按钮、输入框等）和业务组件自身样式。

### 3.2 明暗主题

通过 `@media (prefers-color-scheme: dark)` 切换同一组 CSS 变量的取值，实现系统级明暗模式。亮色与暗色的 accent 色分别使用 `#aa3bff` 与 `#c084fc`，其余变量成对替换。

### 3.3 响应式策略

- 统一断点：`@media (max-width: 1024px)` 出现在多处（`style.css` 中对 `h1`、`h2`、`font-size`、`padding`、`gap` 等进行降级）。
- 使用现代布局：Flexbox 为主（`app-container`、`app-sidebar`、`app-main`、消息气泡区域等）。
- 移动端优化：`min-height: 100svh`、`inset-inline`、`place-content`、`place-items` 等现代 CSS 特性。

### 3.4 样式组织方式

- **全局样式**：`style.css` 提供设计令牌、基础排版、全局容器 `#app`、标题与代码块样式。
- **应用布局**：`assets/main.css` 定义 `app-container` / `app-sidebar` / `app-main` 三栏布局以及聊天消息气泡、SQL 展示块、输入区等应用级样式。
- **组件样式**：各 `.vue` 组件使用 `<style scoped>`，BEM 风格类名（如 `message-bubble`、`message-user`、`message-system`、`sql-display`、`chat-input-area`）。
- **Element Plus 覆盖**：通过 `:deep()` 或全局样式覆盖组件默认样式，未使用 `useCssVars` 或主题变量注入机制。

### 3.5 字体与排版

- 正文：`system-ui, 'Segoe UI', Roboto, sans-serif`，字号 18px，行高 145%。
- 标题：`var(--heading)`，加粗 500，`h1` 56px（≤1024px → 36px），`h2` 24px。
- 等宽：`ui-monospace, Consolas, monospace`，用于 code 与 SQL 展示。
- 中文回退：`'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif`（主要在 `assets/main.css`）。