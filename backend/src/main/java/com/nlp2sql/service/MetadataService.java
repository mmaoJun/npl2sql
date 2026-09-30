package com.nlp2sql.service;

import com.nlp2sql.model.ColumnInfo;
import com.nlp2sql.model.DataSource;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.adapter.DataSourceAdapter;
import com.nlp2sql.adapter.DataSourceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 元数据服务。
 *
 * <p>从数据源获取表结构信息（表名、列名、类型、注释），支持 Redis 缓存。
 * 缓存 Key 格式为 {@code schema:{datasourceId}}，TTL 默认 3600 秒。
 * Redis 不可用时自动降级为直连查询，不影响功能。
 *
 * @see DataSourceAdapter
 * @see DataSourceFactory
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

    private static final String SCHEMA_CACHE_PREFIX = "schema:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DataSourceFactory dataSourceFactory;
    private final DataSourceService dataSourceService;

    @Value("${schema-cache.ttl-seconds:3600}")
    private long cacheTtlSeconds;

    /**
     * 获取数据源的完整 Schema（含列信息）。
     *
     * <p>优先从 Redis 缓存读取；缓存未命中时通过适配器直连数据源查询，
     * 查询结果写入缓存。Redis 异常时自动降级为直连模式。
     *
     * @param datasourceId 数据源 ID
     * @return 表结构列表，每张表包含列名、类型、注释等信息
     */
    public List<TableInfo> getSchema(Long datasourceId) {
        String cacheKey = SCHEMA_CACHE_PREFIX + datasourceId;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List) {
                @SuppressWarnings("unchecked")
                List<TableInfo> tables = (List<TableInfo>) cached;
                if (!tables.isEmpty()) {
                    log.debug("从缓存获取 Schema: datasourceId={}", datasourceId);
                    return tables;
                }
            }
        } catch (Exception e) {
            log.warn("Redis 缓存读取失败: {}", e.getMessage());
        }

        DataSource ds = dataSourceService.getById(datasourceId);
        DataSourceAdapter adapter = dataSourceFactory.getAdapter(
                datasourceId, ds.getType(), ds.toAdapterConfig());
        List<TableInfo> tables = adapter.getTables();

        try {
            redisTemplate.opsForValue().set(cacheKey, tables, cacheTtlSeconds, TimeUnit.SECONDS);
            log.debug("Schema 已写入缓存: datasourceId={}, tables={}", datasourceId, tables.size());
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: {}", e.getMessage());
        }

        return tables;
    }

    /**
     * 强制刷新 Schema 缓存。
     *
     * <p>清除 Redis 缓存和适配器缓存后重新加载 Schema，用于数据源结构变更后的手动刷新。
     *
     * @param datasourceId 数据源 ID
     */
    public void refreshCache(Long datasourceId) {
        String cacheKey = SCHEMA_CACHE_PREFIX + datasourceId;
        redisTemplate.delete(cacheKey);
        dataSourceFactory.removeAdapter(datasourceId);
        getSchema(datasourceId);
    }

    /**
     * 获取数据源的表列表（不含列信息）。
     *
     * <p>仅返回表名和注释，用于前端表选择下拉框等轻量场景。
     *
     * @param datasourceId 数据源 ID
     * @return 表信息列表（仅表名 + 注释）
     */
    public List<TableInfo> getTables(Long datasourceId) {
        List<TableInfo> tables = getSchema(datasourceId);
        return tables.stream().map(t -> {
            TableInfo info = new TableInfo();
            info.setTableName(t.getTableName());
            info.setComment(t.getComment());
            return info;
        }).collect(Collectors.toList());
    }

    /**
     * 获取指定表的列信息。
     *
     * @param datasourceId 数据源 ID
     * @param tableName    表名（不区分大小写）
     * @return 列信息列表；表不存在时返回空列表
     */
    public List<ColumnInfo> getColumns(Long datasourceId, String tableName) {
        List<TableInfo> tables = getSchema(datasourceId);
        return tables.stream()
                .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                .findFirst()
                .map(TableInfo::getColumns)
                .orElse(Collections.emptyList());
    }
}
