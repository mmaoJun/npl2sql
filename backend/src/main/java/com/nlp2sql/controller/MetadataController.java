package com.nlp2sql.controller;

import com.nlp2sql.model.ColumnInfo;
import com.nlp2sql.model.TableInfo;
import com.nlp2sql.model.dto.ApiResponse;
import com.nlp2sql.service.MetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 元数据浏览控制器。
 *
 * <p>提供数据源的表结构查询能力，包括表列表、字段详情、缓存刷新。
 * 元数据通过 Redis 缓存（默认 TTL 3600s），避免频繁查询 INFORMATION_SCHEMA。</p>
 *
 * @see MetadataService
 */
@RestController
@RequestMapping("/api/v1/metadata")
@RequiredArgsConstructor
@Tag(name = "元数据管理", description = "表结构查询与缓存刷新")
public class MetadataController {

    private final MetadataService metadataService;

    /**
     * 获取指定数据源的表列表（精简信息，不含字段详情）。
     *
     * @param datasourceId 数据源 ID
     * @return 表信息列表，每项包含表名和表注释
     */
    @GetMapping("/tables")
    @Operation(summary = "获取表列表", description = "查询指定数据源的所有表（精简信息）")
    public ApiResponse<List<TableInfo>> getTables(@RequestParam Long datasourceId) {
        return ApiResponse.ok(metadataService.getTables(datasourceId));
    }

    /**
     * 获取指定表的字段详情。
     *
     * @param datasourceId 数据源 ID
     * @param tableName    表名
     * @return 字段信息列表，包含列名、数据类型、注释、主键标识等
     */
    @GetMapping("/tables/{tableName}/columns")
    @Operation(summary = "获取字段列表", description = "查询指定表的字段详情")
    public ApiResponse<List<ColumnInfo>> getColumns(@RequestParam Long datasourceId,
                                                      @PathVariable String tableName) {
        return ApiResponse.ok(metadataService.getColumns(datasourceId, tableName));
    }

    /**
     * 手动刷新元数据缓存。
     *
     * <p>清除 Redis 中的 Schema 缓存和适配器连接缓存，重新从数据源拉取最新表结构。</p>
     *
     * @param datasourceId 数据源 ID
     * @return 空响应体
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新缓存", description = "清除元数据缓存并重新加载表结构")
    public ApiResponse<Void> refreshCache(@RequestParam Long datasourceId) {
        metadataService.refreshCache(datasourceId);
        return ApiResponse.ok();
    }
}
