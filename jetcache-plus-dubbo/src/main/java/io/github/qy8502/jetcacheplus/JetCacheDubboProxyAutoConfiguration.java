package io.github.qy8502.jetcacheplus;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JetCacheDubboProxyAutoConfiguration implements ApplicationContextAware, BeanDefinitionRegistryPostProcessor {


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        JetCacheDubboProxyFactoryWrapper.setApplicationContext(applicationContext);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }
}