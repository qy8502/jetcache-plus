package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.MultiLevelCacheConfig;
import com.alicp.jetcache.anno.method.SpringCacheContext;
import com.alicp.jetcache.anno.support.CachedAnnoConfig;
import com.alicp.jetcache.anno.support.GlobalCacheConfig;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import com.alicp.jetcache.redis.lettuce.JetCacheCodec;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCache;
import com.alicp.jetcache.redis.lettuce.RedisLettuceCacheConfig;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.push.PushMessage;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订阅本地缓存的删除事件
 */
public class AutoInvalidateLocalCacheContext extends SpringCacheContext {

    /**
     * 日志
     */
    private static final Logger logger = LoggerFactory.getLogger(AutoInvalidateLocalCacheContext.class);

    /**
     * 构造函数
     *
     * @param cacheManager       缓存管理器
     * @param configProvider     配置提供者
     * @param globalCacheConfig  全局配置
     * @param applicationContext 应用上下文
     */
    public AutoInvalidateLocalCacheContext(CacheManager cacheManager, SpringConfigProvider configProvider, GlobalCacheConfig globalCacheConfig, ApplicationContext applicationContext) {
        super(cacheManager, configProvider, globalCacheConfig, applicationContext);
    }

    static Map<AbstractRedisClient, AutoInvalidateLocalSubscription> subscriptionMap = new ConcurrentHashMap<>();


    @Override
    public Cache __createOrGetCache(CachedAnnoConfig cachedAnnoConfig, String area, String cacheName) {
        Cache cache = super.__createOrGetCache(cachedAnnoConfig, area, cacheName);
        if (cache.config() instanceof MultiLevelCacheConfig) {
            MultiLevelCacheConfig config = (MultiLevelCacheConfig) cache.config();
            if (config.getCaches().size() < 2 && !(config.getCaches().get(1) instanceof RedisLettuceCache)) {
                logger.error("AutoInvalidateLocalCacheContext {}-{}的remoteCache不是RedisLettuceCache，不支持自动失效本地缓存！", area, cacheName);
                return cache;
            }
            Cache local = (Cache) config.getCaches().get(0);
            RedisLettuceCache remote = (RedisLettuceCache) config.getCaches().get(1);
            RedisLettuceCacheConfig remoteConfig = (RedisLettuceCacheConfig) remote.config();
            // 每个redis配置创建一个订阅
            AutoInvalidateLocalSubscription subscription = subscriptionMap.computeIfAbsent(remoteConfig.getRedisClient(),
                    (key) -> new AutoInvalidateLocalSubscription(key));
            // 对应远端缓存与本地缓存关系
            String keyPrefix = subscription.addCacheTracking(remote, local);
            if (keyPrefix != null) {
                logger.debug("JETCACHE_PLUS_AUTO_INVALIDATE_LOCAL -> subscribe to remote cache '{}' for local cache '{}-{}' invalidation", keyPrefix, area, cacheName);
            }
        }
        return cache;
    }


    /**
     * 订阅本地缓存的删除事件
     */
    public class AutoInvalidateLocalListener implements PushListener {
        private String redisUri;
        private RedisCodec<?, ?> cacheCodec;
        private Map<String, Cache> localCacheMap;

        /**
         * 构造函数
         *
         * @param redisUri redis连接地址
         * @param cacheCodec 缓存编解码器
         * @param localCacheMap 本地缓存
         */
        public AutoInvalidateLocalListener(String redisUri, RedisCodec<?, ?> cacheCodec, Map<String, Cache> localCacheMap) {
            this.redisUri = redisUri;
            this.cacheCodec = cacheCodec;
            this.localCacheMap = localCacheMap;
        }

        /**
         * 处理消息
         * @param message 消息
         */
        @SuppressWarnings("unchecked")
        @Override
        public void onPushMessage(PushMessage message) {
            if (message.getType().equals("invalidate")) {
                List<Object> content = message.getContent(cacheCodec::decodeKey);
                List<byte[]> keys = (List<byte[]>) content.get(1);
                keys.forEach(keyBytes -> {
                    try {
                        String redisKey = new String(keyBytes, "UTF8");
                        localCacheMap.forEach((key, value) -> {
                            if (redisKey.startsWith(key)) {
                                String id = redisKey.substring(key.length());
                                boolean result = value.remove(id);
                                logger.debug("JETCACHE_PLUS_AUTO_INVALIDATE_LOCAL -> invalidate local cache by remote cache '{}' from {}: {}", redisKey, redisUri, result);
                            }
                        });
                    } catch (UnsupportedEncodingException e) {
                        logger.error("AutoInvalidateLocalCacheContext 解析缓存key发生异常", e);
                    }
                });
            }
        }
    }

    /**
     * 订阅本地缓存的删除事件
     */
    public class AutoInvalidateLocalSubscription {
        private Map<String, Cache> localCacheMap = new HashMap<>();
        private Map<StatefulRedisPubSubConnection, AutoInvalidateLocalListener> pubSubConnections = new HashMap<>();
        private AbstractRedisClient redisClient;
        private RedisCodec<?, ?> cacheCodec = new JetCacheCodec();

        /**
         * 构造函数
         *
         * @param redisClient Redis客户端
         */
        @SuppressWarnings("unchecked")
        public AutoInvalidateLocalSubscription(AbstractRedisClient redisClient) {
            this.redisClient = redisClient;
            try {
                if (redisClient instanceof RedisClient) {
                    this.pubSubConnections.computeIfAbsent((StatefulRedisPubSubConnection) ((RedisClient) redisClient).connectPubSub(cacheCodec),
                            (connection) -> createListener(connection, redisClient.toString()));
                } else if (redisClient instanceof RedisClusterClient) {
//                    StatefulRedisClusterPubSubConnection pubSubConnection = (StatefulRedisClusterPubSubConnection) ((RedisClusterClient) redisClient).connectPubSub(cacheCodec);
//                    pubSubConnection.setNodeMessagePropagation(true);
//                    this.pubSubConnections.add(pubSubConnection);

                    //clientTracking命令只能针对Redis集群的单个节点，需要所有节点的连接
                    Field initialUris = ReflectionUtils.findField(RedisClusterClient.class, "initialUris");
                    ReflectionUtils.makeAccessible(initialUris);
                    Iterable<RedisURI> uris = (Iterable<RedisURI>) initialUris.get(redisClient);
                    for (RedisClusterNode node : ((RedisClusterClient) redisClient).getPartitions()) {
                        if (node.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                            //找到原始uri，节点uri没有密码
                            RedisURI nodeUri = node.getUri();
                            for (RedisURI uri : uris) {
                                if (uri.getHost().equals(node.getUri().getHost()) && uri.getPort() == node.getUri().getPort()) {
                                    nodeUri = uri;
                                    break;
                                }
                            }
                            RedisURI finalNodeUri = nodeUri;
                            this.pubSubConnections.computeIfAbsent(RedisClient.create(nodeUri).connectPubSub(cacheCodec), (connection) -> createListener(connection, finalNodeUri.toString()));
                        }
                    }
                } else {
                    logger.error("AutoInvalidateLocalCacheContext remoteCache的redisClient类型 {} 不支持！", redisClient.getClass());
                    return;
                }
            } catch (Exception ex) {
                logger.error("AutoInvalidateLocalCacheContext 创建订阅连接发生异常", ex);
                return;
            }
        }

        private AutoInvalidateLocalListener createListener(StatefulRedisPubSubConnection connection, String redisUri) {
            AutoInvalidateLocalListener listener = new AutoInvalidateLocalListener(redisUri, cacheCodec, localCacheMap);
            connection.addListener(listener);
            return listener;
        }

        /**
         * 添加缓存跟踪
         *
         * @param remote 远程缓存
         * @param local  本地缓存
         * @return 缓存前缀
         */
        public synchronized String addCacheTracking(RedisLettuceCache remote, Cache local) {
            String keyPrefix = ((RedisLettuceCacheConfig) remote.config()).getKeyPrefix();
            if (localCacheMap.containsKey(keyPrefix)) {
                if (local != localCacheMap.get(keyPrefix)) {
                    logger.error("AutoInvalidateLocalCacheContext 已存在订阅的" + keyPrefix + "的本地缓存容器发生变动！");
                    localCacheMap.put(keyPrefix, local);
                }
            } else {
                localCacheMap.put(keyPrefix, local);
                this.pubSubConnections.keySet().forEach(conn -> {
                    try {
                        conn.sync().clientTracking(TrackingArgs.Builder.enabled(false));
                        conn.sync().clientTracking(TrackingArgs.Builder.enabled(true).prefixes(localCacheMap.keySet().toArray(new String[]{})).bcast().noloop());
                    } catch (Exception e) {
                        logger.error("AutoInvalidateLocalCacheContext 订阅的" + localCacheMap.keySet() + "的本地缓存clientTracking发生错误！", e);
                    }
                });
            }
            return keyPrefix;
        }

    }

}
