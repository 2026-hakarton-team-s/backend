package com.teams.depscan.agent;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * jar 경로에서 "라이브러리 이름 + 버전"을 뽑아냅니다.
 *
 * <p>Spring Boot 로 빌드한 실행 jar 는 라이브러리를 BOOT-INF/lib/ 안에 jar 째로 넣기 때문에
 * 경로 형식이 여러 가지입니다. (Spring Boot 버전마다 다름)</p>
 *
 * <pre>
 * 일반 jar             : file:/home/user/.m2/.../snakeyaml-1.30.jar
 * Boot 2.x 실행 jar    : jar:file:/app/app.jar!/BOOT-INF/lib/snakeyaml-1.30.jar!/
 * Boot 3.2+ 실행 jar   : jar:nested:/app/app.jar/!BOOT-INF/lib/snakeyaml-1.30.jar!/
 * </pre>
 *
 * <p>어떤 형식이든 "마지막 xxx-버전.jar" 부분만 찾으면 되므로 정규식으로 처리합니다.</p>
 *
 * <p>⚠️ TODO(Agent 담당): sample-app 에 실제로 붙여서 찍히는 경로를 확인하고
 * 여기서 놓치는 형식이 있으면 테스트 케이스를 추가하세요.</p>
 */
final class JarNameParser {

    /**
     * 경로 안의 "마지막 jar 파일 이름"을 찾는 정규식.
     * 예: .../snakeyaml-1.30.jar!/  →  "snakeyaml-1.30"
     */
    private static final Pattern LAST_JAR = Pattern.compile("([^/!]+)\\.jar(?:!/?)?$");

    /**
     * "이름-버전" 을 나누는 정규식.
     * 버전은 "-숫자" 로 시작하는 마지막 부분으로 간주합니다.
     * 예: jackson-databind-2.13.4.2 → name=jackson-databind, version=2.13.4.2
     *     spring-boot-starter-web-2.7.0 → name=spring-boot-starter-web, version=2.7.0
     */
    private static final Pattern NAME_VERSION = Pattern.compile("^(.+?)-(\\d[\\w.\\-]*)$");

    private JarNameParser() {
    }

    /**
     * @param location 클래스 출처 경로 원문
     * @return jar 가 아니거나(예: 클래스 폴더) 형식을 알 수 없으면 empty
     */
    static Optional<JarInfo> parse(String location) {
        if (location == null) {
            return Optional.empty();
        }
        Matcher jarMatcher = LAST_JAR.matcher(location);
        if (!jarMatcher.find()) {
            return Optional.empty(); // build/classes 같은 폴더에서 로드된 클래스 (= 내 앱 코드)
        }

        String fileName = jarMatcher.group(1); // 예: snakeyaml-1.30
        Matcher nv = NAME_VERSION.matcher(fileName);
        if (nv.matches()) {
            return Optional.of(new JarInfo(nv.group(1), nv.group(2), location));
        }
        // 버전 없는 jar (예: app.jar 자체) → 이름만 기록
        return Optional.of(new JarInfo(fileName, "", location));
    }

    /**
     * 파싱 결과.
     *
     * @param artifactId 라이브러리 이름 (예: snakeyaml)
     * @param version    버전 (예: 1.30), 알 수 없으면 빈 문자열
     * @param location   원본 경로 (디버깅용)
     */
    record JarInfo(String artifactId, String version, String location) {
    }
}
