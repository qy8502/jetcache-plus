package com.github.qy8502.jetcacheplusexample;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableMethodCache(basePackages = "com.github.qy8502.jetcacheplusexample")
public class ExampleTeacherApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleTeacherApplication.class, args);
    }

}
