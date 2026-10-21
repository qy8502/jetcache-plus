package io.github.qy8502.jetcacheplusexample.service.impl;

import io.github.qy8502.jetcacheplusexample.dto.ClassDTO;
import io.github.qy8502.jetcacheplusexample.service.ClassService;
import org.apache.dubbo.config.annotation.DubboService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@DubboService
public class ClassServiceImpl implements ClassService {

    private static final Logger logger = LoggerFactory.getLogger(ClassServiceImpl.class);

    public static Map<String, ClassDTO> classRepository;

    static {
        classRepository = IntStream.range(1, 4).boxed()
                .map(i -> new ClassDTO("T" + i, "L" + i, "课程" + i))
                .collect(Collectors.toMap(c -> c.getTeacherId() + ":" + c.getLessonId(), Function.identity()));
    }

    @Override
    public ClassDTO getClassNoCache(String teacherId, String lessonId) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> ClassServiceImpl.getClassNoCache({}, {}) invoked!", teacherId, lessonId);
        return classRepository.get(teacherId + ":" + lessonId);
    }

    @Override
    public List<ClassDTO> listClassByClassesNoCache(List<ClassDTO> classes) {
        logger.warn("JETCACHE_PLUS_EXAMPLE -> ClassServiceImpl.listClassByClassesNoCache({}) invoked!", classKeys(classes));
        return classes.stream()
                .map(c -> classRepository.get(c.getTeacherId() + ":" + c.getLessonId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<String> classKeys(List<ClassDTO> classes) {
        return classes.stream()
                .map(c -> c.getTeacherId() + ":" + c.getLessonId())
                .collect(Collectors.toList());
    }
}
