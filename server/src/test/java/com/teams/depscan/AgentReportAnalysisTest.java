package com.teams.depscan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teams.depscan.domain.AnalysisResult;
import com.teams.depscan.domain.Reachability;
import com.teams.depscan.exception.InvalidReportException;
import com.teams.depscan.parser.LoadedJarParser;
import com.teams.depscan.parser.TrivyReportParser;
import com.teams.depscan.service.AnalysisService;
import com.teams.depscan.service.ReachabilityMatcher;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Agent 자동 전송 분석 흐름 테스트 (Spring 을 띄우지 않는 순수 단위 테스트).
 */
class AgentReportAnalysisTest {

    private static final String STATIC_REPORT = """
            {"Results":[{"Vulnerabilities":[{"VulnerabilityID":"CVE-2022-1471",
            "PkgName":"org.yaml:snakeyaml","InstalledVersion":"1.30","Severity":"HIGH"}]}]}
            """;

    private final AnalysisService service = new AnalysisService(
            new TrivyReportParser(new ObjectMapper()), new LoadedJarParser(), new ReachabilityMatcher());

    @Test
    void 정적_결과가_없으면_Agent_기록을_분석할_수_없음() {
        assertThrows(InvalidReportException.class, () -> service.analyzeAgentReport(text("snakeyaml\t1.30\t42\tjar")));
    }

    @Test
    void 정적_결과만_등록하면_전부_NOT_LOADED() {
        AnalysisResult result = service.analyze(text(STATIC_REPORT), null);

        assertEquals(Reachability.NOT_LOADED, result.findings().get(0).reachability());
    }

    @Test
    void Agent_기록이_오면_REACHABLE_로_바뀜() {
        service.analyze(text(STATIC_REPORT), null);

        AnalysisResult result = service.analyzeAgentReport(text("snakeyaml\t1.30\t42\tjar"));

        assertEquals(Reachability.REACHABLE, result.findings().get(0).reachability());
        assertEquals(result.id(), service.getLatest().id());
    }

    private static InputStream text(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
