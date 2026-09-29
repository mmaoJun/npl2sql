package com.nlp2sql.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型配置属性。
 *
 * <p>绑定 {@code nlp2sql.model.*} 配置项，包含当前 provider 名称、
 * DashScope 模型名和 Ollama 模型名。
 *
 * @see ChatModelPrimarySelector
 */
@Data
@Component
@ConfigurationProperties(prefix = "nlp2sql.model")
public class ModelProperties {

    /** 当前激活的模型 provider：dashscope 或 ollama */
    private String provider = "dashscope";
    /** DashScope 模型名称 */
    private String dashscopeModel = "qwen-plus";
    /** Ollama 模型名称 */
    private String ollamaModel = "qwen2.5-coder:7b";

    /**
     * 获取当前 provider 对应的模型名称。
     *
     * @return 激活的模型名称
     */
    public String getActiveModelName() {
        return "dashscope".equals(provider) ? dashscopeModel : ollamaModel;
    }
}
