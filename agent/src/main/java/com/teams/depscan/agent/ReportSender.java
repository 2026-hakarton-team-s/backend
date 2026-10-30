package com.teams.depscan.agent;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * 로드 기록을 분석 서버로 자동 전송합니다.
 *
 * <p>앱이 켜져 있는 동안 주기적으로 기록을 확인하고, 내용이 달라졌을 때만 전송합니다.
 * 그래서 API 호출로 새 라이브러리가 로드되면 대시보드에 바로 반영할 수 있습니다.</p>
 *
 * <p>⚠️ 서버가 꺼져 있어도 대상 앱은 정상 동작해야 하므로 전송 실패는 경고만 남기고 넘어갑니다.</p>
 */
final class ReportSender {

    private static final String REPORT_PATH = "/api/analyses/agent-report";
    private static final long SEND_INTERVAL_MILLIS = 3_000;
    private static final Duration TIMEOUT = Duration.ofSeconds(3);

    private final URI endpoint;
    private final HttpClient client;

    private String lastSent = "";
    private boolean failureLogged;

    /**
     * @param serverUrl 분석 서버 주소 (예: http://localhost:8080)
     * @throws IllegalArgumentException 주소 형식이 잘못된 경우
     */
    ReportSender(String serverUrl) {
        this.endpoint = toEndpoint(serverUrl);
        this.client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    static URI toEndpoint(String serverUrl) {
        String base = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        return URI.create(base + REPORT_PATH);
    }

    /** 앱이 끝날 때까지 주기적으로 전송하는 백그라운드 스레드 시작 (데몬이라 앱 종료를 막지 않음) */
    void startPeriodicSend(LoadedJarRecorder recorder) {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(SEND_INTERVAL_MILLIS);
                    sendIfChanged(recorder);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (RuntimeException e) {
                    logFailure(e.getMessage());
                }
            }
        }, "depscan-report-sender");
        thread.setDaemon(true);
        thread.start();
    }

    /** 지난 전송 이후 기록이 달라졌을 때만 서버로 전송합니다. */
    synchronized void sendIfChanged(LoadedJarRecorder recorder) {
        String body = String.join("\n", ReportWriter.dataLines(recorder));
        if (body.equals(lastSent)) {
            return;
        }

        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(TIMEOUT)
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (status / 100 == 2) {
                lastSent = body;
                failureLogged = false;
            } else {
                logFailure("HTTP " + status);
            }
        } catch (IOException e) {
            logFailure(e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 3초마다 같은 경고가 쌓이지 않도록 연속 실패는 한 번만 출력합니다. */
    private void logFailure(String reason) {
        if (!failureLogged) {
            System.err.println("[DepScan] 서버 전송 실패 (앱은 계속 실행됩니다): " + reason);
            failureLogged = true;
        }
    }
}
