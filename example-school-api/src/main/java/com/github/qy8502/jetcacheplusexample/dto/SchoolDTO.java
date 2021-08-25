package com.github.qy8502.jetcacheplusexample.dto;


import java.io.Serializable;


public class SchoolDTO implements Serializable {
    private static final long serialVersionUID = -715432915066395982L;
    private String id;
    private String name;

    public SchoolDTO() {
    }

    public SchoolDTO(String id, String name) {
        this.id = id;
        this.name = name;
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
}
