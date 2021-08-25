package com.github.qy8502.jetcacheplusexample.service;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.github.qy8502.jetcacheplus.MultiCacheConsts;
import com.github.qy8502.jetcacheplus.MultiCached;
import com.github.qy8502.jetcacheplusexample.dto.SchoolDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface SchoolService {

    /**
     * 获取单个学校对象（缓存）
     *
     * @param id 编号
     * @return 单个学校对象
     */
    @Cached(name = "School:", key = "#id", expire = 3600, cacheType = CacheType.BOTH, cacheNullValue = true)
    default SchoolDTO getSchool(String id) {
        return getSchoolNoCache(id);
    }

    SchoolDTO getSchoolNoCache(String id);

    @Cached(name = "SchoolList:", expire = 3600, cacheType = CacheType.BOTH)
    List<SchoolDTO> listSchool();

    /**
     * 根据编号集合获取多个学校对象集合（缓存）
     *
     * @param ids 编号集合
     * @return 多个学校对象集合
     */
    @MultiCached(postKey = "#result[$$each$$].key", value = "#result[$$each$$].value")
    @Cached(name = "School:", key = "#ids[$$each$$]", expire = 3600, cacheType = CacheType.BOTH
            , cacheNullValue = true, condition = "#ids[$$each$$]!='S5' && #ids[$$each$$]!='S6'"
            , postCondition = "#result[$$each$$].key!='S7' && #result[$$each$$]!='S8'")
    default Map<String, SchoolDTO> mapSchoolByIds(Set<String> ids) {
        return mapSchoolByIdsNoCache(ids);
    }

    Map<String, SchoolDTO> mapSchoolByIdsNoCache(Set<String> ids);


    /**
     * 根据编号集合获取多个学校对象映射（缓存）
     *
     * @param ids 编号集合
     * @return 多个学校对象映射
     */
    @MultiCached(postKey = "#result[$$each$$].id", value = "#result[$$each$$]")
    @Cached(name = "School:", key = "#ids[$$each$$]", expire = 3600, cacheType = CacheType.BOTH)
    default List<SchoolDTO> listSchoolByIds(List<String> ids) {
        return listSchoolByIdsNoCache(ids);
    }

    List<SchoolDTO> listSchoolByIdsNoCache(List<String> ids);

    /**
     * 删除单个学校对象
     *
     * @param id 编号
     */
    @CacheInvalidate(name = "School:", key = "#id")
    void deleteSchool(String id);

    /**
     * 根据编号集合删除多个学校对象
     *
     * @param ids 编号集合
     */
    @CacheInvalidate(name = "School:", key = "#ids[" + MultiCacheConsts.EACH_ELEMENT + "]", multi = true)
    void deleteSchoolByIds(Set<String> ids);
}
