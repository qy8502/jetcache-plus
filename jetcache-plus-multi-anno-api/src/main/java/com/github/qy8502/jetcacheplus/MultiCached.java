package com.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.CacheConsts;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MultiCached {


    /**
     * If evaluated key is an array or an instance of java.lang.Iterable,
     * set multi to true indicates jetcache to invalidate each element of the iterable keys.
     */
    boolean multi() default MultiCacheConsts.DEFAULT_MULTI;

    /**
     * Specify the key by expression script, optional. If not specified,
     * use all parameters of the target method and keyConvertor to generate one.
     *
     * @return an expression script which specifies key
     */
    String postKey() default CacheConsts.UNDEFINED_STRING;

    /**
     * Specify the cache value by expression script.
     *
     * @return an expression script which specifies cache value
     */
    String value() default "#" +MultiCacheConsts.RESULT + "[" + MultiCacheConsts.EACH_ELEMENT + "]";

}
