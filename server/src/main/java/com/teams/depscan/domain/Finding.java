package com.teams.depscan.domain;

/**
 * 최종 분석 결과 1건 = 취약점 + "실제로 실행됐는지" 판정.
 *
 * @param vulnerability    정적 분석 결과
 * @param reachability     실행 여부 판정
 * @param loadedClassCount 실행된 경우 로드된 클래스 수 (실행 안 됐으면 0)
 */
public record Finding(
        Vulnerability vulnerability,
        Reachability reachability,
        long loadedClassCount
) {
}
