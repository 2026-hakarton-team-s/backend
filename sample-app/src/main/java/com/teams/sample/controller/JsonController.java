package com.teams.sample.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 데모 포인트 ②: jackson-databind 역직렬화를 실행하는 API.
 *
 * <pre>
 * curl -X POST -H "Content-Type: text/plain" --data '{"name":"depscan"}' http://localhost:8081/api/json
 * </pre>
 *
 * <p>참고: Spring MVC 자체도 jackson 을 쓰기 때문에 jackson 은 서버가 뜨는 순간 로드될 수 있습니다.
 * TODO(샘플 앱 담당): 데모에서 "API 를 호출해야만 로드되는" 라이브러리가 더 필요하면,
 * 기본으로는 로드되지 않는 취약 라이브러리를 하나 더 추가하고 전용 API 를 만드세요.</p>
 */
@RestController
public class JsonController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/api/json")
    @SuppressWarnings("unchecked")
    public Map<String, Object> parseJson(@RequestBody String body) throws JsonProcessingException {
        return objectMapper.readValue(body, Map.class);
    }
}
