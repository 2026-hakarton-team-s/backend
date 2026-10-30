package com.teams.depscan.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 설정.
 *
 * <p>프론트(React + Vite)는 개발 중에 localhost:5173 에서 돌고, 서버는 localhost:8080 에서 돌기 때문에
 * 브라우저가 "다른 출처"로 보고 요청을 막습니다. 여기서 프론트 주소를 허용해 줍니다.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173") // TODO(프론트 담당): 포트가 다르면 수정
                .allowedMethods("GET", "POST");
    }
}
