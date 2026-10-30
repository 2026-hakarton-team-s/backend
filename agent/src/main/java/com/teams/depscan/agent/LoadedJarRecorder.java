package com.teams.depscan.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 로드된 jar 경로를 중복 없이 모아두는 저장소.
 *
 * <p>여러 스레드가 동시에 클래스를 로드하므로 thread-safe 한
 * {@link ConcurrentHashMap} 을 사용합니다.</p>
 *
 * <p>key   = jar 경로 원문 (예: jar:nested:/app.jar/!BOOT-INF/lib/snakeyaml-1.30.jar!/)<br>
 *    value = 그 jar 에서 로드된 클래스 개수 (대시보드에서 "얼마나 쓰였나" 표시용)</p>
 */
final class LoadedJarRecorder {

    private final Map<String, AtomicLong> classCountByLocation = new ConcurrentHashMap<>();

    /** 클래스 1개가 해당 위치(jar)에서 로드되었음을 기록 */
    void record(String location) {
        classCountByLocation
                .computeIfAbsent(location, key -> new AtomicLong())
                .incrementAndGet();
    }

    /** 현재까지 기록된 내용을 복사해서 반환 (파일 저장 시 사용) */
    Map<String, Long> snapshot() {
        Map<String, Long> copy = new ConcurrentHashMap<>();
        classCountByLocation.forEach((location, count) -> copy.put(location, count.get()));
        return copy;
    }
}
