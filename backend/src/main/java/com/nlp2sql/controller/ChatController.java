package com.nlp2sql.controller;

import com.nlp2sql.mapper.DataSourceMapper;
import com.nlp2sql.mapper.UserMapper;
import com.nlp2sql.model.DataSource;
import com.nlp2sql.model.User;
import com.nlp2sql.model.dto.ChatRequest;
import com.nlp2sql.model.dto.ChatResponse;
import com.nlp2sql.service.NL2SQLEngine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final NL2SQLEngine nl2SQLEngine;
    private final DataSourceMapper dataSourceMapper;
    private final UserMapper userMapper;

    public ChatController(NL2SQLEngine nl2SQLEngine,
                           DataSourceMapper dataSourceMapper,
                           UserMapper userMapper) {
        this.nl2SQLEngine = nl2SQLEngine;
        this.dataSourceMapper = dataSourceMapper;
        this.userMapper = userMapper;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request,
                              Authentication authentication) {
        DataSource ds = dataSourceMapper.selectById(request.getDatasourceId());
        if (ds == null) {
            return ChatResponse.error(request.getMessage(), "数据源不存在");
        }

        Long userId = null;
        if (authentication != null) {
            User user = userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .eq(User::getUsername, authentication.getName()));
            if (user != null) {
                userId = user.getId();
            }
        }

        return nl2SQLEngine.processQuery(request.getMessage(), request.getDatasourceId(), ds, userId);
    }
}
