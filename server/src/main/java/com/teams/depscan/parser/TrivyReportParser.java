package com.teams.depscan.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teams.depscan.domain.Vulnerability;
import com.teams.depscan.exception.InvalidReportException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Trivy JSON 결과(static.json)를 읽어서 {@link Vulnerability} 목록으로 변환합니다.
 *
 * <p>생성 명령: {@code trivy fs --format json -o static.json app.jar}</p>
 *
 * <p>Trivy JSON 구조 (필요한 부분만):</p>
 * <pre>
 * {
 *   "Results": [
 *     {
 *       "Target": "app.jar",
 *       "Vulnerabilities": [
 *         {
 *           "VulnerabilityID": "CVE-2022-1471",
 *           "PkgName": "org.yaml:snakeyaml",      ← "groupId:artifactId" 형식
 *           "InstalledVersion": "1.30",
 *           "FixedVersion": "2.0",
 *           "Severity": "HIGH",
 *           "Title": "...",
 *           "PrimaryURL": "https://..."
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * <p>⚠️ TODO(분석 담당): 해커톤 전에 sample-app 에 Trivy 를 실제로 돌려서
 * 필드 이름이 위와 같은지 꼭 확인하세요. Trivy 버전에 따라 달라질 수 있습니다.</p>
 */
@Component
@RequiredArgsConstructor
public class TrivyReportParser {

    private final ObjectMapper objectMapper;

    public List<Vulnerability> parse(InputStream json) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (IOException e) {
            throw new InvalidReportException("Trivy 결과 파일이 올바른 JSON 이 아닙니다.", e);
        }

        JsonNode results = root.path("Results");
        if (!results.isArray()) {
            throw new InvalidReportException("Trivy 결과에 'Results' 배열이 없습니다. trivy --format json 으로 생성했는지 확인하세요.");
        }

        List<Vulnerability> vulnerabilities = new ArrayList<>();
        for (JsonNode result : results) {
            // 취약점이 없는 대상은 "Vulnerabilities" 필드 자체가 없으므로 path() 로 안전하게 접근
            for (JsonNode v : result.path("Vulnerabilities")) {
                vulnerabilities.add(toVulnerability(v));
            }
        }
        return vulnerabilities;
    }

    private Vulnerability toVulnerability(JsonNode v) {
        // "org.yaml:snakeyaml" → groupId="org.yaml", artifactId="snakeyaml"
        String pkgName = v.path("PkgName").asText("");
        String groupId = "";
        String artifactId = pkgName;
        int colon = pkgName.lastIndexOf(':');
        if (colon >= 0) {
            groupId = pkgName.substring(0, colon);
            artifactId = pkgName.substring(colon + 1);
        }

        return new Vulnerability(
                v.path("VulnerabilityID").asText(""),
                groupId,
                artifactId,
                v.path("InstalledVersion").asText(""),
                v.path("FixedVersion").asText(""),
                v.path("Severity").asText("UNKNOWN"),
                v.path("Title").asText(""),
                v.path("PrimaryURL").asText("")
        );
    }
}
