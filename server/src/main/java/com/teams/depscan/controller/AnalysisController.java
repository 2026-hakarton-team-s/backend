package com.teams.depscan.controller;

import com.teams.depscan.domain.AnalysisResult;
import com.teams.depscan.dto.AnalysisResponse;
import com.teams.depscan.exception.InvalidReportException;
import com.teams.depscan.service.AnalysisService;
import com.teams.depscan.service.StaticScanService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * 분석 API.
 *
 * <pre>
 * POST /api/analyses              파일 업로드 → 교차 분석 실행 (loadedJars 는 생략 가능)
 * POST /api/analyses/agent-report Agent 자동 전송 → 최근 정적 결과와 교차 분석
 * POST /api/analyses/static-scan  앱 jar 경로 전달 → 서버에서 Trivy 스캔
 * GET  /api/analyses/latest   가장 최근 결과 (대시보드 첫 화면)
 * GET  /api/analyses/{id}     특정 결과 조회
 * </pre>
 *
 * <p>업로드 예시 (터미널에서 테스트):</p>
 * <pre>
 * curl -F "staticReport=@static.json" -F "loadedJars=@loaded-jars.txt" \
 *      http://localhost:8080/api/analyses
 * </pre>
 */
@RestController
@RequestMapping("/api/analyses")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;
    private final StaticScanService staticScanService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse analyze(@RequestPart("staticReport") MultipartFile staticReport,
                                    @RequestPart(value = "loadedJars", required = false) MultipartFile loadedJars) {
        // try-with-resources: 사용이 끝나면 스트림 자동으로 닫기
        try (InputStream trivyJson = staticReport.getInputStream();
             InputStream loadedJarsText = loadedJars == null ? null : loadedJars.getInputStream()) {

            AnalysisResult result = analysisService.analyze(trivyJson, loadedJarsText);
            return AnalysisResponse.from(result);

        } catch (IOException e) {
            throw new InvalidReportException("업로드된 파일을 읽을 수 없습니다.", e);
        }
    }

    /** Agent 가 자동 전송한 로드 기록을 가장 최근 정적 결과와 교차 분석합니다. */
    @PostMapping(value = "/agent-report", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<AnalysisResponse> receiveAgentReport(@RequestBody String report) {
        InputStream loadedJarsText = new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8));
        return analysisService.analyzeAgentReport(loadedJarsText)
                .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(AnalysisResponse.from(result)))
                // 아직 Trivy 스캔 중: 기록은 보관했고 스캔이 끝나면 함께 분석됨
                .orElseGet(() -> ResponseEntity.accepted().build());
    }

    /** Agent 가 알려준 앱 jar 를 서버에서 Trivy 로 스캔합니다. 스캔은 백그라운드로 진행됩니다. */
    @PostMapping(value = "/static-scan", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestStaticScan(@RequestBody String jarPath, HttpServletRequest request) {
        if (!isLocalRequest(request)) {
            throw new InvalidReportException("Trivy 스캔은 서버와 같은 PC 에서만 요청할 수 있습니다.");
        }
        staticScanService.requestScan(jarPath);
    }

    /** 서버 PC 의 프로그램을 실행하는 API 라서 외부 요청을 막습니다. */
    private static boolean isLocalRequest(HttpServletRequest request) {
        try {
            return InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }

    @GetMapping("/latest")
    public AnalysisResponse latest() {
        return AnalysisResponse.from(analysisService.getLatest());
    }

    @GetMapping("/{id}")
    public AnalysisResponse get(@PathVariable Long id) {
        return AnalysisResponse.from(analysisService.get(id));
    }
}
