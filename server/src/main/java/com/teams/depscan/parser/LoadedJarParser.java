package com.teams.depscan.parser;

import com.teams.depscan.domain.LoadedLibrary;
import com.teams.depscan.exception.InvalidReportException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Agent 가 만든 loaded-jars.txt 를 읽어서 {@link LoadedLibrary} 목록으로 변환합니다.
 *
 * <p>파일 형식은 agent 모듈의 ReportWriter 와 반드시 같아야 합니다.</p>
 * <pre>
 * # 로 시작하는 줄은 주석
 * artifactId \t version \t loadedClassCount \t location
 * </pre>
 */
@Component
public class LoadedJarParser {

    private static final int COLUMN_COUNT = 4;

    public List<LoadedLibrary> parse(InputStream text) {
        List<LoadedLibrary> libraries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(text, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue; // 빈 줄, 주석 건너뛰기
                }

                // limit=4: 경로(location)에 탭이 있어도 마지막 칸에 통째로 들어가게
                String[] cols = line.split("\t", COLUMN_COUNT);
                if (cols.length < COLUMN_COUNT) {
                    throw new InvalidReportException(
                            "loaded-jars.txt " + lineNumber + "번째 줄 형식이 잘못되었습니다: " + line);
                }

                libraries.add(new LoadedLibrary(
                        cols[0].trim(),
                        cols[1].trim(),
                        parseCount(cols[2].trim(), lineNumber),
                        cols[3].trim()
                ));
            }
        } catch (IOException e) {
            throw new InvalidReportException("loaded-jars.txt 를 읽을 수 없습니다.", e);
        }
        return libraries;
    }

    private long parseCount(String value, int lineNumber) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new InvalidReportException(
                    "loaded-jars.txt " + lineNumber + "번째 줄의 클래스 수가 숫자가 아닙니다: " + value, e);
        }
    }
}
