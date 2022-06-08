/**
 * Created on 2019/6/7.
 */
package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.DefaultEncoderParser;

import java.util.function.Function;

/**
 * @author <a href="mailto:areyouok@gmail.com">huangli</a>
 */
public class JacksonEncoderParser extends DefaultEncoderParser {

    public static final String SERIAL_POLICY_JACKSON = "JACKSON";

    @Override
    public Function<Object, byte[]> parseEncoder(String valueEncoder) {

        if (SERIAL_POLICY_JACKSON.equalsIgnoreCase(valueEncoder)) {
            return new JacksonValueEncoder();
        } else {
            return super.parseEncoder(valueEncoder);
        }
    }

    @Override
    public Function<byte[], Object> parseDecoder(String valueDecoder) {

        if (SERIAL_POLICY_JACKSON.equalsIgnoreCase(valueDecoder)) {
            return new JacksonValueDecoder();
        } else {
            return super.parseDecoder(valueDecoder);
        }
    }
}
