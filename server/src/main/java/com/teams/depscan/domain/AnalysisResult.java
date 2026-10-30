package com.teams.depscan.domain;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 분석 1회의 전체 결과.
 *
 * @param id         분석 ID
 * @param createdAt  분석 시각
 * @param findings   취약점별 판정 결과 (REACHABLE 이 앞쪽에 오도록 정렬됨)
 */
public record AnalysisResult(
        Long id,
        LocalDateTime createdAt,
        List<Finding> findings
) {

    /** 정적 분석이 경고한 전체 개수 (예: 30) */
    public long totalCount() {
        return findings.size();
    }

    /** 실제로 실행되는 취약점 개수 (예: 3) */
    public long reachableCount() {
        return findings.stream()
                .filter(f -> f.reachability() == Reachability.REACHABLE)
                .count();
    }
}
