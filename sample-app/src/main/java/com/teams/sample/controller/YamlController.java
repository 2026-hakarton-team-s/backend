package com.teams.sample.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

/**
 * 데모 포인트 ①: SnakeYAML 을 "실제로 실행"하는 API.
 *
 * <p>이 API 를 호출하면 snakeyaml jar 의 클래스가 로드되므로
 * Agent 기록에 snakeyaml 이 남고 → 대시보드에서 REACHABLE(빨강)로 표시됩니다.</p>
 *
 * <p>⚠️ 구버전 SnakeYAML 의 {@code new Yaml().load(외부입력)} 은 알려진 위험 패턴입니다.
 * 데모 목적이므로 로컬에서만 실행하세요.</p>
 *
 * <pre>
 * curl -X POST -H "Content-Type: text/plain" --data "name: depscan" http://localhost:8081/api/yaml
 * </pre>
 */
@RestController
public class YamlController {

    @PostMapping("/api/yaml")
    public Object parseYaml(@RequestBody String body) {
        return new Yaml().load(body);
    }
}
