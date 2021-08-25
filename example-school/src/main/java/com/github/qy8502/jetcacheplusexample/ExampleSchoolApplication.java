package com.github.qy8502.jetcacheplusexample;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMethodCache(basePackages = "com.github.qy8502.jetcacheplusexample")
@EnableCreateCacheAnnotation
public class ExampleSchoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleSchoolApplication.class, args);
    }

}
