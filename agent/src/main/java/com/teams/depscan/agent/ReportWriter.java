package com.teams.depscan.agent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 앱 종료 시 기록 결과를 파일로 저장합니다.
 *
 * <p>서버(server 모듈)의 LoadedJarParser 가 이 파일을 읽으므로,
 * <b>형식을 바꾸면 서버 쪽 파서도 같이 바꿔야 합니다.</b></p>
 *
 * <pre>
 * # DepScan loaded jars
 * # generatedAt=2026-11-01T12:00:00+09:00
 * # artifactId \t version \t loadedClassCount \t location
 * snakeyaml	1.30	42	jar:nested:/app.jar/!BOOT-INF/lib/snakeyaml-1.30.jar!/
 * jackson-databind	2.13.3	310	jar:nested:/app.jar/!BOOT-INF/lib/jackson-databind-2.13.3.jar!/
 * </pre>
 *
 * <p>탭(\t)으로 구분한 이유: 경로에 공백이나 쉼표가 들어가도 안전하게 나누기 위해서입니다.</p>
 */
final class ReportWriter {

    private ReportWriter() {
    }

    static void write(LoadedJarRecorder recorder, String outputPath) {
        List<String> lines = new ArrayList<>();
        lines.add("# DepScan loaded jars");
        lines.add("# generatedAt=" + OffsetDateTime.now());
        lines.add("# artifactId\tversion\tloadedClassCount\tlocation");
        lines.addAll(dataLines(recorder));

        try {
            Path path = Path.of(outputPath);
            Files.write(path, lines, StandardCharsets.UTF_8);
            System.out.println("[DepScan] 로드된 jar " + (lines.size() - 3) + "개 기록 완료 → "
                    + path.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[DepScan] 결과 파일 저장 실패: " + e.getMessage());
        }
    }

    /** 주석을 뺀 기록 줄 목록 (파일 저장과 서버 전송이 같은 형식을 사용) */
    static List<String> dataLines(LoadedJarRecorder recorder) {
        Map<String, Long> snapshot = recorder.snapshot();

        // 보기 좋게 라이브러리 이름순 정렬
        return snapshot.entrySet().stream()
                .map(entry -> toLine(entry.getKey(), entry.getValue()))
                .flatMap(Optional::stream)
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    /** Agent 자기 자신의 jar 이름 (결과에서 제외) */
    private static final String SELF_ARTIFACT_ID = "depscan-agent";

    /** jar 로 판별된 것만 한 줄로 변환 (클래스 폴더, Agent 자신은 제외) */
    private static Optional<String> toLine(String location, long classCount) {
        return JarNameParser.parse(location)
                .filter(info -> !info.artifactId().equals(SELF_ARTIFACT_ID))
                .map(info -> String.join("\t",
                        info.artifactId(),
                        info.version(),
                        String.valueOf(classCount),
                        info.location()));
    }
}
