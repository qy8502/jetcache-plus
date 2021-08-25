package com.github.qy8502.jetcacheplus;

import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.aop.CacheAdvisor;
import com.alicp.jetcache.anno.aop.CachePointcut;
import com.alicp.jetcache.anno.aop.JetCacheInterceptor;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.context.ApplicationContext;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;


public class JetCacheDubboProxyFactoryWrapper implements ProxyFactory {
    private static final long serialVersionUID = -3748439745014166871L;
    private static final Logger logger = LoggerFactory.getLogger(JetCacheDubboProxyFactoryWrapper.class);

    private final ProxyFactory proxyFactory;

    public JetCacheDubboProxyFactoryWrapper(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    public CacheAdvisor cacheAdvisor;
    public JetCacheInterceptor jetCacheInterceptor;
    public static ApplicationContext applicationContext;

    public static void setApplicationContext(ApplicationContext applicationContext) {
        JetCacheDubboProxyFactoryWrapper.applicationContext = applicationContext;
    }

    protected CacheAdvisor getCacheAdvisor() {
        if (cacheAdvisor == null) {
            cacheAdvisor = applicationContext.getBean(CacheAdvisor.class);
        }
        return cacheAdvisor;
    }

    protected CachePointcut getCachePointcut() {
        return (CachePointcut) getCacheAdvisor().getPointcut();
    }

    protected JetCacheInterceptor getJetCacheInterceptor() {
        if (jetCacheInterceptor == null) {
            jetCacheInterceptor = applicationContext.getBean(JetCacheInterceptor.class);
        }
        return jetCacheInterceptor;
    }

    @Override
    public <T> T getProxy(Invoker<T> invoker, boolean generic) throws RpcException {
        T dubboProxy = proxyFactory.getProxy(invoker, generic);
        CachePointcut cachePointcut = getCachePointcut();
        Class serviceInterface = invoker.getInterface();

        org.springframework.aop.framework.ProxyFactory defaultProxyFactory = new org.springframework.aop.framework.ProxyFactory();
        defaultProxyFactory.setInterfaces(serviceInterface);
        defaultProxyFactory.addAdvisor(new DefaultPointcutAdvisor(
                new StaticMethodMatcherPointcut() {
                    @Override
                    public boolean matches(Method method, Class<?> targetClass) {
                        return (method.isDefault() && method.getAnnotation(Cached.class) != null);
                    }
                },
                new MethodInterceptor() {
                    Map<Method, MethodHandle> methodHandleMap = new HashMap<>();
                    String interfaceName = serviceInterface.getSimpleName();

                    @Override
                    public Object invoke(MethodInvocation invocation) throws Throwable {
                        MethodHandle methodHandle = methodHandleMap.computeIfAbsent(invocation.getMethod(), method -> {
                            try {
                                Constructor<MethodHandles.Lookup> constructor = null;
                                constructor = MethodHandles.Lookup.class
                                        .getDeclaredConstructor(Class.class);
                                constructor.setAccessible(true);
                                return constructor.newInstance(method.getDeclaringClass())
                                        .in(method.getDeclaringClass())
                                        .unreflectSpecial(method, method.getDeclaringClass())
                                        .bindTo(dubboProxy);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        logger.debug("JETCACHE_PLUS_DUBBO -> invoke interface default method '{}.{}'", interfaceName, invocation.getMethod().getName());
                        return methodHandle.invokeWithArguments(invocation.getArguments());
                    }
                }));
        defaultProxyFactory.setTarget(dubboProxy);
        T defaultProxy = (T) defaultProxyFactory.getProxy();

        org.springframework.aop.framework.ProxyFactory cacheProxyFactory = new org.springframework.aop.framework.ProxyFactory();
        cacheProxyFactory.setInterfaces(serviceInterface);
        //cacheProxyFactory.addAdvice(getJetCacheInterceptor());
        cacheProxyFactory.addAdvisor(new DefaultPointcutAdvisor(
                new StaticMethodMatcherPointcut() {
                    @Override
                    public boolean matches(Method method, Class<?> targetClass) {
                        return cachePointcut.matches(method, targetClass) && method.getAnnotation(Cached.class) != null;
                    }
                },
                getJetCacheInterceptor()));
        cacheProxyFactory.setTarget(defaultProxy);
        T cacheProxy = (T) cacheProxyFactory.getProxy();
        return AopUtils.canApply(cachePointcut, cacheProxy.getClass()) ? cacheProxy : dubboProxy;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> T getProxy(Invoker<T> invoker) throws RpcException {
        return getProxy(invoker, false);
    }

    @Override
    public <T> Invoker<T> getInvoker(T proxy, Class<T> type, URL url) throws RpcException {
        return proxyFactory.getInvoker(proxy, type, url);
    }

}
