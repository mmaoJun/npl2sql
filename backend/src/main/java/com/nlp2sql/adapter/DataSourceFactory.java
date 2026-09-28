package com.nlp2sql.adapter;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataSourceFactory {

    private final Map<Long, BaseDataSourceAdapter> adapterCache = new ConcurrentHashMap<>();

    public BaseDataSourceAdapter getAdapter(Long datasourceId, String type, Map<String, Object> config) {
        return adapterCache.computeIfAbsent(datasourceId, id -> {
            BaseDataSourceAdapter adapter = createAdapter(type);
            adapter.connect(config);
            return adapter;
        });
    }

    public BaseDataSourceAdapter createAdapter(String type) {
        return switch (type.toLowerCase()) {
            case "mysql" -> new MySQLAdapter();
            // 后续扩展:
            // case "postgresql" -> new PostgreSQLAdapter();
            // case "hive" -> new HiveAdapter();
            // case "hdfs" -> new HDFSAdapter();
            // case "rest_api" -> new RestApiAdapter();
            // case "graphql" -> new GraphQLAdapter();
            default -> throw new IllegalArgumentException("不支持的数据源类型: " + type);
        };
    }

    public void removeAdapter(Long datasourceId) {
        BaseDataSourceAdapter adapter = adapterCache.remove(datasourceId);
        if (adapter != null) {
            adapter.disconnect();
        }
    }

    public boolean testConnection(String type, Map<String, Object> config) {
        BaseDataSourceAdapter adapter = createAdapter(type);
        try {
            return adapter.testConnection(config);
        } finally {
            adapter.disconnect();
        }
    }
}
