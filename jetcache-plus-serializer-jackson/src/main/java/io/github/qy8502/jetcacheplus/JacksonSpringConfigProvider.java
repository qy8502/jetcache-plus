package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.SpringConfigProvider;

public class JacksonSpringConfigProvider extends SpringConfigProvider {
    public JacksonSpringConfigProvider() {
        super();
        encoderParser = new JacksonEncoderParser();
    }
}
