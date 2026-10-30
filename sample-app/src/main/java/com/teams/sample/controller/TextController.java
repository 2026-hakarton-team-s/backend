package com.teams.sample.controller;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 데모 포인트 ③: commons-text 를 호출할 때만 로드하는 API.
 *
 * <p>이 API 를 호출해야 commons-text jar 의 클래스가 로드되므로
 * 호출 전에는 NOT_LOADED, 호출 후에는 REACHABLE 로 표시됩니다.</p>
 *
 * <p>로드 여부만 확인하면 되므로 취약 기능(StringSubstitutor.createInterpolator)은 쓰지 않습니다.</p>
 *
 * <pre>
 * curl -X POST -H "Content-Type: text/plain" --data 'hello ${name}' http://localhost:8081/api/text
 * </pre>
 */
@RestController
public class TextController {

    @PostMapping("/api/text")
    public String substitute(@RequestBody String body) {
        return new StringSubstitutor(Map.of("name", "depscan")).replace(body);
    }
}
