package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.*;
import com.alicp.jetcache.autoconfigure.JetCacheAutoConfiguration;
import com.alicp.jetcache.support.StatInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * 配置自动失效本地缓存
 * 在JetCacheAutoConfiguration之前配置
 */
@Configuration
@AutoConfigureBefore(JetCacheAutoConfiguration.class)
public class AutoInvalidateLocalAutoConfiguration {

    /**
     * 构造函数
     */
    public AutoInvalidateLocalAutoConfiguration() {
    }


    /**
     * 配置自动失效本地缓存
     *
     * @param applicationContext 上下文
     * @param globalCacheConfig  全局配置
     * @param encoderParser 编码解码器
     * @param keyConvertorParser key转换器
     * @param metricsCallback 监控回调
     * @return SpringConfigProvider
     */
    @Bean(destroyMethod = "shutdown")
    public SpringConfigProvider springConfigProvider(
            @Autowired ApplicationContext applicationContext,
            @Autowired GlobalCacheConfig globalCacheConfig,
            @Autowired(required = false) EncoderParser encoderParser,
            @Autowired(required = false) KeyConvertorParser keyConvertorParser,
            @Autowired(required = false) Consumer<StatInfo> metricsCallback) {
        return new AutoInvalidateLocalBeans().springConfigProvider(applicationContext, globalCacheConfig,
                encoderParser, keyConvertorParser, metricsCallback);
    }

}
