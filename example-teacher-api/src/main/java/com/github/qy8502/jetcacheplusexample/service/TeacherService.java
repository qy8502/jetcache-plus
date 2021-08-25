package com.github.qy8502.jetcacheplusexample.service;


import com.github.qy8502.jetcacheplusexample.dto.TeacherDTO;

import java.util.List;

public interface TeacherService {

    TeacherDTO getTeacher(String id);

    List<TeacherDTO> listTeacher();
}
