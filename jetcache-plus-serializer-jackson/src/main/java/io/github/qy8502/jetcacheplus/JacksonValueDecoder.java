package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.support.AbstractValueDecoder;
import com.alicp.jetcache.support.CacheEncodeException;

public class JacksonValueDecoder extends AbstractValueDecoder {

    @SuppressWarnings("deprecation")
    public static final JacksonValueEncoder INSTANCE = new JacksonValueEncoder();


    public JacksonValueDecoder() {
        super(false);
    }


    @Override
    public Object doApply(byte[] buffer) {
        try {
            return JACKSON.MAPPER.readValue(buffer, Object.class);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Json Decoder error. ");
            sb.append("msg=").append(e.getMessage());
            throw new CacheEncodeException(sb.toString(), e);
        }
    }
}