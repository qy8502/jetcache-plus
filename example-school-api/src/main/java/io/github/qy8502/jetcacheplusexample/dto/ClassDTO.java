package io.github.qy8502.jetcacheplusexample.dto;


import java.io.Serializable;


public class ClassDTO implements Serializable {

    private static final long serialVersionUID = -2873467147635192603L;

    private String teacherId;
    private String lessonId;
    private String name;

    public ClassDTO() {
    }

    public ClassDTO(String teacherId, String lessonId, String name) {
        this.teacherId = teacherId;
        this.lessonId = lessonId;
        this.name = name;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getLessonId() {
        return lessonId;
    }

    public void setLessonId(String lessonId) {
        this.lessonId = lessonId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
