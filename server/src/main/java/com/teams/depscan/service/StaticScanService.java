package com.teams.depscan.service;

import com.teams.depscan.exception.InvalidReportException;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 서버에서 Trivy 를 직접 실행해 정적 분석 결과를 등록합니다.
 *
 * <p>Trivy 는 처음 실행할 때 DB 를 내려받느라 몇 분이 걸릴 수 있어 백그라운드에서 실행합니다.
 * 스캔이 끝나면 지금까지 받은 Agent 기록과 바로 교차 분석합니다.</p>
 *
 * <p>Agent 가 알려준 경로를 서버가 직접 읽으므로 서버와 앱이 같은 PC 에 있어야 합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StaticScanService {

    private static final long SCAN_TIMEOUT_MINUTES = 10;

    private final AnalysisService analysisService;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** 스캔을 예약합니다. 잘못된 경로는 바로 거절합니다. */
    public void requestScan(String jarPath) {
        Path jar = validate(jarPath);
        executor.submit(() -> scan(jar));
    }

    /** 외부 입력을 프로그램 인자로 넘기므로 실제로 있는 jar 파일의 절대 경로만 허용합니다. */
    public static Path validate(String jarPath) {
        Path jar = Path.of(jarPath.trim());
        if (!jar.isAbsolute() || !jar.toString().endsWith(".jar") || !Files.isRegularFile(jar)) {
            throw new InvalidReportException("스캔할 수 없는 경로입니다. jar 파일의 절대 경로가 필요합니다.");
        }
        return jar;
    }

    private void scan(Path jar) {
        Path report = null;
        try {
            report = Files.createTempFile("depscan-trivy-", ".json");
            Process process = new ProcessBuilder(
                    "trivy", "rootfs", "--scanners", "vuln", "--format", "json",
                    "--output", report.toString(), jar.toString())
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!process.waitFor(SCAN_TIMEOUT_MINUTES, TimeUnit.MINUTES)) {
                process.destroyForcibly();
                log.error("Trivy 스캔 시간 초과: {}", jar);
                return;
            }
            if (process.exitValue() != 0) {
                log.error("Trivy 스캔 실패 (종료 코드 {}): {}", process.exitValue(), jar);
                return;
            }

            try (InputStream trivyJson = Files.newInputStream(report)) {
                analysisService.analyze(trivyJson, null);
            }
            log.info("Trivy 스캔 완료: {}", jar);
        } catch (IOException | InvalidReportException e) {
            log.error("Trivy 스캔 실패: {}", jar, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            deleteQuietly(report);
        }
    }

    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("임시 파일 삭제 실패: {}", file);
        }
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
