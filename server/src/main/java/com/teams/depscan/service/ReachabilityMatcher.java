package com.teams.depscan.service;

import com.teams.depscan.domain.Finding;
import com.teams.depscan.domain.LoadedLibrary;
import com.teams.depscan.domain.Reachability;
import com.teams.depscan.domain.Vulnerability;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * ★ 프로젝트 핵심 로직: 정적 결과 × 동적 결과 교차 분석.
 *
 * <p>"Trivy 가 취약하다고 한 라이브러리"가 "Agent 기록에 있는가?"를 확인합니다.</p>
 *
 * <pre>
 * Trivy : org.yaml:snakeyaml 1.30  (CVE-2022-1471)
 * Agent : snakeyaml 1.30 → 클래스 42개 로드됨
 *   → 이름(artifactId)과 버전이 같으므로 REACHABLE
 * </pre>
 *
 * <p>현재 매칭 기준: artifactId + 버전.
 * Agent 쪽 버전을 알 수 없는 경우에는 artifactId 만으로 매칭합니다.</p>
 *
 * <p>TODO(분석 담당) 개선 아이디어:
 * <ul>
 *   <li>같은 artifactId 를 가진 서로 다른 groupId 구분 (Agent 가 groupId 를 모름)</li>
 *   <li>클래스 단위 매칭: CVE 의 취약 클래스가 실제로 로드됐는지까지 확인</li>
 * </ul></p>
 */
@Component
public class ReachabilityMatcher {

    public List<Finding> match(List<Vulnerability> vulnerabilities, List<LoadedLibrary> loadedLibraries) {
        // 빠른 검색을 위해 Agent 기록을 Map 으로 정리
        // key: "snakeyaml:1.30" (이름+버전),  "snakeyaml" (이름만)
        Map<String, LoadedLibrary> byNameAndVersion = new HashMap<>();
        Map<String, LoadedLibrary> byNameOnly = new HashMap<>();
        for (LoadedLibrary lib : loadedLibraries) {
            byNameAndVersion.put(key(lib.artifactId(), lib.version()), lib);
            byNameOnly.putIfAbsent(normalize(lib.artifactId()), lib);
        }

        return vulnerabilities.stream()
                .map(vuln -> judge(vuln, byNameAndVersion, byNameOnly))
                // 정렬: 실행된 것 먼저 → 심각도 높은 것 먼저
                .sorted(Comparator
                        .comparing((Finding f) -> f.reachability() != Reachability.REACHABLE)
                        .thenComparing(f -> severityRank(f.vulnerability().severity())))
                .toList();
    }

    private Finding judge(Vulnerability vuln,
                          Map<String, LoadedLibrary> byNameAndVersion,
                          Map<String, LoadedLibrary> byNameOnly) {
        Optional<LoadedLibrary> loaded = Optional
                .ofNullable(byNameAndVersion.get(key(vuln.artifactId(), vuln.installedVersion())))
                // 버전까지 일치하는 게 없으면, Agent 가 버전을 모르는 경우에만 이름으로 매칭
                .or(() -> Optional.ofNullable(byNameOnly.get(normalize(vuln.artifactId())))
                        .filter(lib -> lib.version().isBlank()));

        return loaded
                .map(lib -> new Finding(vuln, Reachability.REACHABLE, lib.loadedClassCount()))
                .orElseGet(() -> new Finding(vuln, Reachability.NOT_LOADED, 0));
    }

    private static String key(String artifactId, String version) {
        return normalize(artifactId) + ":" + version.trim();
    }

    private static String normalize(String artifactId) {
        return artifactId.trim().toLowerCase(Locale.ROOT);
    }

    /** 심각도 정렬 순서 (숫자가 작을수록 앞) */
    private static int severityRank(String severity) {
        return switch (severity.toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            default -> 4;
        };
    }
}
