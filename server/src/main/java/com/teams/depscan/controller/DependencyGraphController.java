package com.teams.depscan.controller;

import com.teams.depscan.dto.DependencyGraphResponse;
import com.teams.depscan.exception.InvalidReportException;
import com.teams.depscan.service.DependencyGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 의존성 그래프 API (프론트 mockDependencyGraph 대체용).
 *
 * <pre>
 * POST /api/dependency-graph          deps.txt 업로드 → 그래프 반환
 * GET  /api/dependency-graph/latest   가장 최근 그래프
 *
 * 공통 옵션: ?vulnerablePathsOnly=true  → 취약 라이브러리로 가는 경로만 (발표 화면용)
 * </pre>
 *
 * <p>업로드 예시 (Windows PowerShell 은 curl.exe 로):</p>
 * <pre>
 * curl.exe -F "dependencyTree=@sample-app/deps.txt" http://localhost:8080/api/dependency-graph
 * </pre>
 *
 * <p>vulnerable / reachable 색칠은 가장 최근 POST /api/analyses 결과를 사용합니다.
 * 교차 분석을 먼저 해두면 색이 칠해지고, 안 했으면 트리 모양만 나옵니다.</p>
 */
@RestController
@RequestMapping("/api/dependency-graph")
@RequiredArgsConstructor
public class DependencyGraphController {

    private final DependencyGraphService dependencyGraphService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DependencyGraphResponse upload(@RequestPart("dependencyTree") MultipartFile dependencyTree,
                                          @RequestParam(defaultValue = "false") boolean vulnerablePathsOnly) {
        try (InputStream text = dependencyTree.getInputStream()) {
            return dependencyGraphService.upload(text, vulnerablePathsOnly);
        } catch (IOException e) {
            throw new InvalidReportException("업로드된 deps.txt 를 읽을 수 없습니다.", e);
        }
    }

    @GetMapping("/latest")
    public DependencyGraphResponse latest(@RequestParam(defaultValue = "false") boolean vulnerablePathsOnly) {
        return dependencyGraphService.buildLatest(vulnerablePathsOnly);
    }
}
