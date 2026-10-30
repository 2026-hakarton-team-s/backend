package com.teams.depscan.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * jar 경로 파싱 테스트.
 * 새로운 경로 형식을 발견하면 여기에 케이스를 추가하세요.
 */
class JarNameParserTest {

    @Test
    void 일반_jar_경로() {
        var info = JarNameParser.parse("file:/home/user/libs/snakeyaml-1.30.jar").orElseThrow();
        assertEquals("snakeyaml", info.artifactId());
        assertEquals("1.30", info.version());
    }

    @Test
    void 스프링부트2_실행jar_내부_경로() {
        var info = JarNameParser.parse(
                "jar:file:/app/app.jar!/BOOT-INF/lib/jackson-databind-2.13.4.2.jar!/").orElseThrow();
        assertEquals("jackson-databind", info.artifactId());
        assertEquals("2.13.4.2", info.version());
    }

    @Test
    void 스프링부트3_실행jar_내부_경로() {
        var info = JarNameParser.parse(
                "jar:nested:/app/app.jar/!BOOT-INF/lib/spring-boot-starter-web-3.2.0.jar!/").orElseThrow();
        assertEquals("spring-boot-starter-web", info.artifactId());
        assertEquals("3.2.0", info.version());
    }

    @Test
    void 클래스_폴더는_무시() {
        assertTrue(JarNameParser.parse("file:/app/build/classes/java/main/").isEmpty());
    }
}
