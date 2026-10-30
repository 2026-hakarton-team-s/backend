package com.teams.depscan.domain;

/**
 * 동적 분석(Agent)이 기록한 "실제로 로드된 라이브러리" 1개.
 * agent 모듈의 ReportWriter 가 만든 loaded-jars.txt 의 한 줄에 해당합니다.
 *
 * @param artifactId       라이브러리 이름 (예: snakeyaml)
 * @param version          버전 (예: 1.30), 알 수 없으면 빈 문자열
 * @param loadedClassCount 이 jar 에서 로드된 클래스 수 (많을수록 많이 쓰인 것)
 * @param location         원본 jar 경로
 */
public record LoadedLibrary(
        String artifactId,
        String version,
        long loadedClassCount,
        String location
) {
}
