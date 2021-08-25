package io.github.qy8502.jetcacheplusexample;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMethodCache(basePackages = "io.github.qy8502.jetcacheplusexample")
public class ExampleTeacherApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleTeacherApplication.class, args);
    }

}
