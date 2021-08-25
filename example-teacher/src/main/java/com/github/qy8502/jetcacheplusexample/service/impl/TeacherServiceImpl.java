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
package com.github.qy8502.jetcacheplusexample.service.impl;

import com.github.qy8502.jetcacheplusexample.dto.SchoolDTO;
import com.github.qy8502.jetcacheplusexample.dto.TeacherDTO;
import com.github.qy8502.jetcacheplusexample.service.SchoolService;
import com.github.qy8502.jetcacheplusexample.service.TeacherService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class TeacherServiceImpl implements TeacherService {

    private static final Logger logger = LoggerFactory.getLogger(TeacherServiceImpl.class);

    public static Map<String, TeacherDTO> teacherRepository;

    static {
        teacherRepository = IntStream.range(1, 6).asLongStream()
                .mapToObj(i -> new TeacherDTO("T" + i, "教师" + i, "S" + (i % 3 + 1)))
                .collect(Collectors.toMap(TeacherDTO::getId, Function.identity()));
    }

    @DubboReference()
    public SchoolService schoolService;

    @Override
    public TeacherDTO getTeacher(String id) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> TeacherServiceImpl.getTeacher({}) invoked!", id);
        TeacherDTO teacher = teacherRepository.get(id);
        if (teacher != null) {
            SchoolDTO school = schoolService.getSchool(teacher.getSchoolId());
            teacher.setSchool(school);
        }
        return teacher;
    }

    @Override
    public List<TeacherDTO> listTeacher() {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> TeacherServiceImpl.listTeacher() invoked!");
        List<TeacherDTO> list = new ArrayList<>(teacherRepository.values());
        Set<String> schoolIds = list.stream().map(TeacherDTO::getSchoolId).collect(Collectors.toSet());
        Map<String, SchoolDTO> schoolMap = schoolService.mapSchoolByIds(schoolIds);
        list.forEach(teacher -> teacher.setSchool(schoolMap.get(teacher.getSchoolId())));
        return list;
    }
}
