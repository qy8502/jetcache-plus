package io.github.qy8502.jetcacheplusexample;

import com.alicp.jetcache.CacheValueHolder;
import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.qy8502.jetcacheplus.JACKSON;
import io.github.qy8502.jetcacheplusexample.dto.SchoolDTO;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMethodCache(basePackages = "io.github.qy8502.jetcacheplusexample")
@EnableCreateCacheAnnotation
public class ExampleSchoolApplication {

    public static void main(String[] args) throws JsonProcessingException {
        SchoolDTO schoolDTO = new SchoolDTO("S1", "希望小学1");
        CacheValueHolder cacheValueHolder = new CacheValueHolder(schoolDTO, 1000);
        String string = JACKSON.MAPPER.writeValueAsString(schoolDTO);
        String stringCache = JACKSON.MAPPER.writeValueAsString(cacheValueHolder);
        System.out.println(string);
        System.out.println(stringCache);
        // CacheValueHolder no @class property
        SpringApplication.run(ExampleSchoolApplication.class, args);
    }

}
