package io.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.support.JetCacheBaseBeans;
import com.alicp.jetcache.anno.support.SpringConfigProvider;

/**
 * 自动创建缓存的bean
 */
public class AutoInvalidateLocalBeans extends JetCacheBaseBeans {

    /**
     * 构造函数
     */
    public AutoInvalidateLocalBeans() {
        super();
    }

    /**
     * 创建配置提供者
     *
     * @return 配置提供者
     */
    @Override
    protected SpringConfigProvider createConfigProvider() {
        return new AutoInvalidateLocalConfigProvider();
    }
}
