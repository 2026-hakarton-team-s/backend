package com.teams.depscan;

import com.teams.depscan.domain.Finding;
import com.teams.depscan.domain.LoadedLibrary;
import com.teams.depscan.domain.Reachability;
import com.teams.depscan.domain.Vulnerability;
import com.teams.depscan.service.ReachabilityMatcher;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 핵심 교차 분석 로직 테스트 (Spring 을 띄우지 않는 순수 단위 테스트).
 */
class ReachabilityMatcherTest {

    private final ReachabilityMatcher matcher = new ReachabilityMatcher();

    @Test
    void 로드된_라이브러리의_취약점만_REACHABLE() {
        // 정적 분석: 취약점 2개
        List<Vulnerability> vulns = List.of(
                vuln("CVE-2022-1471", "org.yaml", "snakeyaml", "1.30", "HIGH"),
                vuln("CVE-0000-0001", "com.example", "unused-lib", "1.0", "CRITICAL")
        );
        // 동적 분석: snakeyaml 만 실제로 로드됨
        List<LoadedLibrary> loaded = List.of(
                new LoadedLibrary("snakeyaml", "1.30", 42, "jar:...snakeyaml-1.30.jar!/")
        );

        List<Finding> findings = matcher.match(vulns, loaded);

        // 실행된 snakeyaml 이 (심각도가 더 낮아도) 맨 앞에 와야 함
        assertEquals("snakeyaml", findings.get(0).vulnerability().artifactId());
        assertEquals(Reachability.REACHABLE, findings.get(0).reachability());
        assertEquals(42, findings.get(0).loadedClassCount());

        assertEquals(Reachability.NOT_LOADED, findings.get(1).reachability());
    }

    @Test
    void 버전이_다르면_REACHABLE_아님() {
        List<Vulnerability> vulns = List.of(vuln("CVE-2022-1471", "org.yaml", "snakeyaml", "1.30", "HIGH"));
        List<LoadedLibrary> loaded = List.of(new LoadedLibrary("snakeyaml", "2.2", 10, "..."));

        assertEquals(Reachability.NOT_LOADED, matcher.match(vulns, loaded).get(0).reachability());
    }

    private static Vulnerability vuln(String cve, String group, String artifact, String version, String severity) {
        return new Vulnerability(cve, group, artifact, version, "", severity, "", "");
    }
}
