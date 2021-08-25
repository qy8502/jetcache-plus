package com.github.qy8502.jetcacheplus;

import com.alicp.jetcache.*;
import com.alicp.jetcache.anno.method.CacheInvokeConfig;
import com.alicp.jetcache.anno.method.CacheInvokeContext;
import com.alicp.jetcache.anno.support.CacheContext;
import com.alicp.jetcache.anno.support.CacheInvalidateAnnoConfig;
import com.alicp.jetcache.anno.support.CacheUpdateAnnoConfig;
import com.alicp.jetcache.anno.support.CachedAnnoConfig;
import com.alicp.jetcache.event.CacheLoadAllEvent;
import com.alicp.jetcache.event.CacheLoadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiCacheHandler {
    private static Logger logger = LoggerFactory.getLogger(MultiCacheHandler.class);

    private static class CacheContextSupport extends CacheContext {

        public CacheContextSupport() {
            super(null, null);
        }

        static void _enable() {
            enable();
        }

        static void _disable() {
            disable();
        }

        static boolean _isEnabled() {
            return isEnabled();
        }
    }

    public static Object invoke(CacheInvokeContext context) throws Throwable {
        if (context.getCacheInvokeConfig().isEnableCacheContext()) {
            try {
                CacheContextSupport._enable();
                return doInvoke(context);
            } finally {
                CacheContextSupport._disable();
            }
        } else {
            return doInvoke(context);
        }
    }

    private static Object doInvoke(CacheInvokeContext context) throws Throwable {
        CacheInvokeConfig cic = context.getCacheInvokeConfig();
        CachedAnnoConfig cachedConfig = cic.getCachedAnnoConfig();
        if (cachedConfig != null && (cachedConfig.isEnabled() || CacheContextSupport._isEnabled())) {
            return invokeWithCached(context);
        } else if (cic.getInvalidateAnnoConfigs() != null || cic.getUpdateAnnoConfig() != null) {
            return invokeWithInvalidateOrUpdate(context);
        } else {
            return invokeOrigin(context);
        }
    }

    private static Object loadAndCount(CacheInvokeContext context, Cache cache, Object key) throws Throwable {
        long t = System.currentTimeMillis();
        Object v = null;
        boolean success = false;
        try {
            v = invokeOrigin(context);
            success = true;
        } finally {
            t = System.currentTimeMillis() - t;
            CacheLoadEvent event = new CacheLoadEvent(cache, t, key, v, success);
            while (cache instanceof ProxyCache) {
                cache = ((ProxyCache) cache).getTargetCache();
            }
            if (cache instanceof AbstractCache) {
                ((AbstractCache) cache).notify(event);
            }
        }
        return v;
    }

    private static Object invokeOrigin(CacheInvokeContext context) throws Throwable {
        return context.getInvoker().invoke();
    }

    private static Object invokeWithCached(CacheInvokeContext context)
            throws Throwable {
        CacheInvokeConfig cic = context.getCacheInvokeConfig();
        CachedAnnoConfig cac = cic.getCachedAnnoConfig();
        MultiCachedAnnoConfig mcac;
        if (!(cac instanceof MultiCachedAnnoConfig)) {
            mcac = new MultiCachedAnnoConfig(cac);
            cic.setCachedAnnoConfig(mcac);
        } else {
            mcac = (MultiCachedAnnoConfig) cac;
        }
        Cache cache = context.getCacheFunction().apply(context, cac);
        if (cache == null) {
            logger.error("no cache with name: " + context.getMethod());
            return invokeOrigin(context);
        }

        Object key = MultiExpressionUtil.evalKey(context, mcac);
        if (key == null) {
            return loadAndCount(context, cache, key);
        }

        try {
            if (key instanceof MultiCacheMap) {
                String methodName = String.format("%s.%s", context.getMethod().getDeclaringClass().getSimpleName(), context.getMethod().getName());

                MultiCacheMap mkey = ((MultiCacheMap) key);
                if (mkey.isEmpty()) {
                    return toMultiValues(mcac, mkey, Collections.emptyMap());
                }

                // 通过缓存获取
                Set<Object> keySet = new HashSet();
                // 如果有条件根据条件允许的key
                if (mkey.getConditions() != null) {
                    mkey.forEach((k, v) -> {
                        if (mkey.getCondition(k)) {
                            keySet.add(v);
                        }
                    });
                } else {
                    keySet.addAll(mkey.values());
                }
                logger.debug("JETCACHE_PLUS_MULTI -> {} get cache: {}", methodName, keySet);

                Map<Object, Object> cachedValues = cache.getAll(keySet);

                logger.debug("JETCACHE_PLUS_MULTI -> {} result from cache: {}", methodName, cachedValues.keySet());

                //如果数量一致，即全部走缓存
                if (cachedValues.size() == mkey.size()) {
                    logger.debug("JETCACHE_PLUS_MULTI -> {} final result: {}", methodName, cachedValues.keySet());
                    return toMultiValues(mcac, mkey, cachedValues);
                }

                Set loadKeys = new HashSet<>(mkey.values());
                loadKeys.removeAll(cachedValues.keySet());

                logger.debug("JETCACHE_PLUS_MULTI -> {} invoke method: {}", methodName, loadKeys);

                //修改参数为没有命中缓存的
                if (mkey.getMultiArgIndex() >= 0) {
                    Class argType = mcac.getDefineMethod().getParameterTypes()[mkey.getMultiArgIndex()];
                    if (argType.isArray()) {
                        context.getArgs()[mkey.getMultiArgIndex()] = loadKeys.stream().map(k -> mkey.getElement(k)).toArray();
                    } else if (argType.equals(List.class) || argType.equals(Collection.class)) {
                        context.getArgs()[mkey.getMultiArgIndex()] = loadKeys.stream().map(k -> mkey.getElement(k)).collect(Collectors.toList());
                    } else if (argType.equals(Set.class)) {
                        context.getArgs()[mkey.getMultiArgIndex()] = loadKeys.stream().map(k -> mkey.getElement(k)).collect(Collectors.toSet());
                    } else if (argType.isAssignableFrom(Collection.class)) {
                        Collection collection = (Collection) context.getArgs()[mkey.getMultiArgIndex()];
                        cachedValues.keySet().forEach(k -> collection.remove(mkey.getElement(k)));
                    } else {
                        logger.error("jetcache @MultiCached arg[" + mkey.getMultiArgIndex() + "] only be : Array, List, Set or Collection. Not support " + argType);
                    }
                }

                long t = System.currentTimeMillis();
                boolean success = false;
                Map resultMap = new HashMap();
                Map cacheMap = new HashMap();
                try {
                    Object result = invokeOrigin(context);
                    context.setResult(result);
                    if (result != null) {
                        Object postKey = MultiExpressionUtil.evalPostKey(context, mcac);
                        if (postKey instanceof MultiCacheMap) {
                            ((MultiCacheMap) postKey).forEach((k, v) -> {
                                resultMap.put(v, ((MultiCacheMap) postKey).getValue(k));
                                if (((MultiCacheMap) postKey).getCondition(k)) {
                                    cacheMap.put(v, ((MultiCacheMap) postKey).getValue(k));
                                }
                            });
                        } else if (postKey != null) {
                            resultMap.put(postKey, result);
                            if (MultiExpressionUtil.evalPostCondition(context, mcac)) {
                                cacheMap.putAll(resultMap);
                            }
                        }
                    }
                    success = true;
                } finally {
                    t = System.currentTimeMillis() - t;
                    CacheLoadAllEvent event = new CacheLoadAllEvent(cache, t, loadKeys, resultMap, success);
                    CacheUtil.getAbstractCache(cache).notify(event);
                }

                logger.debug("JETCACHE_PLUS_MULTI -> {} final result: {}", methodName, resultMap.keySet());

                if (cac.isCacheNullValue()) {
                    loadKeys.forEach(k -> {
                        if (!cacheMap.containsKey(k)) {
                            cacheMap.put(k, null);
                        }
                    });
                }

                cache.putAll(cacheMap);

                logger.debug("JETCACHE_PLUS_MULTI -> {} put cache: {}", methodName, cacheMap.keySet());

                cachedValues.putAll(resultMap);

                logger.debug("JETCACHE_PLUS_MULTI -> {} final result: {}", methodName, cachedValues.keySet());

                return toMultiValues(mcac, mkey, cachedValues);

            } else {

                if (!MultiExpressionUtil.evalCondition(context, mcac)) {
                    return loadAndCount(context, cache, key);
                }


                CacheLoader loader = new CacheLoader() {
                    @Override
                    public Object load(Object k) throws Throwable {
                        Object result = invokeOrigin(context);
                        context.setResult(result);
                        return result;
                    }

                    @Override
                    public boolean vetoCacheUpdate() {
                        return !MultiExpressionUtil.evalPostCondition(context, cic.getCachedAnnoConfig());
                    }
                };

                Object result = cache.computeIfAbsent(key, loader);
                return result;
            }
        } catch (CacheInvokeException e) {
            throw e.getCause();
        }
    }

    private static Object toMultiValues(MultiCachedAnnoConfig mcac, MultiCacheMap<Object, Object> mkey, Map<Object, Object> cachedValues) {
        Class returnType = mcac.getDefineMethod().getReturnType();
        Stream<Object> result = mkey.entrySet().stream().map(entry -> cachedValues.get(entry.getValue())).filter(Objects::nonNull);
        if (returnType.isArray()) {
            return result.toArray();
        } else if (returnType.equals(List.class) || returnType.equals(Collection.class)) {
            return result.collect(Collectors.toList());
        } else if (returnType.equals(Set.class)) {
            return result.collect(Collectors.toSet());
        } else if (returnType.equals(Map.class)) {
            return mkey.entrySet().stream().collect(HashMap::new, (m, entry) -> m.put(entry.getKey(), cachedValues.get(entry.getValue())), HashMap::putAll);
        } else {
            logger.error("jetcache @MultiCached returnType only be : List, Set, Collection or Map. Not support " + returnType);
            return null;
        }
    }


    private static Object invokeWithInvalidateOrUpdate(CacheInvokeContext context) throws Throwable {
        Object originResult = invokeOrigin(context);
        context.setResult(originResult);
        CacheInvokeConfig cic = context.getCacheInvokeConfig();

        if (cic.getInvalidateAnnoConfigs() != null) {
            doInvalidate(context, cic.getInvalidateAnnoConfigs());
        }
        CacheUpdateAnnoConfig updateAnnoConfig = cic.getUpdateAnnoConfig();
        if (updateAnnoConfig != null) {
            doUpdate(context, updateAnnoConfig);
        }

        return originResult;
    }


    private static void doInvalidate(CacheInvokeContext context, List<CacheInvalidateAnnoConfig> annoConfig) {
        for (CacheInvalidateAnnoConfig config : annoConfig) {
            doInvalidate(context, config);
        }
    }

    private static void doInvalidate(CacheInvokeContext context, CacheInvalidateAnnoConfig annoConfig) {
        Cache cache = context.getCacheFunction().apply(context, annoConfig);
        if (cache == null) {
            return;
        }
        Object key = MultiExpressionUtil.evalKey(context, annoConfig);
        if (key == null) {
            return;
        }
        if (key instanceof MultiCacheMap) {
            Set keys = new HashSet();
            MultiCacheMap<Object, Object> multi = ((MultiCacheMap) key);
            multi.entrySet().forEach(entry -> {
                if (multi.getCondition(entry.getKey())) {
                    keys.add(entry.getValue());
                }
            });
            cache.removeAll(keys);

        } else {
            boolean condition = MultiExpressionUtil.evalCondition(context, annoConfig);
            if (!condition) {
                return;
            }

            if (annoConfig.isMulti()) {
                Iterable it = MultiCacheHandler.toIterable(key);
                if (it == null) {
                    logger.error("jetcache @CacheInvalidate key is not instance of Iterable or array: " + annoConfig.getDefineMethod());
                    return;
                }
                Set keys = new HashSet();
                it.forEach(k -> keys.add(k));
                cache.removeAll(keys);
            } else {
                cache.remove(key);
            }
        }
    }

    private static void doUpdate(CacheInvokeContext context, CacheUpdateAnnoConfig updateAnnoConfig) {
        Cache cache = context.getCacheFunction().apply(context, updateAnnoConfig);
        if (cache == null) {
            return;
        }
        boolean condition = MultiExpressionUtil.evalCondition(context, updateAnnoConfig);
        if (!condition) {
            return;
        }
        Object key = MultiExpressionUtil.evalKey(context, updateAnnoConfig);
        Object value = MultiExpressionUtil.evalValue(context, updateAnnoConfig);
        if (key == null || value == MultiExpressionUtil.EVAL_FAILED) {
            return;
        }
        if (updateAnnoConfig.isMulti()) {
            if (value == null) {
                return;
            }
            Iterable keyIt = MultiCacheHandler.toIterable(key);
            Iterable valueIt = MultiCacheHandler.toIterable(value);
            if (keyIt == null) {
                logger.error("jetcache @CacheUpdate key is not instance of Iterable or array: " + updateAnnoConfig.getDefineMethod());
                return;
            }
            if (valueIt == null) {
                logger.error("jetcache @CacheUpdate value is not instance of Iterable or array: " + updateAnnoConfig.getDefineMethod());
                return;
            }

            List keyList = new ArrayList();
            List valueList = new ArrayList();
            keyIt.forEach(o -> keyList.add(o));
            valueIt.forEach(o -> valueList.add(o));
            if (keyList.size() != valueList.size()) {
                logger.error("jetcache @CacheUpdate key size not equals with value size: " + updateAnnoConfig.getDefineMethod());
                return;
            } else {
                Map m = new HashMap();
                for (int i = 0; i < valueList.size(); i++) {
                    m.put(keyList.get(i), valueList.get(i));
                }
                cache.putAll(m);
            }
        } else {
            cache.put(key, value);
        }
    }


    public static Iterable toIterable(Object obj) {
        if (obj.getClass().isArray()) {
            if (obj instanceof Object[]) {
                return Arrays.asList((Object[]) obj);
            } else {
                List list = new ArrayList();
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++) {
                    list.add(Array.get(obj, i));
                }
                return list;
            }
        } else if (obj instanceof Map) {
            return ((Map) obj).entrySet();
        } else if (obj instanceof Iterable) {
            return (Iterable) obj;
        } else {
            return null;
        }
    }

}
