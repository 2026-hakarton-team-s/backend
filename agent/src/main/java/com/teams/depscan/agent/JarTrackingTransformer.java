package com.teams.depscan.agent;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * 클래스가 로드될 때마다 JVM 이 호출해주는 감시자.
 *
 * <p>원래 ClassFileTransformer 는 "클래스 바이트코드를 수정"하기 위한 인터페이스지만,
 * 우리는 수정하지 않고 <b>관찰만</b> 합니다. 그래서 항상 null 을 반환합니다.
 * (null 반환 = "바이트코드 그대로 사용해 주세요")</p>
 *
 * <p>ProtectionDomain → CodeSource → Location 을 따라가면
 * "이 클래스가 어느 jar 파일에서 왔는지" 알 수 있습니다.</p>
 */
final class JarTrackingTransformer implements ClassFileTransformer {

    private final LoadedJarRecorder recorder;

    JarTrackingTransformer(LoadedJarRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        // ⚠️ 여기서 예외가 터지면 대상 앱이 오작동할 수 있으므로 모든 예외를 삼킵니다.
        try {
            String location = extractLocation(protectionDomain);
            if (location != null) {
                recorder.record(location);
            }
        } catch (Throwable ignored) {
            // 관찰 실패는 무시 (앱 동작이 우선)
        }
        return null; // 바이트코드 수정 안 함
    }

    /**
     * 클래스의 출처(jar 경로)를 문자열로 꺼냅니다.
     * JDK 내부 클래스(java.lang.String 등)는 CodeSource 가 없어서 null 이 반환됩니다.
     */
    private static String extractLocation(ProtectionDomain domain) {
        if (domain == null) {
            return null;
        }
        CodeSource codeSource = domain.getCodeSource();
        if (codeSource == null) {
            return null;
        }
        URL url = codeSource.getLocation();
        return url == null ? null : url.toString();
    }
}
