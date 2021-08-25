package io.github.qy8502.jetcacheplusexample.service;


import io.github.qy8502.jetcacheplusexample.dto.TeacherDTO;

import java.util.List;

public interface TeacherService {

    TeacherDTO getTeacher(String id);

    List<TeacherDTO> listTeacher();
}
