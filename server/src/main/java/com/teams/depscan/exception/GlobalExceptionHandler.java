package com.teams.depscan.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/**
 * 예외를 일관된 JSON 에러 응답으로 바꿔줍니다.
 *
 * <p>Spring 6 의 {@link ProblemDetail} 표준 형식 사용:</p>
 * <pre>
 * { "status": 400, "title": "Bad Request", "detail": "Trivy 결과에 'Results' 배열이 없습니다..." }
 * </pre>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidReportException.class)
    public ProblemDetail handleInvalidReport(InvalidReportException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(AnalysisNotFoundException.class)
    public ProblemDetail handleNotFound(AnalysisNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** 파일 2개 중 하나를 빠뜨리고 업로드한 경우 */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ProblemDetail handleMissingPart(MissingServletRequestPartException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "'" + e.getRequestPartName() + "' 파일이 필요합니다. (staticReport, loadedJars 둘 다 업로드)");
    }
}
