package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.support.AbstractValueEncoder;
import com.alicp.jetcache.support.CacheEncodeException;

/**
 * @author qy850
 */
public class JacksonValueEncoder extends AbstractValueEncoder {

    @SuppressWarnings("deprecation")
    public static final JacksonValueEncoder INSTANCE = new JacksonValueEncoder();

    public JacksonValueEncoder() {
        super(false);
    }

    @Override
    public byte[] apply(Object value) {
        try {
            return JACKSON.MAPPER.writeValueAsBytes(value);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Json Encode error. ");
            sb.append("msg=").append(e.getMessage());
            throw new CacheEncodeException(sb.toString(), e);
        }
    }
}