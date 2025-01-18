package io.github.qy8502.jetcacheplus;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置SpringConfigProvider的EncoderParser
 *
 * @author qy850
 */
@Configuration
public class JacksonSerializerConfig {

    @Bean
    public JacksonEncoderParser jacksonEncoderParser() {
        return new JacksonEncoderParser();
    }
}
