/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.qy8502.jetcacheplusexample.service.impl;

import io.github.qy8502.jetcacheplusexample.dto.SchoolDTO;
import io.github.qy8502.jetcacheplusexample.service.SchoolService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@DubboService
public class SchoolServiceImpl implements SchoolService {

    private static final Logger logger = LoggerFactory.getLogger(SchoolServiceImpl.class);


    public static Map<String, SchoolDTO> schoolRepository;

    static {
        schoolRepository = IntStream.range(1, 4).asLongStream()
                .mapToObj(i -> new SchoolDTO("S" + i, "希望小学" + i))
                .collect(Collectors.toMap(SchoolDTO::getId, Function.identity()));
    }

    @Override
    public SchoolDTO getSchoolNoCache(String id) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> SchoolServiceImpl.getSchoolNoCache({}) invoked!", id);
        return schoolRepository.get(id);
    }

    @Override
    public List<SchoolDTO> listSchool() {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> SchoolServiceImpl.listSchool() invoked!");
        return new ArrayList<>(schoolRepository.values());
    }


//    @CreateCache(name = "School:", expire = 3600, cacheType = CacheType.BOTH)
//    Cache<String, SchoolDTO> schoolCache;


//    public Map<String, SchoolDTO> mapSchoolByIds(Set<String> ids) {
//        Map<String, SchoolDTO> schoolMap = schoolCache.getAll(ids);
//        Set<String> idsNoCache;
//        if (schoolMap.isEmpty()) {
//            idsNoCache = ids;
//        } else if (schoolMap.size() == ids.size()) {
//            return schoolMap;
//        } else {
//            idsNoCache = ids.stream().filter(id -> !schoolMap.containsKey(id)).collect(Collectors.toSet());
//        }
//        Map<String, SchoolDTO> schoolMapNoCache = mapSchoolByIdsNoCache(idsNoCache);
//        schoolCache.putAll(schoolMapNoCache);
//        schoolMap.putAll(schoolMapNoCache);
//        return schoolMap;
//    }

    @Override
    public Map<String, SchoolDTO> mapSchoolByIdsNoCache(Set<String> ids) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> SchoolServiceImpl.mapSchoolByIdsNoCache({}) invoked!", ids);
        return schoolRepository.entrySet().stream()
                .filter(entry -> ids.contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }


//    public List<SchoolDTO> listSchoolByIds(Set<String> ids) {
//        Map<String, SchoolDTO> schoolMap = schoolCache.getAll(ids);
//        Set<String> idsNoCache;
//        if (schoolMap.isEmpty()) {
//            idsNoCache = ids;
//        } else if (schoolMap.size() == ids.size()) {
//            return new ArrayList<>(schoolMap.values());
//        } else {
//            idsNoCache = ids.stream().filter(id -> !schoolMap.containsKey(id)).collect(Collectors.toSet());
//        }
//        List<SchoolDTO> schoolListNoCache = listSchoolByIdsNoCache(idsNoCache);
//        schoolCache.putAll(schoolListNoCache.stream().collect(Collectors.toMap(SchoolDTO::getId, Function.identity(), (a, b) -> a)));
//        schoolListNoCache.addAll(schoolMap.values());
//        return schoolListNoCache;
//    }

    @Override
    public List<SchoolDTO> listSchoolByIdsNoCache(List<String> ids) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> SchoolServiceImpl.listSchoolByIdsNoCache({}) invoked!", ids);
        return schoolRepository.entrySet().stream()
                .filter(entry -> ids.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }


    @Override
    public void deleteSchoolByIds(Set<String> ids) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> SchoolServiceImpl.deleteSchoolByIds({}) invoked!", ids);
        return;
    }

    @Override
    public void deleteSchool(String id) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> SchoolServiceImpl.deleteSchool({}) invoked!", id);
        return;
    }
}
