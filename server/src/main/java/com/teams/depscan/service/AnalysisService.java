package com.teams.depscan.service;

import com.teams.depscan.domain.AnalysisResult;
import com.teams.depscan.domain.Finding;
import com.teams.depscan.domain.LoadedLibrary;
import com.teams.depscan.domain.Vulnerability;
import com.teams.depscan.exception.AnalysisNotFoundException;
import com.teams.depscan.parser.LoadedJarParser;
import com.teams.depscan.parser.TrivyReportParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 분석 흐름 전체를 조율합니다.
 *
 * <pre>
 * static.json      ──(TrivyReportParser)──▶ 취약점 30개
 * loaded-jars.txt  ──(LoadedJarParser)───▶ 실제 로드된 jar 목록
 *                                  ↓
 *                       (ReachabilityMatcher)
 *                                  ↓
 *                   실제 실행되는 취약점 3개 + 나머지 27개
 * </pre>
 *
 * <p>저장소: 해커톤 1차 버전은 메모리(Map)에 저장합니다. 서버를 재시작하면 사라집니다.
 * TODO(분석 담당): 필요하면 H2 + JPA 로 교체 (server/build.gradle 주석 참고)</p>
 */
@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final TrivyReportParser trivyReportParser;
    private final LoadedJarParser loadedJarParser;
    private final ReachabilityMatcher reachabilityMatcher;

    /** 임시 저장소: 분석 ID → 결과 */
    private final Map<Long, AnalysisResult> store = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    /** 가장 최근 정적 분석 결과. Agent 가 보낸 기록은 이 결과와 비교합니다. */
    private volatile List<Vulnerability> latestVulnerabilities;

    /** 가장 최근 동적 결과. Trivy 스캔보다 Agent 기록이 먼저 도착해도 잃어버리지 않도록 보관합니다. */
    private volatile List<LoadedLibrary> latestLoadedLibraries = List.of();

    /**
     * 정적 결과와 동적 결과를 받아 교차 분석을 수행하고 저장합니다.
     *
     * @param loadedJarsText 동적 결과. null 이면 가장 최근 Agent 기록을 사용합니다.
     */
    public AnalysisResult analyze(InputStream trivyJson, InputStream loadedJarsText) {
        latestVulnerabilities = trivyReportParser.parse(trivyJson);
        if (loadedJarsText != null) {
            latestLoadedLibraries = loadedJarParser.parse(loadedJarsText);
        }
        return match(latestVulnerabilities, latestLoadedLibraries);
    }

    /**
     * Agent 가 자동 전송한 기록을 가장 최근 정적 결과와 교차 분석합니다.
     *
     * @return 아직 정적 결과가 없으면 empty. 기록은 보관했다가 스캔이 끝나면 함께 분석합니다.
     */
    public Optional<AnalysisResult> analyzeAgentReport(InputStream loadedJarsText) {
        latestLoadedLibraries = loadedJarParser.parse(loadedJarsText);
        List<Vulnerability> vulnerabilities = latestVulnerabilities;
        if (vulnerabilities == null) {
            return Optional.empty();
        }
        return Optional.of(match(vulnerabilities, latestLoadedLibraries));
    }

    private AnalysisResult match(List<Vulnerability> vulnerabilities, List<LoadedLibrary> loadedLibraries) {
        List<Finding> findings = reachabilityMatcher.match(vulnerabilities, loadedLibraries);

        long id = idSequence.incrementAndGet();
        AnalysisResult result = new AnalysisResult(id, LocalDateTime.now(), findings);
        store.put(id, result);
        return result;
    }

    public AnalysisResult get(Long id) {
        AnalysisResult result = store.get(id);
        if (result == null) {
            throw new AnalysisNotFoundException(id);
        }
        return result;
    }

    /** 가장 최근 분석 결과 (대시보드 첫 화면용) */
    public AnalysisResult getLatest() {
        long latestId = idSequence.get();
        if (latestId == 0) {
            throw new AnalysisNotFoundException(null);
        }
        return get(latestId);
    }
}
