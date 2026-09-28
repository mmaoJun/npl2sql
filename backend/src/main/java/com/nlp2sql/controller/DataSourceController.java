package com.nlp2sql.controller;

import com.nlp2sql.mapper.DataSourceMapper;
import com.nlp2sql.model.DataSource;
import com.nlp2sql.model.dto.ApiResponse;
import com.nlp2sql.model.dto.DataSourceDTO;
import com.nlp2sql.adapter.DataSourceFactory;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/datasources")
public class DataSourceController {

    private final DataSourceMapper dataSourceMapper;
    private final DataSourceFactory dataSourceFactory;

    public DataSourceController(DataSourceMapper dataSourceMapper,
                                 DataSourceFactory dataSourceFactory) {
        this.dataSourceMapper = dataSourceMapper;
        this.dataSourceFactory = dataSourceFactory;
    }

    @PostMapping
    public ApiResponse<DataSource> create(@Valid @RequestBody DataSourceDTO dto) {
        DataSource ds = new DataSource();
        ds.setName(dto.getName());
        ds.setType(dto.getType());
        ds.setHost(dto.getHost());
        ds.setPort(dto.getPort());
        ds.setDatabaseName(dto.getDatabaseName());
        ds.setUsername(dto.getUsername());
        ds.setPassword(dto.getPassword());
        ds.setExtraConfig(dto.getExtraConfig());
        dataSourceMapper.insert(ds);
        return ApiResponse.ok(ds);
    }

    @GetMapping
    public ApiResponse<List<DataSource>> list() {
        List<DataSource> list = dataSourceMapper.selectList(null);
        // 脱敏：不返回密码
        list.forEach(ds -> ds.setPassword("******"));
        return ApiResponse.ok(list);
    }

    @PutMapping("/{id}")
    public ApiResponse<DataSource> update(@PathVariable Long id,
                                           @Valid @RequestBody DataSourceDTO dto) {
        DataSource ds = dataSourceMapper.selectById(id);
        if (ds == null) {
            return ApiResponse.error(404, "数据源不存在");
        }
        ds.setName(dto.getName());
        ds.setType(dto.getType());
        ds.setHost(dto.getHost());
        ds.setPort(dto.getPort());
        ds.setDatabaseName(dto.getDatabaseName());
        ds.setUsername(dto.getUsername());
        if (!"******".equals(dto.getPassword())) {
            ds.setPassword(dto.getPassword());
        }
        ds.setExtraConfig(dto.getExtraConfig());
        dataSourceMapper.updateById(ds);
        dataSourceFactory.removeAdapter(id);
        return ApiResponse.ok(ds);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        dataSourceMapper.deleteById(id);
        dataSourceFactory.removeAdapter(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/test")
    public ApiResponse<Boolean> testConnection(@PathVariable Long id) {
        DataSource ds = dataSourceMapper.selectById(id);
        if (ds == null) {
            return ApiResponse.error(404, "数据源不存在");
        }
        Map<String, Object> config = new HashMap<>();
        config.put("host", ds.getHost());
        config.put("port", ds.getPort());
        config.put("databaseName", ds.getDatabaseName());
        config.put("username", ds.getUsername());
        config.put("password", ds.getPassword());
        boolean success = dataSourceFactory.testConnection(ds.getType(), config);
        return success ? ApiResponse.ok(true) : ApiResponse.error("连接失败");
    }
}
