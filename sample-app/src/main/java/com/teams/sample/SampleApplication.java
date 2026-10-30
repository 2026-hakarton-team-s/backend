package com.teams.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 데모용 취약 샘플 앱.
 *
 * <p>실행 순서:</p>
 * <pre>
 * 1) 빌드      : cd sample-app && ../gradlew bootJar
 * 2) 정적 분석 : trivy fs --format json -o static.json build/libs/sample-app.jar
 * 3) 동적 분석 : java -javaagent:../agent/build/libs/depscan-agent.jar -jar build/libs/sample-app.jar
 * 4) API 호출  : (아래 컨트롤러 참고) 호출 후 Ctrl+C 로 종료 → loaded-jars.txt 생성
 * </pre>
 */
@SpringBootApplication
public class SampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
}
