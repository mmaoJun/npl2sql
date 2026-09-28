package com.nlp2sql.controller;

import com.nlp2sql.mapper.DataSourceMapper;
import com.nlp2sql.model.ColumnInfo;
import com.nlp2sql.model.DataSource;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.model.dto.ApiResponse;
import com.nlp2sql.service.MetadataService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/metadata")
public class MetadataController {

    private final MetadataService metadataService;
    private final DataSourceMapper dataSourceMapper;

    public MetadataController(MetadataService metadataService,
                               DataSourceMapper dataSourceMapper) {
        this.metadataService = metadataService;
        this.dataSourceMapper = dataSourceMapper;
    }

    @GetMapping("/tables")
    public ApiResponse<List<TableInfo>> getTables(@RequestParam Long datasourceId) {
        DataSource ds = dataSourceMapper.selectById(datasourceId);
        if (ds == null) {
            return ApiResponse.error(404, "数据源不存在");
        }
        List<TableInfo> tables = metadataService.getTables(datasourceId, ds);
        return ApiResponse.ok(tables);
    }

    @GetMapping("/tables/{tableName}/columns")
    public ApiResponse<List<ColumnInfo>> getColumns(@RequestParam Long datasourceId,
                                                      @PathVariable String tableName) {
        DataSource ds = dataSourceMapper.selectById(datasourceId);
        if (ds == null) {
            return ApiResponse.error(404, "数据源不存在");
        }
        List<ColumnInfo> columns = metadataService.getColumns(datasourceId, ds, tableName);
        return ApiResponse.ok(columns);
    }

    @PostMapping("/refresh")
    public ApiResponse<Void> refreshCache(@RequestParam Long datasourceId) {
        DataSource ds = dataSourceMapper.selectById(datasourceId);
        if (ds == null) {
            return ApiResponse.error(404, "数据源不存在");
        }
        metadataService.refreshCache(datasourceId, ds);
        return ApiResponse.ok();
    }
}
