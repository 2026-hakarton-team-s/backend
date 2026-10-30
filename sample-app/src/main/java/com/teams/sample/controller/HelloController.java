package com.teams.sample.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 취약 라이브러리를 건드리지 않는 평범한 API (비교용).
 *
 * <pre>
 * curl http://localhost:8081/api/hello
 * </pre>
 */
@RestController
public class HelloController {

    @GetMapping("/api/hello")
    public String hello() {
        return "hello, depscan";
    }
}
