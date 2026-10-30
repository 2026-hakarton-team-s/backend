package com.teams.depscan.dto;

import com.teams.depscan.domain.Finding;
import com.teams.depscan.domain.Reachability;
import com.teams.depscan.domain.Vulnerability;

/**
 * 취약점 1건 응답. 프론트에서 쓰기 편하도록 중첩 없이 평평하게 펼쳤습니다.
 */
public record FindingResponse(
        String cveId,
        String library,          // "org.yaml:snakeyaml"
        String installedVersion,
        String fixedVersion,
        String severity,
        String title,
        String referenceUrl,
        Reachability reachability, // REACHABLE(빨강) / NOT_LOADED(회색)
        long loadedClassCount
) {

    public static FindingResponse from(Finding finding) {
        Vulnerability v = finding.vulnerability();
        String library = v.groupId().isBlank() ? v.artifactId() : v.groupId() + ":" + v.artifactId();
        return new FindingResponse(
                v.cveId(),
                library,
                v.installedVersion(),
                v.fixedVersion(),
                v.severity(),
                v.title(),
                v.referenceUrl(),
                finding.reachability(),
                finding.loadedClassCount()
        );
    }
}
