package com.teams.depscan.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 대시보드 의존성 그래프 응답. 프론트 mockDependencyGraph 와 같은 형식입니다.
 *
 * <pre>
 * {
 *   "nodes": [
 *     { "id": "app", "label": "sample-app", "root": true },
 *     { "id": "org.yaml:snakeyaml", "label": "snakeyaml\n1.30",
 *       "vulnerable": true, "reachable": true,
 *       "version": "1.30", "vulnerabilityCount": 7, "maxSeverity": "CRITICAL" }
 *   ],
 *   "edges": [ { "source": "app", "target": "org.springframework.boot:spring-boot-starter-web" } ]
 * }
 * </pre>
 *
 * <p>mock 에 없던 필드(version, vulnerabilityCount, maxSeverity)는 툴팁 등에 쓰라고 추가했습니다.
 * 프론트에서 안 써도 문제없습니다.</p>
 */
public record DependencyGraphResponse(
        List<NodeResponse> nodes,
        List<EdgeResponse> edges
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NodeResponse(
            String id,
            String label,
            Boolean root,              // 루트(앱 자신)에만 true, 나머지는 생략
            boolean vulnerable,        // Trivy 가 취약점을 찾은 라이브러리
            boolean reachable,         // 그중 실제로 실행(로드)된 라이브러리
            String version,
            int vulnerabilityCount,
            String maxSeverity         // 취약점이 없으면 생략
    ) {
    }

    public record EdgeResponse(String source, String target) {
    }
}
