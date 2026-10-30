package com.teams.depscan.domain;

/**
 * 교차 분석 결과: 취약 라이브러리가 실제로 실행되었는가?
 */
public enum Reachability {

    /** 실행 중 로드됨 → 실제 위험, 즉시 패치 대상 (대시보드 빨간색) */
    REACHABLE,

    /** 포함은 되어 있지만 실행 중 로드되지 않음 → 우선순위 낮음 (대시보드 회색) */
    NOT_LOADED
}
