package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.CacheContext;
import com.alicp.jetcache.anno.support.SpringConfigProvider;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

public class AutoInvalidateLocalConfigProvider extends SpringConfigProvider {
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        this.applicationContext = applicationContext;
    }


    @Override
    protected CacheContext newContext() {
        return new AutoInvalidateLocalCacheContext(this, globalCacheConfig, applicationContext);
    }
}
