# 常见问题排查

## 编译错误

### 双 ChatModel bean 冲突

**症状**：启动报错 `NoUniqueBeanDefinitionException: expected single matching bean but found 2: dashscopeChatModel, ollamaChatModel`

**原因**：DashScope 和 Ollama starter 各注册了一个 `ChatModel` bean，Spring 无法确定注入哪个。

**解决**：确认 `ChatModelPrimarySelector` 存在且正常工作。检查 `nlp2sql.model.provider` 配置值是否为 `dashscope` 或 `ollama`。

---

### Lombok 注解处理器未生效

**症状**：`@RequiredArgsConstructor`、`@Data` 等注解不生效，编译报找不到构造函数或 getter/setter。

**解决**：确认 `pom.xml` 中 `maven-compiler-plugin` 配置了 `annotationProcessorPaths`：
```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
</annotationProcessorPaths>
```

## 运行时异常

### DashScope API 返回 HTTP 400 "url error"

**症状**：调用 DashScope 时报 `400 Bad Request`，消息包含 "url error"。

**原因**：模型名不存在于 DashScope 平台。`qwen3.7-flash` 等版本号格式的模型名不可用。

**解决**：使用平台真实模型名：`qwen-plus`、`qwen-turbo`、`qwen-max`、`qwen2.5-coder-plus`。

---

### Redis 连接失败

**症状**：启动时或运行时 `RedisConnectionFailureException`。

**排查**：
1. 确认 Redis 服务已启动：`redis-cli ping` → 应返回 `PONG`
2. 检查 `application.yml` 中 Redis 配置（host/port/password）
3. 确认 `commons-pool2` 依赖已引入（Lettuce 连接池需要）

**注意**：Redis 不可用时系统仍可工作（Schema 缓存读写失败会被 catch 并 warn 日志），但性能会下降。

---

### MyBatis-Plus 自动填充不生效

**症状**：`createdAt`、`updatedAt` 字段插入/更新时为 null。

**原因**：未配置 `MetaObjectHandler` Bean。

**解决**：添加配置类：
```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime::now, LocalDateTime.class);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime::now, LocalDateTime.class);
            }
        };
    }
}
```

## 前端问题

### Chat 接口返回数据解析失败

**症状**：前端发送聊天请求后，页面不显示结果或报 `Cannot read properties of undefined`。

**原因**：`ChatController` 返回 `ApiResponse<ChatResponse>`，axios 拦截器返回的是 `ApiResponse` body `{code, message, data}`，`ChatResponse` 在 `response.data` 中。

**解决**：前端需要先解包 `ApiResponse`：
```javascript
const res = await chat({ message, datasourceId })
const response = res.data  // 解包 ApiResponse → ChatResponse
if (response.success) { ... }
```

---

### CORS 跨域错误

**症状**：浏览器控制台报 `Access to XMLHttpRequest has been blocked by CORS policy`。

**排查**：
1. 确认 `WebConfig.java` 中 `addAllowedOrigin` 包含前端的 origin（如 `http://localhost:5173`）
2. 确认 `setAllowCredentials(true)` 已设置
3. 确认 `addExposedHeader("X-Trace-Id")` 已添加（如果前端需要读取该头）

## 模型调用故障

### Ollama 连接超时

**症状**：`ConnectTimeoutException` 或长时间无响应。

**排查**：
1. 确认 Ollama 服务运行中：`curl http://localhost:11434/api/tags`
2. 确认模型已拉取：`ollama list`
3. 检查 `num-ctx` 配置，过大可能导致内存不足（默认 4096）

---

### SQL 生成质量差

**可能原因**：
1. **Schema 信息不完整**：检查 `MetadataService.getSchema()` 返回的表结构是否包含所有相关表和字段注释
2. **Prompt 模板不够精确**：调整 `resources/prompts/v1.yml` 中的提示词
3. **模型能力不足**：尝试切换到更强的模型（如 `qwen-max` 或 `qwen2.5-coder-plus`）
4. **temperature 过高**：当前设为 0（确定性输出），如果被改过会导致结果不稳定
