package com.teams.depscan.service;

import com.teams.depscan.domain.AnalysisResult;
import com.teams.depscan.domain.DependencyTree;
import com.teams.depscan.domain.Finding;
import com.teams.depscan.domain.Reachability;
import com.teams.depscan.domain.Vulnerability;
import com.teams.depscan.dto.DependencyGraphResponse;
import com.teams.depscan.dto.DependencyGraphResponse.EdgeResponse;
import com.teams.depscan.dto.DependencyGraphResponse.NodeResponse;
import com.teams.depscan.exception.AnalysisNotFoundException;
import com.teams.depscan.parser.GradleDependencyTreeParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 의존성 그래프 = (Gradle 의존성 트리) + (가장 최근 교차 분석 결과의 취약/실행 여부).
 *
 * <pre>
 * deps.txt ──(GradleDependencyTreeParser)──▶ 노드 + 연결선
 *                                              │
 * AnalysisService.getLatest() ──────────────▶ vulnerable / reachable 색칠
 * </pre>
 *
 * <p>저장: AnalysisService 와 같이 메모리에 "가장 최근 트리 1개"만 보관합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class DependencyGraphService {

    private static final List<String> SEVERITY_ORDER = List.of("CRITICAL", "HIGH", "MEDIUM", "LOW", "UNKNOWN");

    private final GradleDependencyTreeParser parser;
    private final AnalysisService analysisService;

    private final AtomicReference<DependencyTree> latestTree = new AtomicReference<>();

    /** deps.txt 업로드 → 저장 후 바로 그래프 반환 */
    public DependencyGraphResponse upload(InputStream depsText, boolean vulnerablePathsOnly) {
        latestTree.set(parser.parse(depsText));
        return buildLatest(vulnerablePathsOnly);
    }

    /**
     * 저장된 트리 + 최근 분석 결과로 그래프를 만듭니다.
     *
     * @param vulnerablePathsOnly true 면 "앱 → … → 취약 라이브러리" 경로에 있는 노드만 남깁니다.
     *                            (전체는 30개 남짓이라 발표 화면에서는 이쪽이 읽기 쉬움)
     */
    public DependencyGraphResponse buildLatest(boolean vulnerablePathsOnly) {
        DependencyTree tree = latestTree.get();
        if (tree == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "업로드된 의존성 트리가 없습니다. POST /api/dependency-graph 로 deps.txt 를 먼저 올리세요.");
        }

        Map<String, LibraryStatus> statusByLibrary = collectStatus();

        Set<String> keep = vulnerablePathsOnly
                ? nodesOnPathsToVulnerable(tree, statusByLibrary)
                : null; // null = 전부 표시

        List<NodeResponse> nodes = new ArrayList<>();
        nodes.add(new NodeResponse(DependencyTree.ROOT_ID, tree.rootName(), true,
                false, false, null, 0, null));

        for (DependencyTree.Node n : tree.nodes()) {
            if (keep != null && !keep.contains(n.key())) {
                continue;
            }
            LibraryStatus s = statusByLibrary.get(n.key());
            boolean vulnerable = s != null;
            String label = vulnerable ? n.artifactId() + "\n" + n.version() : n.artifactId();
            nodes.add(new NodeResponse(
                    n.key(),
                    label,
                    null,
                    vulnerable,
                    vulnerable && s.reachable,
                    n.version(),
                    vulnerable ? s.count : 0,
                    vulnerable ? s.maxSeverity : null));
        }

        List<EdgeResponse> edges = tree.edges().stream()
                .filter(e -> keep == null || (isKept(e.source(), keep) && isKept(e.target(), keep)))
                .map(e -> new EdgeResponse(e.source(), e.target()))
                .toList();

        return new DependencyGraphResponse(nodes, edges);
    }

    /** 최근 분석 결과에서 라이브러리별 취약점 수 / 최고 심각도 / 실행 여부를 모읍니다. 분석 전이면 빈 Map. */
    private Map<String, LibraryStatus> collectStatus() {
        AnalysisResult latest;
        try {
            latest = analysisService.getLatest();
        } catch (AnalysisNotFoundException e) {
            return Map.of(); // 아직 교차 분석 전: 트리 모양만 보여줌
        }

        Map<String, LibraryStatus> result = new HashMap<>();
        for (Finding f : latest.findings()) {
            Vulnerability v = f.vulnerability();
            String key = v.groupId() + ":" + v.artifactId();
            LibraryStatus s = result.computeIfAbsent(key, k -> new LibraryStatus());
            s.count++;
            s.reachable |= f.reachability() == Reachability.REACHABLE;
            s.maxSeverity = higher(s.maxSeverity, v.severity());
        }
        return result;
    }

    /** 취약 노드에서 연결선을 거꾸로 따라 올라가며 경로상의 모든 노드를 모읍니다. */
    private Set<String> nodesOnPathsToVulnerable(DependencyTree tree, Map<String, LibraryStatus> status) {
        Map<String, List<String>> parentsOf = new HashMap<>();
        for (DependencyTree.Edge e : tree.edges()) {
            parentsOf.computeIfAbsent(e.target(), k -> new ArrayList<>()).add(e.source());
        }

        Set<String> keep = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();
        for (DependencyTree.Node n : tree.nodes()) {
            if (status.containsKey(n.key())) {
                queue.add(n.key());
            }
        }
        while (!queue.isEmpty()) {
            String id = queue.poll();
            if (!keep.add(id)) {
                continue;
            }
            for (String parent : parentsOf.getOrDefault(id, List.of())) {
                if (!parent.equals(DependencyTree.ROOT_ID)) {
                    queue.add(parent);
                }
            }
        }
        return keep;
    }

    private static boolean isKept(String id, Set<String> keep) {
        return id.equals(DependencyTree.ROOT_ID) || keep.contains(id);
    }

    private static String higher(String current, String candidate) {
        String c = candidate == null ? "UNKNOWN" : candidate.toUpperCase(Locale.ROOT);
        if (current == null) {
            return c;
        }
        return rank(c) < rank(current) ? c : current;
    }

    private static int rank(String severity) {
        int i = SEVERITY_ORDER.indexOf(severity);
        return i < 0 ? SEVERITY_ORDER.size() : i;
    }

    /** 라이브러리 하나의 집계 결과 */
    private static final class LibraryStatus {
        int count;
        boolean reachable;
        String maxSeverity;
    }
}
