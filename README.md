![example workflow](https://github.com/qy8502/jetcache-plus/actions/workflows/main.yml/badge.svg)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.qy8502/jetcache-plus-dubbo.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.qy8502/jetcache-plus/)
![GitHub license](https://img.shields.io/github/license/qy8502/jetcache-plus.svg?style=flat-square)

# JetCache Plus

[alibaba jetcache](https://github.com/alibaba/jetcache/wiki/Home_CN) 提供了一套相对完善的java缓存方案。
但是在分布式和微服务应用时，仍然有一些需求没有实现，包括：
* 在使用两级缓存时不能同步失效分布式服务下的本地缓存。
* `@Cached`注解不能提供获取多个缓存集合，而`@CacheInvalidate`和`@CacheUpdate`有multi模式却无法通过SPEL拼写集合中每个key。
* 在使用基于dubbo的微服务框架中，服务消费者无法先调用缓存再调用RCP，以提高效率。

jetcache-plus作为jetcache的功能增强插件，提供了解决这些问题的功能。

---------------------------------------------

<p>
    
# 1. 本地缓存自动失效
## 1.1. 背景
jetcache支持本地缓存和二级缓存。但是在分布式部署时，哪怕本地缓存设置过去时间很短，一样会存在数据是过时或者不同节点数据不一致的情况。如果本地缓存可以和redis分布式缓存同步失效，将可以极大提高本地缓存的应用效果。

## 1.2. 实现方式
利用redis 6 新特性 client tracking 实现，参考：
<br>
[Redis server-assisted client side caching](https://redis.io/commands/client-tracking)
<br>
[Redis 6.0 客户端缓存特性及实践](https://colobu.com/2020/05/02/redis-client-side-caching/)
<br>
注意：仅针对 redis 6 服务端，和 lettuce 6 作为 redis 客户端。只针对 cacheType = CacheType.BOTH 的两级缓存

通过实现 `LocalCacheAutoInvalidateCacheContext` 继承 `SpringCacheContext` 重写 `buildCache` 方法，再创建缓存后，通过配置 `clientTracking` 和添加监听，使得 redis 缓存的前缀与本地缓存建立失效清除的关系。
`SpringConfigProvider` 为 spring bean , 通过实现 `LocalCacheAutoInvalidateConfigProvider` 继承 `SpringConfigProvider` 重新 `newContext` 方法替换 `CacheContext` 为 `LocalCacheAutoInvalidateCacheContext` 的实例 。
通过 `LocalCacheAutoInvalidateAutoConfiguration` 申明
<br>
注意：由于通过redis key前缀匹配关系，所以cache的name应当包含特殊的终止符号，如：`School:`与`SchoolDetails:`可以区分开来。

目前lettuce的`clientTracking`方法不支持redis集群，订阅过程中对每个主节点都执行了`clientTracking`并订阅。
`clientTracking`使用的BCAST模式，将针对缓存前缀进行跟踪。

## 1.3. 使用说明

build.gradle文件引入依赖，使用 redis-lettuce 且排除 lettuce 因为其版本不支持 clientTracking。引入lettuce-core 6.x。
```groovy
    implementation 'io.github.qy8502:jetcache-plus-auto-invalidate-local:0.0.1'
    implementation('com.alicp.jetcache:jetcache-starter-redis-lettuce:2.6.0'){
        exclude group: 'io.lettuce'
    }
    implementation 'io.lettuce:lettuce-core:6.1.4.RELEASE'
```

    
<p>

    
# 2. 多个缓存注解支持
## 2.1. 背景
jetcache 提供的CacheAPI中是支持`putAll`和`getAll`处理多个缓存的。这在使用redis缓存时可以有效的减少请求数量。
但是通过`@Cached`注解不支持缓存集合的处理。实现这一个功能的难点在于不方便通过SPEL拼接每一个缓存的key，这需要为表达式解析提供一个新规则指定可以迭代的集合。

## 2.2. 实现方式
处理注解缓存基本通过JetCacheInterceptor拦截由CacheHandler处理。
通过`MultiJetCacheProxyAutoConfiguration`和`BeanFactoryPostProcessor.postProcessBeanFactory`将`JetCacheInterceptor`替换为`MultiJetCacheInterceptor`，改为使用`MultiCacheHandler`处理缓存。
`MultiExpressionEvaluator`代替`ExpressionEvaluator`处理El表达式, 定义`$$each$$`标记，使之可以对集合参数或返回值迭代解析成key或value的集合。
如： `#schools[$$each$$].id` 或者 `#result[$$each$$].value`

`MultiCacheHandler`处理多个缓存机制：
1. 将根据方法参数集合解析出keys集合。
2. 通过`Cache.getAll`得到命中的缓存。
3. 比对出未命中的缓存修改方法参数调用方法。 
4. 通过`Cache.putAll`将未命中缓存的方法返回值进行缓存。
5. 将命中缓存和未命中缓存的结果合并返回。
```text
Arg:ids         Cache.getAll    InvokeMethod    Cache.putAll    Result
┌────────┐      ┌────────┐      ┌────────┐      ┌────────┐      ┌────────┐
│ S1     │      │ S1     │      │ S2     │      │ S2     │      │ S1     │
│        │      │        │      │        │      │        │      │        │
│ S2     │ ───► │ S3     │ ───► │ S4     │ ───► │ S4     │ ───► │ S2     │
│        │      │        │      │        │      │        │      │        │
│ S3     │      │        │      │        │      │        │      │ S3     │
│        │      │        │      │        │      │        │      │        │
│ S4     │      │        │      │        │      │        │      │ S4     │
└────────┘      └────────┘      └────────┘      └────────┘      └────────┘

```

注意：由于方法调用时，需要改造参数和返回值集合，所以使用$$each$$标记的参数或返回值必须是`Set`/`List`/`Map`/`Collection`接口，方便缓存处理时实例化默认对象替换原有参数或返回值。
<br>另外，因为经过缓存处理的返回值是经过key排重的，参数`[S1,S2,S1]`为参数返回的集合为`[S1,S2]`。
<br>同时因为方法调用返回结果需要与缓存结果拼装，方法调用返回null，处理后的结果为空集合。
<br>如果方法调用返回结果集合中某一项为null，由于无法解析key，该项会被舍去，如参数`[S1,S0]`实际调用返回`[S1,null]`，最终返回`[S1]`
<br>支持Map返回值，这种形式的方法要求，返回Map的key必须是传入集合参数的项,其value允许为null，如参数`[S1,S0]`最终返回`[S1:S1,S0:null]`

多个缓存返回结果为集合，需要根据返回值对象解析多个keys，无法依赖传入参数解析的keys，需要对`@Cached`注解进行扩展。
所以增加了`@MultiCached`注解，提供`postKey`和`value`用于根据返回值解析存入缓存的单个key和value。

## 2.3. 使用说明
build.gradle文件引入依赖，`@MultiCached`注解可能为项目接口使用，单独一个引用。<br>
服务接口层
```groovy
    implementation 'io.github.qy8502:jetcache-plus-multi-anno-api:0.0.1'
    implementation 'com.alicp.jetcache:jetcache-anno:2.6.0'
```
服务实现层
```groovy
    implementation 'io.github.qy8502:jetcache-plus-multi:0.0.1'
    implementation('com.alicp.jetcache:jetcache-starter-redis-lettuce:2.6.0')
```

多个缓存注解使用
```java

public interface SchoolService {

    /**
     * 获取单个学校对象（缓存）
     * @param id 编号
     * @return 单个学校对象
     */
    @Cached(name = "School:", key = "#id", expire = 3600, cacheType = CacheType.BOTH)
    SchoolDTO getSchool(String id);

    /**
     * 根据编号集合获取多个学校对象集合（缓存）
     * @param ids 编号集合
     * @return 多个学校对象集合
     */
    @MultiCached(postKey = "#result[$$each$$].id", value = "#result[$$each$$]")
    @Cached(name = "School:", key = "#ids[$$each$$]", expire = 3600, cacheType = CacheType.BOTH)
    List<SchoolDTO> listSchoolByIds(List<String> ids);
    
    /**
     * 根据编号集合获取多个学校对象映射（缓存）
     * @param ids 编号集合
     * @return 多个学校对象映射
     */
    @MultiCached(postKey = "#result[$$each$$].key", value = "#result[$$each$$].value")
    @Cached(name = "School:", key = "#ids[$$each$$]", expire = 3600, cacheType = CacheType.BOTH)
    default Map<String, SchoolDTO> mapSchoolByIds(Set<String> ids) {
        return mapSchoolByIdsNoCache(ids);
    }
}

```

而且`@CacheInvalidate`与`@CacheUpdate`也支持`$$each$$`，使用`$$each$$`也可以使用其常量 `MultiCacheConsts.EACH_ELEMENT`
```java
public interface SchoolService {

    /**
     * 删除单个学校对象
     * @param id 编号
     */
    @CacheInvalidate(name = "School:", key = "#id")
    void deleteSchool(String id);

    /**
     * 根据编号集合删除多个学校对象
     * @param ids 编号集合
     */
    @CacheInvalidate(name = "School:", key = "#ids[" + MultiCacheConsts.EACH_ELEMENT + "]", multi = true)
    void deleteSchoolByIds(Set<String> ids);
}

```

<p>
    

# 3. dubbo先缓存调用支持
## 3.1. 背景
目前通过dubbo实现微服务，跨服务获取数据，往往先通过dubbo再到redis的方式获取数据。这样做在缓存可以命中的情况下多出了大约10ms的dubbo时间消耗和相应带宽消耗，比较浪费。需要调用者先写走redis再走dubbo。

## 3.2. 实现方式
利用 [dubbo动态代理扩展](https://dubbo.apache.org/zh/docs/v2.7/dev/impls/proxy-factory/) 实现将服务消费者的代理实例，再包装一层代理实现缓存注解的拦截的处理。

通过`JetCacheDubboProxyFactoryWrapper`实现`ProxyFactory`接口，根据是否是包含缓存的服务，不是直接返回dubbo代理，是创建三层缓存代理：
1. 第一层代理仅仅为了判断是否是`@Cached`注解方法，如果不是直接dubbo调用，`@CacheInvalidate`或`@CacheUpdate`都应当由远端服务处理完逻辑后处理缓存，不在此多管闲事。
2. 如果是`@Cached`注解方法，通过`JetCacheInterceptor`创建第二层的缓存代理类，调用时处理缓存。
3. 缓存未命中时，进入第三层代理，判断方法是否default方法体，如果是直接执行`default`逻辑，不是则执行dubbo调用。
这种设计提供了一种解决方案，服务声明两个方法`getXxx`和`getXxxNoCache`之类，前者注解`@Cached`且`defaulte`方法体调用后者，后者的实现处理具体业务逻辑。
这样使得dubbo消费者调用`getXxx`时，在消费者端执行缓存处理。 
缓存如果未命中，在消费者端就转为dubbo调用`getXxxNoCache`方法。避免在服务提供端调用`getXxx`再执行一遍缓存处理逻辑。
```
switcher proxy
┌──────────────────────────────────────────────────┐
│  @Cached method         /       other            │
│         │                         │              │
│         │                         │              │
│         ▼                         │              │
│  cache interceptor proxy          │              │
│  ┌────────────────────────────────┼───────────┐  │
│  │  handle cache                  │           │  │
│  │       │                        │           │  │
│  │       │                        │           │  │
│  │       ▼                        │           │  │
│  │  default method proxy          │           │  │
│  │  ┌─────────────────────────────┼────────┐  │  │
│  │  │  is default method    /     │ other  │  │  │
│  │  │      │                      │    │   │  │  │
│  │  │      │                      │    │   │  │  │
│  │  │      │                      ▼    ▼   │  │  │
│  │  │      │             dubbo proxy       │  │  │
│  │  │      │             ┌──────────────┐  │  │  │
│  │  │      ▼             │  rpc         │  │  │  │
│  │  │  default method ──►│              │  │  │  │
│  │  │                    │              │  │  │  │
│  │  │                    └──────────────┘  │  │  │
│  │  │                                      │  │  │
│  │  └──────────────────────────────────────┘  │  │
│  │                                            │  │
│  └────────────────────────────────────────────┘  │
│                                                  │
└──────────────────────────────────────────────────┘
```


## 3.3. 使用说明

build.gradle文件引入依赖
服务接口层
```groovy
    implementation 'com.alicp.jetcache:jetcache-anno:2.6.0'
```
服务实现层
```groovy
    implementation 'io.github.qy8502:jetcache-plus-dubbo:0.0.1'
    implementation('com.alicp.jetcache:jetcache-starter-redis-lettuce:2.6.0')
```

声明服务接口，为了避免重复调用缓存处理逻辑，定义缓存方法和不用的缓存方法。
```java

public interface SchoolService {

    /**
     * 获取单个学校对象（缓存）
     *
     * @param id 编号
     * @return 单个学校对象
     */
    @Cached(name = "School:", key = "#id", expire = 3600, cacheType = CacheType.BOTH)
    default SchoolDTO getSchool(String id) {
        return getSchoolNoCache(id);
    }

    SchoolDTO getSchoolNoCache(String id);

   
    /**
     * 删除单个学校对象
     * @param id 编号
     */
    @CacheInvalidate(name = "School:", key = "#id")
    void deleteSchool(String id);
    
}

```

    
<p>
    

# 4. 例子
模块example-school提供dubbo服务，模块example-teacher为服务消费者，修改两个模块中`application.yml`文件的nacos连接配置和redis连接配置。
先启动ExampleSchoolApplication端口8081，在启动ExampleTeacherApplication端口8080。

获取单个school
<br>[http://127.0.0.1:8081/school/S1](http://127.0.0.1:8081/school/S1)

根据编号集合获取多个school
<br>[http://127.0.0.1:8081/school/list?ids=S1,S2](http://127.0.0.1:8081/school/list?ids=S1,S2)

根据编号集合获取多个school映射
<br>[http://127.0.0.1:8081/school/map?ids=S1,S2](http://127.0.0.1:8081/school/map?ids=S1,S2)

根据编号集合删除多个school
<br>[http://127.0.0.1:8081/school/delete?ids=S1,S2](http://127.0.0.1:8081/school/delete?ids=S1,S2)

获取单个teacher
<br>http://127.0.0.1:8080/teacher/T1

获取teacher集合
<br>http://127.0.0.1:8080/teacher/list
<br>这个请求涵盖了所有增强的特性，通过输出日志可以看出
```text
2021-08-25 14:24:46.406  WARN 1408 --- [nio-8080-exec-1] c.g.q.j.service.impl.TeacherServiceImpl  : JETCACHE_PLUS_EXAMPLE -> TeacherServiceImpl.listTeacher() invoked!
2021-08-25 14:24:51.473 DEBUG 1408 --- [nio-8080-exec-1] c.g.q.jetcacheplus.MultiCacheHandler     : JETCACHE_PLUS_MULTI -> SchoolService.mapSchoolByIds get cache: [S3, S1, S2]
2021-08-25 14:24:51.506 DEBUG 1408 --- [nio-8080-exec-1] c.g.q.jetcacheplus.MultiCacheHandler     : JETCACHE_PLUS_MULTI -> SchoolService.mapSchoolByIds result from cache: [S1, S2]
2021-08-25 14:24:51.506 DEBUG 1408 --- [nio-8080-exec-1] c.g.q.jetcacheplus.MultiCacheHandler     : JETCACHE_PLUS_MULTI -> SchoolService.mapSchoolByIds invoke method: [S3]
2021-08-25 14:24:51.509 DEBUG 1408 --- [nio-8080-exec-1] c.g.q.j.JetCacheDubboProxyFactoryWrapper : JETCACHE_PLUS_DUBBO -> invoke interface default method 'SchoolService.mapSchoolByIds'
2021-08-25 14:24:51.516  WARN 21524 --- [20880-thread-26] c.g.q.j.service.impl.SchoolServiceImpl   : JETCACHE_PLUS_EXAMPLE -> SchoolServiceImpl.mapSchoolByIdsNoCache([S3]) invoked!
2021-08-25 14:24:51.541 DEBUG 1408 --- [nio-8080-exec-1] c.g.q.jetcacheplus.MultiCacheHandler     : JETCACHE_PLUS_MULTI -> SchoolService.mapSchoolByIds final result: [S3]
2021-08-25 14:24:51.547 DEBUG 1408 --- [nio-8080-exec-1] c.g.q.jetcacheplus.MultiCacheHandler     : JETCACHE_PLUS_MULTI -> SchoolService.mapSchoolByIds put cache: [S3]
2021-08-25 14:24:51.548 DEBUG 1408 --- [nio-8080-exec-1] c.g.q.jetcacheplus.MultiCacheHandler     : JETCACHE_PLUS_MULTI -> SchoolService.mapSchoolByIds final result: [S3, S1, S2]
2021-08-25 14:24:51.559 DEBUG 1408 --- [oEventLoop-13-1] c.g.q.j.AutoInvalidateLocalCacheContext  : JETCACHE_PLUS_AUTO_INVALIDATE_LOCAL -> invalidate local cache by remote cache 'cache:jetcache-plus-example:School:S3': true
```
