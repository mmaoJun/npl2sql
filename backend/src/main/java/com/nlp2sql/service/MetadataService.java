package com.nlp2sql.service;

import com.nlp2sql.model.ColumnInfo;
import com.nlp2sql.model.DataSource;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.adapter.BaseDataSourceAdapter;
import com.nlp2sql.adapter.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class MetadataService {

    private static final Logger log = LoggerFactory.getLogger(MetadataService.class);
    private static final String SCHEMA_CACHE_PREFIX = "schema:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final DataSourceFactory dataSourceFactory;

    @Value("${schema-cache.ttl-seconds:3600}")
    private long cacheTtlSeconds;

    public MetadataService(RedisTemplate<String, Object> redisTemplate,
                           DataSourceFactory dataSourceFactory) {
        this.redisTemplate = redisTemplate;
        this.dataSourceFactory = dataSourceFactory;
    }

    public List<TableInfo> getSchema(Long datasourceId, DataSource dsConfig) {
        String cacheKey = SCHEMA_CACHE_PREFIX + datasourceId;

        // 先查 Redis 缓存
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

        // 从数据库实时提取
        Map<String, Object> config = buildConfig(dsConfig);
        BaseDataSourceAdapter adapter = dataSourceFactory.getAdapter(datasourceId, dsConfig.getType(), config);
        List<TableInfo> tables = adapter.getTables();

        // 写入缓存
        try {
            redisTemplate.opsForValue().set(cacheKey, tables, cacheTtlSeconds, TimeUnit.SECONDS);
            log.debug("Schema 已写入缓存: datasourceId={}, tables={}", datasourceId, tables.size());
        } catch (Exception e) {
            log.warn("Redis 缓存写入失败: {}", e.getMessage());
        }

        return tables;
    }

    public void refreshCache(Long datasourceId, DataSource dsConfig) {
        String cacheKey = SCHEMA_CACHE_PREFIX + datasourceId;
        redisTemplate.delete(cacheKey);
        dataSourceFactory.removeAdapter(datasourceId);
        getSchema(datasourceId, dsConfig);
    }

    public List<TableInfo> getTables(Long datasourceId, DataSource dsConfig) {
        List<TableInfo> tables = getSchema(datasourceId, dsConfig);
        return tables.stream().map(t -> {
            TableInfo info = new TableInfo();
            info.setTableName(t.getTableName());
            info.setComment(t.getComment());
            return info;
        }).collect(Collectors.toList());
    }

    public List<ColumnInfo> getColumns(Long datasourceId, DataSource dsConfig, String tableName) {
        List<TableInfo> tables = getSchema(datasourceId, dsConfig);
        return tables.stream()
                .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                .findFirst()
                .map(TableInfo::getColumns)
                .orElse(Collections.emptyList());
    }

    private Map<String, Object> buildConfig(DataSource ds) {
        Map<String, Object> config = new HashMap<>();
        config.put("host", ds.getHost());
        config.put("port", ds.getPort());
        config.put("databaseName", ds.getDatabaseName());
        config.put("username", ds.getUsername());
        config.put("password", ds.getPassword());
        return config;
    }
}
