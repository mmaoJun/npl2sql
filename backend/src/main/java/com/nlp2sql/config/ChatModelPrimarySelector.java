package com.nlp2sql.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

import java.util.Map;

/**
 * ChatModel 主 Bean 选择器。
 *
 * <p>实现 {@link BeanDefinitionRegistryPostProcessor}，在容器启动早期根据
 * {@code nlp2sql.model.provider} 配置将对应的 ChatModel Bean 标记为 {@code @Primary}，
 * 解决 DashScope 和 Ollama 两个 Provider 同时存在时的 Bean 注入冲突。
 *
 * @see ModelProperties
 */
@org.springframework.context.annotation.Configuration
@Slf4j
public class ChatModelPrimarySelector implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private static final Map<String, String> PROVIDER_BEAN_MAP = Map.of(
            "dashscope", "dashscopeChatModel",
            "ollama", "ollamaChatModel"
    );

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    /**
     * 根据 provider 配置将对应 ChatModel Bean 标记为 Primary。
     *
     * <p>provider 与 Bean 名称映射：dashscope → dashscopeChatModel，ollama → ollamaChatModel。
     * 未找到对应 Bean 时仅打印警告，不中断启动。
     */
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        String provider = environment.getProperty("nlp2sql.model.provider", "dashscope");
        String primaryBeanName = PROVIDER_BEAN_MAP.get(provider);

        if (primaryBeanName == null) {
            log.warn("未知的模型 provider: {}, 默认使用 dashscope", provider);
            primaryBeanName = "dashscopeChatModel";
        }

        if (registry.containsBeanDefinition(primaryBeanName)) {
            registry.getBeanDefinition(primaryBeanName).setPrimary(true);
            log.info("已设置 {} 为主 ChatModel (provider={})", primaryBeanName, provider);
        } else {
            log.warn("未找到 ChatModel bean: {}, 请检查对应 provider 的配置是否完整", primaryBeanName);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }
}
