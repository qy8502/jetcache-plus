package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.support.CacheContext;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * 自动刷新本地缓存配置提供者
 */
public class AutoInvalidateLocalConfigProvider extends SpringConfigProvider {
    private ApplicationContext applicationContext;

    /**
     * 构造函数
     */
    public AutoInvalidateLocalConfigProvider() {
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        this.applicationContext = applicationContext;
    }


    @Override
    public CacheContext newContext(CacheManager cacheManager) {
        return new AutoInvalidateLocalCacheContext(cacheManager, this, globalCacheConfig, applicationContext);
    }
}
