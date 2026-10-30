package com.teams.depscan.exception;

/**
 * 요청한 분석 결과가 없을 때 발생합니다. → 404 응답
 */
public class AnalysisNotFoundException extends RuntimeException {

    public AnalysisNotFoundException(Long id) {
        super(id == null
                ? "아직 분석 결과가 없습니다. 먼저 파일을 업로드하세요."
                : "분석 결과를 찾을 수 없습니다. id=" + id);
    }
}
