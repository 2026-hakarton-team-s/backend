package com.teams.depscan;

import com.teams.depscan.domain.DependencyTree;
import com.teams.depscan.parser.GradleDependencyTreeParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * deps.txt 파서 테스트 (실제 sample-app 2.7.0 결과 일부를 그대로 사용).
 */
class GradleDependencyTreeParserTest {

    private static final String DEPS = "﻿\n"
            + "> Task :dependencies\n"
            + "\n"
            + "------------------------------------------------------------\n"
            + "Root project 'sample-app'\n"
            + "------------------------------------------------------------\n"
            + "\n"
            + "runtimeClasspath - Runtime classpath of source set 'main'.\n"
            + "\\--- org.springframework.boot:spring-boot-starter-web -> 2.7.0\n"
            + "     +--- org.springframework.boot:spring-boot-starter:2.7.0\n"
            + "     |    +--- org.springframework.boot:spring-boot-starter-logging:2.7.0\n"
            + "     |    |    \\--- ch.qos.logback:logback-classic:1.2.11\n"
            + "     |    |         +--- ch.qos.logback:logback-core:1.2.11\n"
            + "     |    |         \\--- org.slf4j:slf4j-api:1.7.32 -> 1.7.36\n"
            + "     |    \\--- org.yaml:snakeyaml:1.30\n"
            + "     +--- org.springframework.boot:spring-boot-starter-json:2.7.0\n"
            + "     |    +--- org.springframework.boot:spring-boot-starter:2.7.0 (*)\n"
            + "     |    \\--- com.fasterxml.jackson.core:jackson-databind:2.13.3\n"
            + "     |         +--- com.fasterxml.jackson:jackson-bom:2.13.3\n"
            + "     |         |    \\--- com.fasterxml.jackson.core:jackson-core:2.13.3 (c)\n"
            + "     |         \\--- com.fasterxml.jackson.core:jackson-core:2.13.3\n"
            + "     \\--- org.springframework:spring-webmvc:5.3.20\n"
            + "\n"
            + "(c) - A dependency constraint, not a dependency.\n";

    private final GradleDependencyTreeParser parser = new GradleDependencyTreeParser();

    @Test
    void 트리를_노드와_연결선으로_변환() {
        DependencyTree tree = parser.parse(new ByteArrayInputStream(DEPS.getBytes(StandardCharsets.UTF_8)));

        assertEquals("sample-app", tree.rootName());

        Map<String, DependencyTree.Node> nodes = tree.nodes().stream()
                .collect(Collectors.toMap(DependencyTree.Node::key, Function.identity()));

        // 화살표가 있으면 실제 선택된 버전
        assertEquals("2.7.0", nodes.get("org.springframework.boot:spring-boot-starter-web").version());
        assertEquals("1.7.36", nodes.get("org.slf4j:slf4j-api").version());
        assertEquals("1.30", nodes.get("org.yaml:snakeyaml").version());

        // BOM 과 (c) 제약 줄은 제외
        assertFalse(nodes.containsKey("com.fasterxml.jackson:jackson-bom"));
        // 중복 없이 한 번만
        assertEquals(11, nodes.size());

        // 연결선: 앱 → starter-web, starter → snakeyaml, (*) 반복도 연결선은 추가
        assertTrue(tree.edges().contains(new DependencyTree.Edge("app", "org.springframework.boot:spring-boot-starter-web")));
        assertTrue(tree.edges().contains(new DependencyTree.Edge(
                "org.springframework.boot:spring-boot-starter", "org.yaml:snakeyaml")));
        assertTrue(tree.edges().contains(new DependencyTree.Edge(
                "org.springframework.boot:spring-boot-starter-json", "org.springframework.boot:spring-boot-starter")));
        // 깊은 단계에서 다시 얕은 단계로 돌아올 때 부모가 올바른지
        assertTrue(tree.edges().contains(new DependencyTree.Edge(
                "org.springframework.boot:spring-boot-starter-web", "org.springframework:spring-webmvc")));
        assertTrue(tree.edges().contains(new DependencyTree.Edge(
                "com.fasterxml.jackson.core:jackson-databind", "com.fasterxml.jackson.core:jackson-core")));
    }
}
