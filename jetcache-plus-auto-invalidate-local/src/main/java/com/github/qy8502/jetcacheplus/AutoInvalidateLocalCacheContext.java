package com.github.qy8502.jetcacheplus;

import com.alicp.jetcache.Cache;
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
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnectionImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoInvalidateLocalCacheContext extends SpringCacheContext {

    private static final Logger logger = LoggerFactory.getLogger(AutoInvalidateLocalCacheContext.class);

    public class AutoInvalidateLocalSubscription {
        private Map<String, Cache> localCacheMap;
        private List<StatefulRedisPubSubConnectionImpl> pubSubConnections = new ArrayList<>();
        private AbstractRedisClient redisClient;
        private PushListener listener;
        private RedisCodec cacheCodec = new JetCacheCodec();

        public AutoInvalidateLocalSubscription(AbstractRedisClient redisClient) {
            this.redisClient = redisClient;
            this.localCacheMap = new ConcurrentHashMap<>();
            if (redisClient instanceof RedisClient) {
                this.pubSubConnections.add((StatefulRedisPubSubConnectionImpl) ((RedisClient) redisClient).connectPubSub(cacheCodec));
            } else if (redisClient instanceof RedisClusterClient) {
                //clientTracking命令只能针对Redis集群的单个节点，需要所有节点的连接
                ((RedisClusterClient) redisClient).getPartitions().forEach(node -> {
                    if (node.is(RedisClusterNode.NodeFlag.UPSTREAM)) {
                        this.pubSubConnections.add((StatefulRedisPubSubConnectionImpl) RedisClient.create(node.getUri()).connectPubSub(cacheCodec));
                    }
                });
            } else {
                logger.error("AutoInvalidateLocalCacheContext remoteCache的redisClient类型 {} 不支持！", redisClient.getClass());
                return;
            }
            this.listener = message -> {
                if (message.getType().equals("invalidate")) {
                    List<Object> content = message.getContent(cacheCodec::decodeKey);
                    List<byte[]> keys = (List<byte[]>) content.get(1);
                    keys.forEach(keyBytes -> {
                        try {
                            String redisKey = new String(keyBytes, "UTF8");
                            localCacheMap.entrySet().forEach(entry -> {
                                if (redisKey.startsWith(entry.getKey())) {
                                    String key = redisKey.substring(entry.getKey().length());
                                    boolean result = entry.getValue().remove(key);
                                    logger.debug("JETCACHE_PLUS_AUTO_INVALIDATE_LOCAL -> invalidate local cache by remote cache '{}': {}", redisKey, result);
                                }
                            });
                        } catch (UnsupportedEncodingException e) {
                            logger.error("AutoInvalidateLocalCacheContext 解析缓存key发生异常", e);
                        }
                    });
                }
            };
            this.pubSubConnections.forEach(conn -> conn.addListener(this.listener));
        }


        public String addCacheTracking(RedisLettuceCache remote, Cache local) {
            String keyPrefix = ((RedisLettuceCacheConfig) remote.config()).getKeyPrefix();
            localCacheMap.put(keyPrefix, local);
            this.pubSubConnections.forEach(conn -> {
                conn.sync().clientTracking(TrackingArgs.Builder.enabled(false));
                conn.sync().clientTracking(TrackingArgs.Builder.enabled(true).prefixes(localCacheMap.keySet().toArray(new String[]{})).bcast().noloop());
            });
            return keyPrefix;
        }

    }

    Map<AbstractRedisClient, AutoInvalidateLocalSubscription> subscriptionMap = new ConcurrentHashMap();


    public AutoInvalidateLocalCacheContext(SpringConfigProvider configProvider, GlobalCacheConfig globalCacheConfig, ApplicationContext applicationContext) {
        super(configProvider, globalCacheConfig, applicationContext);
    }

    @Override
    protected Cache buildCache(CachedAnnoConfig cachedAnnoConfig, String area, String cacheName) {
        Cache cache = super.buildCache(cachedAnnoConfig, area, cacheName);
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
            AutoInvalidateLocalSubscription subscription = subscriptionMap.computeIfAbsent(remoteConfig.getRedisClient(), (key) -> new AutoInvalidateLocalSubscription(key));
            // 对应远端缓存与本地缓存关系
            String keyPrefix = subscription.addCacheTracking(remote, local);
            if (keyPrefix != null) {
                logger.debug("JETCACHE_PLUS_AUTO_INVALIDATE_LOCAL -> subscribe to remote cache '{}' for local cache '{}-{}' invalidation", keyPrefix, area, cacheName);
            }
        }
        return cache;
    }
}
