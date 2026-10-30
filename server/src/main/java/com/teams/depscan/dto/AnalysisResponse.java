package com.teams.depscan.dto;

import com.teams.depscan.domain.AnalysisResult;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 대시보드에 내려주는 분석 결과.
 *
 * <pre>
 * {
 *   "id": 1,
 *   "createdAt": "2026-11-01T12:00:00",
 *   "summary": { "totalCount": 30, "reachableCount": 3, "notLoadedCount": 27 },
 *   "findings": [ { "cveId": "CVE-2022-1471", "reachability": "REACHABLE", ... }, ... ]
 * }
 * </pre>
 *
 * <p>프론트 담당과 이 구조를 먼저 합의해두면 서로 기다리지 않고 동시에 개발할 수 있습니다.</p>
 */
public record AnalysisResponse(
        Long id,
        LocalDateTime createdAt,
        Summary summary,
        List<FindingResponse> findings
) {

    /** "경고 30개 → 실제 위험 3개" 차트용 숫자 */
    public record Summary(long totalCount, long reachableCount, long notLoadedCount) {
    }

    public static AnalysisResponse from(AnalysisResult result) {
        long total = result.totalCount();
        long reachable = result.reachableCount();
        return new AnalysisResponse(
                result.id(),
                result.createdAt(),
                new Summary(total, reachable, total - reachable),
                result.findings().stream().map(FindingResponse::from).toList()
        );
    }
}
