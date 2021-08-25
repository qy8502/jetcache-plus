package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.CacheConsts;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.CacheAnnoConfig;
import com.alicp.jetcache.anno.support.CacheUpdateAnnoConfig;
import com.alicp.jetcache.anno.support.CachedAnnoConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.function.Function;

public class MultiExpressionUtil {

    private static final Logger logger = LoggerFactory.getLogger(MultiExpressionUtil.class);

    static Object EVAL_FAILED = new Object();

    public static boolean evalCondition(CacheInvokeContext context, CacheAnnoConfig cac) {
        String condition = cac.getCondition();
        try {
            if (cac.getConditionEvaluator() == null) {
                if (CacheConsts.isUndefined(condition)) {
                    cac.setConditionEvaluator(o -> true);
                } else {
                    MultiExpressionEvaluator e = new MultiExpressionEvaluator(condition, cac.getDefineMethod());
                    cac.setConditionEvaluator((o) -> e.isMulti() || (Boolean) e.apply(o));
                }
            }
            return cac.getConditionEvaluator().apply(context);
        } catch (Exception e) {
            logger.error("error occurs when eval condition \"" + condition + "\" in " + context.getMethod() + ":" + e.getMessage(), e);
            return false;
        }
    }

    public static boolean evalPostCondition(CacheInvokeContext context, CachedAnnoConfig cac) {
        String postCondition = cac.getPostCondition();
        try {
            if (cac.getPostConditionEvaluator() == null) {
                if (CacheConsts.isUndefined(postCondition)) {
                    cac.setPostConditionEvaluator(o -> true);
                } else {
                    MultiExpressionEvaluator e = new MultiExpressionEvaluator(postCondition, cac.getDefineMethod());
                    cac.setPostConditionEvaluator((o) -> e.isMulti() || (Boolean) e.apply(o));
                }
            }
            return cac.getPostConditionEvaluator().apply(context);
        } catch (Exception e) {
            logger.error("error occurs when eval postCondition \"" + postCondition + "\" in " + context.getMethod() + ":" + e.getMessage(), e);
            return false;
        }
    }

    public static Object evalKey(CacheInvokeContext context, CacheAnnoConfig cac) {
        String keyScript = cac.getKey();
        try {
            if (cac.getKeyEvaluator() == null) {
                if (CacheConsts.isUndefined(keyScript)) {
                    cac.setKeyEvaluator(o -> {
                        CacheInvokeContext c = (CacheInvokeContext) o;
                        return c.getArgs() == null ? "_$JETCACHE_NULL_KEY$_" : c.getArgs();
                    });
                } else {
                    MultiExpressionEvaluator e = new MultiExpressionEvaluator(keyScript, cac.getDefineMethod());
                    if (e.isMulti()) {
                        String condition = cac.getCondition();
                        Function<Object, Object> ec;
                        if (CacheConsts.isUndefined(condition)) {
                            ec = (o -> null);
                        } else {
                            MultiExpressionEvaluator mee = new MultiExpressionEvaluator(condition, cac.getDefineMethod());
                            if (mee.isMulti()) {
                                ec = mee;
                            } else {
                                ec = (o) -> MultiCacheMap.defaultValueMap(mee.apply(o));

                            }
                        }

                        cac.setKeyEvaluator((o) -> {
                            MultiCacheMap map = (MultiCacheMap) e.apply(o);
                            map.setConditions((MultiCacheMap) ec.apply(o));
                            return map;
                        });
                    } else {
                        cac.setKeyEvaluator((o) -> e.apply(o));
                    }
                }
            }
            return cac.getKeyEvaluator().apply(context);
        } catch (Exception e) {
            logger.error("error occurs when eval key \"" + keyScript + "\" in " + context.getMethod() + ":" + e.getMessage(), e);
            return null;
        }
    }


    public static Object evalPostKey(CacheInvokeContext context, MultiCachedAnnoConfig cac) {
        String keyScript = cac.getPostKey();
        try {
            if (cac.getPostKeyEvaluator() == null) {
                if (CacheConsts.isUndefined(keyScript)) {
                    cac.setPostKeyEvaluator(o -> {
                        CacheInvokeContext c = (CacheInvokeContext) o;
                        return c.getArgs() == null ? "_$JETCACHE_NULL_KEY$_" : c.getArgs();
                    });
                } else {
                    MultiExpressionEvaluator e = new MultiExpressionEvaluator(keyScript, cac.getDefineMethod());
                    if (e.isMulti()) {
                        String condition = cac.getPostCondition();
                        Function<Object, Object> ec;
                        if (CacheConsts.isUndefined(condition)) {
                            ec = (o -> null);
                        } else {
                            MultiExpressionEvaluator mee = new MultiExpressionEvaluator(condition, cac.getDefineMethod());
                            if (mee.isMulti()) {
                                ec = mee;
                            } else {
                                ec = (o) -> MultiCacheMap.defaultValueMap(mee.apply(o));

                            }
                        }

                        String valueScript = cac.getValue();
                        Function<Object, Object> ev;
                        if (CacheConsts.isUndefined(valueScript) || StringUtils.isEmpty(valueScript)) {
                            ev = (o) -> null;
                        } else {
                            MultiExpressionEvaluator mee = new MultiExpressionEvaluator(valueScript, cac.getDefineMethod());
                            if (mee.isMulti()) {
                                ev = mee;
                            } else {
                                ev  = (o) -> MultiCacheMap.defaultValueMap(mee.apply(o));
                            }
                        }
                        cac.setPostKeyEvaluator((o) -> {
                            MultiCacheMap map = (MultiCacheMap) e.apply(o);
                            map.setConditions((MultiCacheMap) ec.apply(o));
                            map.setValues((MultiCacheMap) ev.apply(o));
                            return map;
                        });
                    } else {
                        cac.setPostKeyEvaluator((o) -> e.apply(o));
                    }
                }
            }
            return cac.getPostKeyEvaluator().apply(context);
        } catch (Exception e) {
            logger.error("error occurs when eval post key \"" + keyScript + "\" in " + context.getMethod() + ":" + e.getMessage(), e);
            return null;
        }

    }

    public static Object evalValue(CacheInvokeContext context, CacheUpdateAnnoConfig cac) {
        String valueScript = cac.getValue();
        try {
            if (cac.getValueEvaluator() == null) {
                MultiExpressionEvaluator e = new MultiExpressionEvaluator(valueScript, cac.getDefineMethod());
                cac.setValueEvaluator((o) -> e.apply(o));
            }
            return cac.getValueEvaluator().apply(context);
        } catch (Exception e) {
            logger.error("error occurs when eval value \"" + valueScript + "\" in " + context.getMethod() + ":" + e.getMessage(), e);
            return EVAL_FAILED;
        }
    }

//    public static Object evalValue(CacheInvokeContext context, CachedAnnoConfig cac, MultiCachedAnnoConfig mcac) {
//        String valueScript = mcac.getValue();
//        try {
//            if (mcac.getValueEvaluator() == null) {
//                MultiExpressionEvaluator e = new MultiExpressionEvaluator(valueScript, cac.getDefineMethod());
//                mcac.setValueEvaluator((o) -> e.apply(o));
//            }
//            return mcac.getValueEvaluator().apply(context);
//        } catch (Exception e) {
//            logger.error("error occurs when eval post value \"" + valueScript + "\" in " + context.getMethod() + ":" + e.getMessage(), e);
//            return EVAL_FAILED;
//        }
//    }
}
