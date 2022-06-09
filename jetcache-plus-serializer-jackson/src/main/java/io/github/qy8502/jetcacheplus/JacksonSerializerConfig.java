package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.SpringConfigProvider;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;

/**
 * 配置SpringConfigProvider的EncoderParser
 *
 * @author qy850
 */
@Configuration
public class JacksonSerializerConfig implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (SpringConfigProvider.class.isInstance(bean)) {
            ((SpringConfigProvider) bean).setEncoderParser(new JacksonEncoderParser());
        }
        return bean;
    }

}