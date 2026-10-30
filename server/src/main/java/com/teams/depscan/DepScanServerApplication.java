package com.teams.depscan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * DepScan 분석 서버 진입점.
 * 실행: ./gradlew :server:bootRun  →  http://localhost:8080
 */
@SpringBootApplication
public class DepScanServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DepScanServerApplication.class, args);
    }
}
