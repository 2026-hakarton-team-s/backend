package com.teams.depscan.domain;

import java.util.List;

/**
 * Gradle 의존성 트리(./gradlew dependencies 결과)를 그래프 형태로 정리한 것.
 *
 * @param rootName 분석 대상 앱 이름 (예: sample-app)
 * @param nodes    라이브러리 목록 (groupId:artifactId 기준으로 중복 제거)
 * @param edges    "누가 누구를 끌고 왔는가" 연결 (source → target)
 */
public record DependencyTree(
        String rootName,
        List<Node> nodes,
        List<Edge> edges
) {

    /** 그래프에서 루트(앱 자신)를 가리키는 고정 ID */
    public static final String ROOT_ID = "app";

    /**
     * 라이브러리 1개.
     *
     * @param groupId    예: org.yaml
     * @param artifactId 예: snakeyaml
     * @param version    실제로 선택된(resolved) 버전. 예: 1.30
     */
    public record Node(String groupId, String artifactId, String version) {

        /** 그래프 ID 겸 Trivy 결과와 매칭하는 키. 예: "org.yaml:snakeyaml" */
        public String key() {
            return groupId + ":" + artifactId;
        }
    }

    /** source 가 target 을 의존성으로 가져옴 (ID 는 Node.key() 또는 ROOT_ID) */
    public record Edge(String source, String target) {
    }
}
