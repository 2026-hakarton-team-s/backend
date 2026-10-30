package com.teams.depscan.agent;

import java.lang.instrument.Instrumentation;

/**
 * DepScan Agent 진입점.
 *
 * <p>JVM 은 {@code -javaagent:depscan-agent.jar} 옵션이 있으면
 * 앱의 main() 보다 먼저 {@link #premain(String, Instrumentation)} 을 호출합니다.
 * 여기서 "클래스가 로드될 때마다 알려줘" 라고 JVM 에 등록(addTransformer)해 두면,
 * 이후 로드되는 모든 클래스의 출처(jar 경로)를 기록할 수 있습니다.</p>
 *
 * <p>냉장고 비유: 이 클래스가 "부엌 카메라"를 설치하는 역할입니다.</p>
 */
public final class DepScanAgent {

    /** 결과 파일 기본 이름 (앱을 실행한 폴더에 생성됨) */
    private static final String DEFAULT_OUTPUT = "loaded-jars.txt";

    private DepScanAgent() {
        // 유틸리티 클래스: 인스턴스 생성 금지
    }

    /**
     * 앱 시작 시 Agent 를 붙이는 방식 (-javaagent 옵션).
     *
     * @param agentArgs -javaagent:depscan-agent.jar=여기에 오는 문자열 (예: "output=result.txt")
     * @param inst      JVM 이 넘겨주는 계측(Instrumentation) 객체
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        start(agentArgs, inst);
    }

    /**
     * 이미 실행 중인 JVM 에 나중에 붙이는 방식 (Attach API).
     * 해커톤 범위에서는 사용하지 않지만, 향후 확장을 위해 열어둡니다.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) {
        start(agentArgs, inst);
    }

    private static void start(String agentArgs, Instrumentation inst) {
        String outputPath = parseOutputPath(agentArgs);

        // 1) 로드된 jar 를 모아둘 저장소
        LoadedJarRecorder recorder = new LoadedJarRecorder();

        // 2) 클래스가 로드될 때마다 recorder 에 jar 경로를 넘겨주는 감시자 등록
        inst.addTransformer(new JarTrackingTransformer(recorder));

        // 3) 앱이 종료될 때 기록을 파일로 저장 (Ctrl+C, System.exit, 정상 종료 모두 포함)
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> ReportWriter.write(recorder, outputPath), "depscan-report-writer"));

        System.out.println("[DepScan] Agent 시작 - 종료 시 결과 저장 위치: " + outputPath);
    }

    /**
     * "output=파일경로" 형식의 Agent 인자를 해석합니다.
     * 인자가 없으면 기본 파일 이름을 사용합니다.
     */
    static String parseOutputPath(String agentArgs) {
        if (agentArgs == null || agentArgs.isBlank()) {
            return DEFAULT_OUTPUT;
        }
        for (String pair : agentArgs.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].trim().equals("output") && !kv[1].isBlank()) {
                return kv[1].trim();
            }
        }
        return DEFAULT_OUTPUT;
    }
}
