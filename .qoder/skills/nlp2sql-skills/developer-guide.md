# 架构与开发指南

## 1. 三层架构

### 1.1 Controller 层

Controller 仅负责接收参数、调用 Service、返回结果。入参用 DTO，出参用 VO（禁止 Entity 直出直入），统一返回 `ApiResponse<T>`。

```java
@RestController
@RequestMapping("/api/v1/datasources")
@RequiredArgsConstructor
public class DataSourceController {

    private final DataSourceService dataSourceService;

    @PostMapping
    public ApiResponse<DataSourceVO> create(@Valid @RequestBody DataSourceDTO dto) {
        return ApiResponse.ok(dataSourceService.create(dto));
    }
}
```

**强制项**：
- Controller 禁止直接注入 Mapper，数据加载全部委托 Service
- 写操作返回 VO，不返回 Entity
- 构造器注入 `@RequiredArgsConstructor`，禁止字段注入

### 1.2 Service 层

业务逻辑与事务边界，可被多个 Controller 及其他 Service 调用。不做接口/实现分离。

```java
@Service
@RequiredArgsConstructor
public class DataSourceService {

    private final DataSourceMapper dataSourceMapper;
    private final DataSourceFactory dataSourceFactory;

    public DataSourceVO create(DataSourceDTO dto) {
        DataSource ds = toEntity(dto);
        dataSourceMapper.insert(ds);
        return DataSourceVO.fromEntity(ds);
    }

    /** 供其他 Service 内部调用，返回 Entity */
    public DataSource getById(Long id) {
        DataSource ds = dataSourceMapper.selectById(id);
        if (ds == null) {
            throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "数据源不存在");
        }
        return ds;
    }
}
```

**规范**：
- Service 方法接受 DTO，对外返回 VO
- 内部方法（供其他 Service 调用）可返回 Entity
- DTO→Entity 转换逻辑放在 Service，不放 Controller
- `@Transactional(rollbackFor = Exception.class)` 加在写方法上

### 1.3 Mapper 层

继承 `BaseMapper<T>`，简单条件用 `LambdaQueryWrapper`；确需手写 SQL 用注解。

```java
@Mapper
public interface DataSourceMapper extends BaseMapper<DataSource> {
}
```

**禁止**：
- `SELECT *`（新写 SQL 必须显式列名）
- `${}` 拼接条件参数（排序字段走白名单枚举）
- `*Mapper.xml`（优先注解或 LambdaQueryWrapper）

### 1.4 DTO / VO 层

| 类型 | 用途 | 命名 |
|------|------|------|
| DTO | 入参，携带 `jakarta.validation` 校验注解 | `XxxDTO` |
| VO | 出参，敏感字段脱敏 | `XxxVO` |
| Entity | 数据库映射，禁止直接出现在 Controller 签名中 | 与表名对应 |

```java
@Data
public class DataSourceVO {
    private Long id;
    private String name;
    private String password;  // 脱敏为 "******"

    public static DataSourceVO fromEntity(DataSource ds) {
        DataSourceVO vo = new DataSourceVO();
        // ... 字段映射
        vo.setPassword("******");
        return vo;
    }
}
```

## 2. 通用基础组件

### 2.1 BusinessException + ErrorCode

统一业务异常机制，HTTP 状态恒为 200，判错看 body `code`。

```java
// 抛出
throw new BusinessException(ErrorCode.DATASOURCE_NOT_FOUND, "数据源不存在");

// 捕获（GlobalExceptionHandler 统一处理）
@ExceptionHandler(BusinessException.class)
@ResponseStatus(HttpStatus.OK)
public ApiResponse<Void> handleBusinessException(BusinessException ex) {
    return ApiResponse.error(ex.getCode(), ex.getMessage());
}
```

**错误码分段**：
- 4xx：通用 HTTP 语义（400/401/403/404）
- 1xxx：数据源域
- 2xxx：NL2SQL 引擎域
- 9xxx：系统级

新增域取下一个千位段，禁止散落魔法数字。

### 2.2 ApiResponse\<T\>

统一返回体：

```java
public class ApiResponse<T> {
    private int code;       // 200=成功，非 200=失败
    private String message;
    private T data;
}
```

- 成功：`ApiResponse.ok(data)` → `{code: 200, message: "success", data: ...}`
- 失败：`ApiResponse.error(code, message)` 或抛 `BusinessException`

### 2.3 TraceIdFilter

链路追踪过滤器，`@Order(Integer.MIN_VALUE)` 确保最先执行：

```
请求 → TraceIdFilter → 业务处理 → 响应
         ↓
    MDC("traceId") → 日志 pattern 输出 [%X{traceId}]
    Response Header: X-Trace-Id → 前端串联
```

- 优先复用上游 `X-Trace-Id` 请求头
- 无则生成 UUID（去掉横线）
- 写入 MDC 供日志输出
- 回写响应头供前端/调用方关联
- `finally` 块清除 MDC，防止线程复用时污染

**日志 pattern 配置**：
```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] [%X{traceId}] %-5level %logger{36} - %msg%n"
```

**注意**：`@Async` 和线程池任务不自动继承 MDC，需手动传递 traceId。

## 3. 多数据源适配器模式

用于支持多种数据源类型（MySQL、PostgreSQL、Hive 等），通过工厂 + 缓存管理。

```
BaseDataSourceAdapter (抽象类)
├── MySQLAdapter
├── PostgreSQLAdapter (待扩展)
└── HiveAdapter (待扩展)

DataSourceFactory (工厂 + ConcurrentHashMap 缓存)
├── getAdapter(id, type, config)  → 缓存命中则复用
├── createAdapter(type)           → switch 创建新实例
├── removeAdapter(id)             → 清除缓存 + disconnect
└── testConnection(type, config)  → 创建临时实例测试后 disconnect
```

**核心方法**：
- `connect(config)` — 建立连接
- `disconnect()` — 释放连接
- `getTables()` — 获取表结构元数据
- `executeQuery(sql, limit)` — 执行查询
- `getDialect()` — 返回 SQL 方言标识

**扩展新数据源**：
1. 创建 `XxxAdapter extends BaseDataSourceAdapter`
2. 在 `DataSourceFactory.createAdapter()` 的 switch 中添加 case
3. 实现所有抽象方法

## 4. 元数据缓存策略

Schema 元数据通过 Redis 缓存，避免频繁查询 INFORMATION_SCHEMA：

```
getSchema(datasourceId)
├── 1. 查 Redis 缓存 key: "schema:{datasourceId}"
├── 2. 命中 → 直接返回
├── 3. 未命中 → 通过 Adapter 从数据库实时提取
└── 4. 写入 Redis，TTL = schema-cache.ttl-seconds (默认 3600s)
```

**缓存刷新**：
- 手动：`POST /api/v1/metadata/refresh?datasourceId=xxx`
- 数据源更新/删除时自动清除 Adapter 缓存

## 5. 构造器注入规范

所有 Spring Bean 使用 `@RequiredArgsConstructor` + `final` 字段：

```java
@Service
@RequiredArgsConstructor
public class DataSourceService {
    private final DataSourceMapper dataSourceMapper;      // ✅
    private final DataSourceFactory dataSourceFactory;    // ✅
}
```

**禁止**：
- `@Autowired` 字段注入
- `@Autowired` setter 注入
- 手动编写构造函数（Lombok 自动生成）

## 6. CORS 配置

开发环境限定具体 origin，不使用 `*` 通配：

```java
config.addAllowedOrigin("http://localhost:5173");  // Vite dev server
config.addAllowedOrigin("http://localhost:3000");
config.setAllowCredentials(true);
config.addExposedHeader("X-Trace-Id");  // 暴露 traceId 给前端
```
