# Spring AI 双 Provider 集成指南

## 1. 架构概述

NL2SQL 使用 Spring AI Alibaba 实现双 Provider 模型切换，支持 DashScope（通义千问云端）和 Ollama（本地模型）两种后端，通过配置一键切换，用于模型对比评估。

```
                    ┌─────────────────────┐
                    │  nlp2sql.model.     │
                    │  provider=dashscope │
                    │  | ollama           │
                    └────────┬────────────┘
                             │
                    ┌────────▼────────────┐
                    │ ChatModelPrimary    │
                    │ Selector (BDRP)     │
                    │ 设置 Primary bean   │
                    └────────┬────────────┘
                             │
              ┌──────────────┼──────────────┐
              │                             │
     ┌────────▼────────┐          ┌────────▼────────┐
     │ DashScope       │          │ Ollama           │
     │ ChatModel       │          │ ChatModel        │
     │ (云端 API)       │          │ (本地 11434)      │
     └─────────────────┘          └─────────────────┘
              │                             │
              └──────────┬──────────────────┘
                         │
                ┌────────▼────────┐
                │ @Primary        │
                │ ChatModel 注入   │
                │ 到 SQLGenerator │
                └─────────────────┘
```

## 2. Maven 依赖

```xml
<!-- Spring AI BOM -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>1.0.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
        <dependency>
            <groupId>com.alibaba.cloud.ai</groupId>
            <artifactId>spring-ai-alibaba-bom</artifactId>
            <version>1.0.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- DashScope (阿里云百炼 / 通义千问) -->
    <dependency>
        <groupId>com.alibaba.cloud.ai</groupId>
        <artifactId>spring-ai-alibaba-starter-dashscope</artifactId>
    </dependency>

    <!-- Ollama (本地模型) -->
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-ollama</artifactId>
    </dependency>
</dependencies>
```

## 3. 配置

### 3.1 application.yml

```yaml
spring:
  profiles:
    active: dashscope  # 切换: dashscope | ollama

  ai:
    dashscope:
      api-key: ${DASHSCOPE_API_KEY:}
      chat:
        options:
          model: ${nlp2sql.model.dashscope-model}
          temperature: 0

    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: ${nlp2sql.model.ollama-model}
          temperature: 0
          num-ctx: 4096

nlp2sql:
  model:
    provider: dashscope           # dashscope | ollama
    dashscope-model: qwen-plus    # 可选: qwen-turbo, qwen-max, qwen2.5-coder-plus
    ollama-model: qwen2.5-coder:7b
```

### 3.2 模型配置属性类

```java
@Data
@Component
@ConfigurationProperties(prefix = "nlp2sql.model")
public class ModelProperties {
    private String provider = "dashscope";
    private String dashscopeModel = "qwen-plus";
    private String ollamaModel = "qwen2.5-coder:7b";

    public String getActiveModelName() {
        return "dashscope".equals(provider) ? dashscopeModel : ollamaModel;
    }
}
```

## 4. 双 ChatModel Bean 冲突解决

两个 `spring-ai-*-starter` 各自动注册一个 `ChatModel` bean，直接注入会报 `NoUniqueBeanDefinitionException`。

**解决方案**：`BeanDefinitionRegistryPostProcessor` 在容器启动时根据配置将对应的 bean 标记为 `@Primary`。

```java
@Configuration
public class ChatModelPrimarySelector
        implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Map<String, String> PROVIDER_BEAN_MAP = Map.of(
            "dashscope", "dashscopeChatModel",
            "ollama", "ollamaChatModel"
    );

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
        String provider = environment.getProperty("nlp2sql.model.provider", "dashscope");
        String primaryBeanName = PROVIDER_BEAN_MAP.get(provider);

        if (registry.containsBeanDefinition(primaryBeanName)) {
            registry.getBeanDefinition(primaryBeanName).setPrimary(true);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        // no-op
    }
}
```

**关键点**：
- 必须在 `BeanDefinitionRegistryPostProcessor` 阶段设置，而非 `@PostConstruct`（那时 bean 已实例化）
- DashScope 的 bean 名是 `dashscopeChatModel`，Ollama 的是 `ollamaChatModel`
- 新增 Provider 时，只需在 `PROVIDER_BEAN_MAP` 中加一行

## 5. 业务层使用

直接注入 `ChatModel` 即可，Spring 自动注入标记了 `@Primary` 的那个：

```java
@Service
@RequiredArgsConstructor
public class SQLGenerator {
    private final ChatModel chatModel;  // 自动注入 Primary bean

    public String generate(String prompt) {
        return chatModel.call(prompt);
    }
}
```

## 6. DashScope 可用模型

| 模型名 | 说明 | 适用场景 |
|--------|------|----------|
| `qwen-plus` | 通义千问 Plus | 通用 NL2SQL（推荐） |
| `qwen-turbo` | 通义千问 Turbo | 快速响应，简单查询 |
| `qwen-max` | 通义千问 Max | 复杂查询，高精度 |
| `qwen2.5-coder-plus` | 代码专用模型 | SQL 生成精度更高 |

**注意**：DashScope API 只支持其平台上的模型，不能使用 `qwen3.7-flash` 等不存在的模型名，否则会返回 HTTP 400 "url error"。

## 7. 模型切换操作

### 切换到 DashScope
```yaml
spring:
  profiles:
    active: dashscope
nlp2sql:
  model:
    provider: dashscope
    dashscope-model: qwen-plus
```
设置环境变量：`export DASHSCOPE_API_KEY=sk-xxx`

### 切换到 Ollama
```yaml
spring:
  profiles:
    active: ollama
nlp2sql:
  model:
    provider: ollama
    ollama-model: qwen2.5-coder:7b
```
确保 Ollama 服务运行中：`ollama serve`，且已拉取模型：`ollama pull qwen2.5-coder:7b`

## 8. Prompt 模板管理

Prompt 模板通过外部 YAML 文件管理（`resources/prompts/v1.yml`），支持版本化：

```yaml
prompt:
  version: v1  # 对应 resources/prompts/ 目录下的 yml 文件名
```

切换 Prompt 版本只需修改配置，无需改代码。
