package com.nlp2sql.controller;

import com.nlp2sql.model.dto.ApiResponse;
import com.nlp2sql.model.dto.ChatRequest;
import com.nlp2sql.model.dto.ChatResponse;
import com.nlp2sql.service.NL2SQLEngine;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * NL2SQL 智能查询控制器。
 *
 * <p>接收用户自然语言输入，经由 {@link NL2SQLEngine} 完成 Schema 获取、Prompt 构建、
 * SQL 生成、安全校验、查询执行的全流程，返回结构化结果。</p>
 *
 * @see NL2SQLEngine
 * @see ChatRequest
 * @see ChatResponse
 */
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "智能查询", description = "自然语言转 SQL 并执行查询")
public class ChatController {

    private final NL2SQLEngine nl2SQLEngine;

    /**
     * 提交自然语言查询，返回 SQL 生成结果与查询数据。
     *
     * <p>核心流程：Schema 获取 → Prompt 构建 → SQL 生成 → 安全校验 → 执行查询 → 审计记录。
     * 需要携带 JWT Token 进行身份认证，匿名用户也可访问（username 为 null 时审计记录 userId 为空）。</p>
     *
     * @param request        聊天请求，包含自然语言消息和数据源 ID
     * @param authentication Spring Security 认证信息，由 JWT 过滤器注入
     * @return 包含生成 SQL、查询结果列/行、执行耗时等信息的响应体
     */
    @PostMapping
    @Operation(summary = "提交自然语言查询", description = "将自然语言转换为 SQL 并执行，返回查询结果")
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
                                           Authentication authentication) {
        String username = authentication != null ? authentication.getName() : null;
        ChatResponse response = nl2SQLEngine.processQuery(
                request.getMessage(), request.getDatasourceId(), username);
        return ApiResponse.ok(response);
    }
}
