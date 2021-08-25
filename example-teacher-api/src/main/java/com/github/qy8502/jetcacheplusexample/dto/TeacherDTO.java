package com.github.qy8502.jetcacheplusexample.dto;


import java.io.Serializable;


public class TeacherDTO implements Serializable {

    private static final long serialVersionUID = 6114818412444042956L;
    private String id;
    private String name;
    private String schoolId;
    private SchoolDTO school;

    public TeacherDTO() {
    }

    public TeacherDTO(String id, String name, String schoolId) {
        this.id = id;
        this.name = name;
        this.schoolId = schoolId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(String schoolId) {
        this.schoolId = schoolId;
    }

    public SchoolDTO getSchool() {
        return school;
    }

    public void setSchool(SchoolDTO school) {
        this.school = school;
    }
}
