package io.github.qy8502.jetcacheplusexample.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import io.github.qy8502.jetcacheplus.MultiCached;
import io.github.qy8502.jetcacheplusexample.dto.ClassDTO;

import java.util.List;

public interface ClassService {

    /**
     * 获取单个班级对象（缓存，联合主键 teacherId + lessonId）
     *
     * @param teacherId 教师编号
     * @param lessonId  课程编号
     * @return 单个班级对象
     */
    @Cached(name = "Class:", key = "#teacherId+':'+#lessonId", expire = 3600, cacheType = CacheType.BOTH)
    default ClassDTO getClass(String teacherId, String lessonId) {
        return getClassNoCache(teacherId, lessonId);
    }

    ClassDTO getClassNoCache(String teacherId, String lessonId);

    /**
     * 根据班级对象集合获取多个班级对象集合（缓存，联合主键 teacherId + lessonId）
     *
     * @param classes 班级对象集合
     * @return 多个班级对象集合
     */
    @MultiCached(postKey = "#result[$$each$$].teacherId+':'+#result[$$each$$].lessonId", value = "#result[$$each$$]")
    @Cached(name = "Class:", key = "#classes[$$each$$].teacherId+':'+#classes[$$each$$].lessonId", expire = 3600, cacheType = CacheType.BOTH)
    default List<ClassDTO> listClassByClasses(List<ClassDTO> classes) {
        return listClassByClassesNoCache(classes);
    }

    List<ClassDTO> listClassByClassesNoCache(List<ClassDTO> classes);
}
