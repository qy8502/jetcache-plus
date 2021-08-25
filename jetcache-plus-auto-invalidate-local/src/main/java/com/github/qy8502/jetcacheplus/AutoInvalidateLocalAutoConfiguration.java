package com.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.SpringConfigProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AutoInvalidateLocalAutoConfiguration {


    @Bean
    public SpringConfigProvider springConfigProvider() {
        return new AutoInvalidateLocalConfigProvider();
    }

}
