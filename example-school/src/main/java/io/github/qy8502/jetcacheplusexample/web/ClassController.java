package io.github.qy8502.jetcacheplusexample.web;

import io.github.qy8502.jetcacheplusexample.dto.ClassDTO;
import io.github.qy8502.jetcacheplusexample.service.ClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ClassController {

    @Autowired
    private ClassService classService;

    @GetMapping("/class/{teacherId}/{lessonId}")
    public ClassDTO getClass(@PathVariable String teacherId, @PathVariable String lessonId) {
        return classService.getClass(teacherId, lessonId);
    }

    @PostMapping("/class/list")
    public List<ClassDTO> listClassByClasses(@RequestBody List<ClassDTO> classes) {
        return classService.listClassByClasses(classes);
    }
}
