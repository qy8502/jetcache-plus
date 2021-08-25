package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.aop.JetCacheInterceptor;
import com.alicp.jetcache.anno.config.JetCacheProxyConfiguration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

@Configuration
@AutoConfigureBefore(JetCacheProxyConfiguration.class)
public class MultiJetCacheProxyAutoConfiguration {

    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    public AutoMatchJetCacheInterceptorInjector autoMatchJetCacheInterceptorInjector() {
        return new AutoMatchJetCacheInterceptorInjector();
    }

    public class AutoMatchJetCacheInterceptorInjector implements BeanFactoryPostProcessor {

        @Override
        public void postProcessBeanFactory(final ConfigurableListableBeanFactory factory) throws BeansException {
            String[] names = factory.getBeanNamesForType(JetCacheInterceptor.class);
            for (String name : names) {
                BeanDefinition bd = factory.getBeanDefinition(name);
                bd.setBeanClassName(MultiJetCacheInterceptor.class.getName());
                bd.setFactoryBeanName(null);
                bd.setFactoryMethodName(null);
            }
        }
    }

}
