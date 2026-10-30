package com.teams.depscan.parser;

import com.teams.depscan.domain.DependencyTree;
import com.teams.depscan.exception.InvalidReportException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code ./gradlew dependencies --configuration runtimeClasspath} 출력(deps.txt)을 읽어
 * {@link DependencyTree} 로 변환합니다.
 *
 * <p>생성 명령 (Windows PowerShell, sample-app 폴더에서):</p>
 * <pre>
 * ..\gradlew.bat dependencies --configuration runtimeClasspath | Out-File -Encoding utf8 deps.txt
 * </pre>
 *
 * <p>입력 예:</p>
 * <pre>
 * runtimeClasspath - Runtime classpath of source set 'main'.
 * \--- org.springframework.boot:spring-boot-starter-web -> 2.7.0
 *      +--- org.springframework.boot:spring-boot-starter:2.7.0
 *      |    +--- org.yaml:snakeyaml:1.30
 *      |    \--- org.springframework:spring-core:5.3.20 (*)
 * </pre>
 *
 * <p>처리 규칙</p>
 * <ul>
 *   <li>들여쓰기 5칸 = 깊이 1단계. 바로 위 단계의 라이브러리가 부모(source)가 됩니다.</li>
 *   <li>{@code a:b -> 2.7.0}, {@code a:b:1.0 -> 1.1} 처럼 화살표가 있으면 오른쪽(실제 선택된) 버전을 씁니다.</li>
 *   <li>{@code (*)} = 앞에서 이미 펼친 항목의 반복 → 연결선만 추가합니다.</li>
 *   <li>{@code (c)} = 버전 제약(constraint)일 뿐 실제 의존성이 아님 → 건너뜁니다.</li>
 *   <li>{@code xxx-bom} = 버전 묶음(BOM)일 뿐 jar 가 아님 → 하위 항목까지 건너뜁니다.</li>
 * </ul>
 */
@Component
public class GradleDependencyTreeParser {

    /** 트리 한 줄: (들여쓰기)(+--- 또는 \---) (좌표) */
    private static final Pattern TREE_LINE = Pattern.compile("^([| ]*)[+\\\\]--- (.+)$");

    /** 줄 끝 표시: (*) 반복, (c) 제약, (n) 해석 안 됨 */
    private static final Pattern MARKER = Pattern.compile("\\s+\\((\\*|c|n)\\)\\s*$");

    /** 첫 줄의 프로젝트 이름: Root project 'sample-app' */
    private static final Pattern ROOT_PROJECT = Pattern.compile("Root project '([^']+)'");

    /** 들여쓰기 한 단계의 글자 수 ("|    " 또는 "     ") */
    private static final int INDENT = 5;

    public DependencyTree parse(InputStream text) {
        String rootName = "app";
        Map<String, DependencyTree.Node> nodes = new LinkedHashMap<>();
        Set<DependencyTree.Edge> edges = new LinkedHashSet<>();

        // 깊이별 부모 ID를 쌓아두는 스택 (맨 위 = 직전 줄의 부모 후보)
        Deque<String> parents = new ArrayDeque<>();
        boolean inTree = false;
        boolean treeFinished = false;
        int skipDeeperThan = -1; // BOM 처럼 건너뛴 항목의 하위 줄도 건너뛰기 위한 깊이

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(text, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && !treeFinished) {
                line = line.replace("﻿", ""); // PowerShell 이 붙이는 BOM 제거

                Matcher root = ROOT_PROJECT.matcher(line);
                if (root.find()) {
                    rootName = root.group(1);
                    continue;
                }

                Matcher m = TREE_LINE.matcher(line);
                if (!m.matches()) {
                    // 트리가 시작된 뒤 트리 형식이 아닌 줄이 나오면 첫 번째 트리(설정 하나) 끝
                    if (inTree) {
                        treeFinished = true;
                    }
                    continue;
                }
                inTree = true;

                int depth = m.group(1).length() / INDENT;
                String rest = m.group(2).trim();

                // 건너뛴 항목(BOM)의 하위 줄이면 무시
                if (skipDeeperThan >= 0) {
                    if (depth > skipDeeperThan) {
                        continue;
                    }
                    skipDeeperThan = -1;
                }

                String marker = "";
                Matcher mk = MARKER.matcher(rest);
                if (mk.find()) {
                    marker = mk.group(1);
                    rest = rest.substring(0, mk.start()).trim();
                }
                if (marker.equals("c") || marker.equals("n")) {
                    continue; // 제약/미해석 항목은 실제 의존성이 아님
                }

                DependencyTree.Node node = toNode(rest);
                if (node == null || node.artifactId().endsWith("-bom")) {
                    skipDeeperThan = depth;
                    continue;
                }

                // 현재 깊이에 맞게 부모 스택 정리: 스택 크기 == depth 가 되도록
                while (parents.size() > depth) {
                    parents.pop();
                }
                String parentId = parents.isEmpty() ? DependencyTree.ROOT_ID : parents.peek();

                nodes.putIfAbsent(node.key(), node);
                edges.add(new DependencyTree.Edge(parentId, node.key()));

                // 이 노드가 다음 줄(더 깊은 줄)의 부모 후보
                parents.push(node.key());
            }
        } catch (IOException e) {
            throw new InvalidReportException("deps.txt 를 읽을 수 없습니다.", e);
        }

        if (nodes.isEmpty()) {
            throw new InvalidReportException(
                    "의존성 트리를 찾지 못했습니다. './gradlew dependencies --configuration runtimeClasspath' 결과 파일인지 확인하세요.");
        }
        return new DependencyTree(rootName, new ArrayList<>(nodes.values()), new ArrayList<>(edges));
    }

    /**
     * "그룹:이름:버전", "그룹:이름 -> 버전", "그룹:이름:버전1 -> 버전2" 를 해석합니다.
     * 형식을 알 수 없으면 null.
     */
    static DependencyTree.Node toNode(String coordinate) {
        String left = coordinate;
        String resolvedVersion = null;

        int arrow = coordinate.indexOf(" -> ");
        if (arrow >= 0) {
            left = coordinate.substring(0, arrow).trim();
            String right = coordinate.substring(arrow + 4).trim();
            // "-> 1.7.36" 또는 드물게 "-> 그룹:이름:버전" (치환된 경우)
            int lastColon = right.lastIndexOf(':');
            resolvedVersion = lastColon >= 0 ? right.substring(lastColon + 1) : right;
        }

        String[] parts = left.split(":");
        if (parts.length < 2) {
            return null; // "project :xxx" 같은 형식은 그래프에서 제외
        }
        String version = resolvedVersion != null ? resolvedVersion : (parts.length >= 3 ? parts[2] : "");
        return new DependencyTree.Node(parts[0].trim(), parts[1].trim(), version.trim());
    }
}
