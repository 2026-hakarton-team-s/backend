package com.teams.sample.controller;

import org.apache.commons.collections.map.LRUMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 데모 포인트 ④: commons-collections 를 호출할 때만 로드하는 API.
 *
 * <p>이 API 를 호출해야 commons-collections jar 의 클래스가 로드되므로
 * 호출 전에는 NOT_LOADED, 호출 후에는 REACHABLE 로 표시됩니다.</p>
 *
 * <pre>
 * curl http://localhost:8081/api/cache
 * </pre>
 */
@RestController
public class CollectionsController {

    @GetMapping("/api/cache")
    public int cacheSize() {
        LRUMap cache = new LRUMap(2);
        cache.put("name", "depscan");
        return cache.size();
    }
}
