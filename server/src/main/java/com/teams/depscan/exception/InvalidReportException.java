package com.teams.depscan.exception;

/**
 * 업로드된 파일(static.json, loaded-jars.txt)의 형식이 잘못되었을 때 발생합니다. → 400 응답
 */
public class InvalidReportException extends RuntimeException {

    public InvalidReportException(String message) {
        super(message);
    }

    public InvalidReportException(String message, Throwable cause) {
        super(message, cause);
    }
}
