package com.teams.depscan.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Agent 인자 해석 테스트.
 */
class AgentOptionTest {

    @Test
    void 서버_주소와_출력_파일을_함께_해석() {
        String args = "server=http://localhost:8080,output=result.txt";

        assertEquals("http://localhost:8080", DepScanAgent.parseOption(args, "server"));
        assertEquals("result.txt", DepScanAgent.parseOutputPath(args));
    }

    @Test
    void 인자가_없으면_서버_전송_안함() {
        assertNull(DepScanAgent.parseOption(null, "server"));
        assertEquals("loaded-jars.txt", DepScanAgent.parseOutputPath(null));
    }

    @Test
    void 주소_끝의_슬래시는_무시() {
        assertEquals("http://localhost:8080/api/analyses/agent-report",
                ReportSender.toEndpoint("http://localhost:8080/", "/api/analyses/agent-report").toString());
    }

    @Test
    void Agent_를_빼고_앱_jar_를_찾음() {
        assertEquals("/app/sample-app.jar",
                DepScanAgent.findAppJar("/app/sample-app.jar:/tools/depscan-agent.jar"));
    }

    @Test
    void 앱이_jar_하나로_실행되지_않으면_찾지_않음() {
        assertNull(DepScanAgent.findAppJar("/app/classes:/libs/a.jar:/libs/b.jar"));
        assertNull(DepScanAgent.findAppJar("/app/build/classes"));
        assertNull(DepScanAgent.findAppJar(null));
    }
}
