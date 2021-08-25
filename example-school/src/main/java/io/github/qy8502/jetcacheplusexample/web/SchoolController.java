/*
 * Copyright 2013-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.qy8502.jetcacheplusexample.web;

import io.github.qy8502.jetcacheplusexample.dto.SchoolDTO;
import io.github.qy8502.jetcacheplusexample.service.SchoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class SchoolController {

    @Autowired
    private SchoolService schoolService;


    @GetMapping("/school/{id}")
    public SchoolDTO getSchool(@PathVariable String id) {
        return schoolService.getSchool(id);
    }

    @GetMapping("/school/list")
    public List<SchoolDTO> listSchool() {
        return schoolService.listSchool();
    }

    @GetMapping("/school/{id}/delete")
    public void deleteSchool(@PathVariable String id) {
        schoolService.deleteSchool(id);
    }

    @GetMapping(value = "/school/delete", params = {"ids"})
    public void deleteSchoolByIds(@RequestParam Set<String> ids) {
        schoolService.deleteSchoolByIds(ids);
    }


    @GetMapping(value = "/school/map", params = {"ids"})
    public Map<String, SchoolDTO> mapSchoolByIds(@RequestParam Set<String> ids) {
        return schoolService.mapSchoolByIds(ids);
    }

    @GetMapping(value = "/school/list", params = {"ids"})
    public List<SchoolDTO> listSchoolByIds(@RequestParam List<String> ids) {
        return schoolService.listSchoolByIds(ids);
    }
}
