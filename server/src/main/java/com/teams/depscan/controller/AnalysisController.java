package com.teams.depscan.controller;

import com.teams.depscan.domain.AnalysisResult;
import com.teams.depscan.dto.AnalysisResponse;
import com.teams.depscan.exception.InvalidReportException;
import com.teams.depscan.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.nio.charset.StandardCharsets;

/**
 * 분석 API.
 *
 * <pre>
 * POST /api/analyses              파일 업로드 → 교차 분석 실행 (loadedJars 는 생략 가능)
 * POST /api/analyses/agent-report Agent 자동 전송 → 최근 정적 결과와 교차 분석
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
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResponse receiveAgentReport(@RequestBody String report) {
        InputStream loadedJarsText = new ByteArrayInputStream(report.getBytes(StandardCharsets.UTF_8));
        return AnalysisResponse.from(analysisService.analyzeAgentReport(loadedJarsText));
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
