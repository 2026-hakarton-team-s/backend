# DepScan — 런타임 기반 스프링 의존성 취약점 검증 도구

> 기존 도구는 "취약한 라이브러리가 30개 있어요"까지만 알려줍니다.
> DepScan은 **"그중 실제로 실행되는 건 3개예요, 이것부터 고치세요"**라고 알려줍니다.

<br>

## 📌 목차

1. [문제 정의](#-문제-정의)
2. [해결 방법](#-해결-방법)
3. [동작 흐름](#-동작-흐름)
4. [구성 요소](#-구성-요소)
5. [기술 스택](#-기술-스택)
6. [실행 방법](#-실행-방법)
7. [데모 시나리오](#-데모-시나리오)
8. [프로젝트 구조](#-프로젝트-구조)
9. [향후 계획](#-향후-계획)
10. [팀 구성](#-팀-구성)

<br>

## 🚨 문제 정의

### 1. 내가 어떤 라이브러리를 쓰는지 모른다

스프링 프로젝트에 `spring-boot-starter-web` 하나만 추가해도, 그 라이브러리가 사용하는 다른 라이브러리(**전이 의존성**)가 줄줄이 함께 들어옵니다.

```
my-spring-app
└── spring-boot-starter-web          ← 내가 추가한 것
    ├── spring-boot-starter-json
    │   └── jackson-databind         ← 모르는 사이에 들어옴
    └── spring-boot-starter-tomcat
        └── tomcat-embed-core        ← 모르는 사이에 들어옴
```

개발자는 직접 추가한 몇 개만 알고, 실제로 포함된 수십~수백 개의 라이브러리는 파악하지 못합니다.

### 2. 경고가 너무 많아서 진짜 위험을 놓친다 (경고 피로)

기존 SCA 도구(Dependabot, Trivy 등)는 버전만 보고 취약점을 경고하기 때문에 경고가 수십 개씩 쏟아집니다.
하지만 대부분은 **포함만 되어 있고 실제로는 실행되지 않는** 코드입니다.

결국 개발자들은 경고를 무시하게 되고, 그 안에 섞인 **진짜 위험한 취약점**까지 함께 놓칩니다.

### 목표

> **스프링 개발자가 경고 피로로 놓치는, 실제로 실행되는 취약 라이브러리를 배포 전에 찾아내서, 공격자가 악용하기 전에 우선 패치하도록 만든다.**

<br>

## 💡 해결 방법

**정적 분석**(기존 도구)과 **동적 분석**(직접 제작)을 결합합니다.

| 단계 | 방법 | 결과 |
|---|---|---|
| 정적 분석 | 기존 SCA 도구(Trivy)로 의존성 트리와 CVE 매칭 | 취약 라이브러리 **30개** 경고 |
| 동적 분석 | 직접 만든 **Java Agent**로 실행 중 실제 로드된 라이브러리 기록 | 실제 사용된 라이브러리 목록 |
| 교차 분석 | 두 결과를 비교 | 실제 실행되는 취약점 **3개**만 우선순위로 표시 |

### 냉장고로 비유하면

- **정적 분석** = 냉장고 재료 목록과 리콜 목록 비교 → "문제 재료 30개 있음"
- **동적 분석** = 부엌 카메라로 실제 요리에 쓰는 재료 기록
- **교차 분석** = "30개 중 실제로 먹고 있는 3개부터 버리세요"

<br>

## 🔄 동작 흐름

```
┌──────────────────────┐
│  스프링 앱 (분석 대상)  │
└──────────┬───────────┘
           │
     ┌─────┴──────────────────────────┐
     ▼                                ▼
┌─────────────────┐        ┌─────────────────────┐
│ [정적] Trivy     │        │ [동적] DepScan Agent │
│ jar 파일 스캔    │        │ 앱 실행 중 로드된     │
│ → CVE 매칭       │        │ jar 기록             │
└────────┬────────┘        └──────────┬──────────┘
         │ static.json                │ loaded-jars.txt
         └──────────────┬─────────────┘
                        ▼
           ┌─────────────────────────┐
           │ [교차 분석] DepScan 서버  │
           │ 두 결과를 매칭            │
           └────────────┬────────────┘
                        ▼
           ┌─────────────────────────┐
           │ 대시보드                  │
           │ 경고 30개 → 실제 위험 3개 │
           └─────────────────────────┘
```

<br>

## 🧩 구성 요소

| 구성 요소 | 설명 | 제작 방식 |
|---|---|---|
| **정적 스캐너** | 의존성 트리 추출 및 CVE 매칭 | 기존 도구 활용 (Trivy) |
| **DepScan Agent** | 앱 실행 중 실제 로드된 클래스·jar 기록 (동적 탐지 핵심) | 직접 제작 |
| **분석 서버 + 대시보드** | 정적·동적 결과 교차 분석 및 시각화 | 직접 제작 |
| **샘플 취약 앱** | 취약점이 알려진 구버전 Spring Boot 앱 (검증용 정답지) | 직접 제작 |

### 왜 정적 분석은 기존 도구를 쓰나요?

정적 분석은 이미 검증된 오픈소스 도구가 많습니다. 저희는 바퀴를 다시 만들지 않고, **기존 도구가 하지 못하는 "실제로 실행되는가?"를 판단하는 계층**에 집중합니다.

<br>

## 🛠 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Agent | Java Instrumentation API (`java.lang.instrument`) |
| 정적 분석 | Trivy |
| 취약점 DB | OSV (Open Source Vulnerabilities) |
| Build | Gradle |
| Frontend | (팀 결정 후 기재) |

<br>

## 🚀 실행 방법

### 0. 최초 1회: Gradle Wrapper 생성

```bash
# Gradle 이 설치되어 있다면 (또는 IntelliJ 로 열면 자동 처리)
gradle wrapper --gradle-version 8.14.3
```

### 1. Agent 빌드

```bash
./gradlew :agent:build
# → agent/build/libs/depscan-agent.jar
```

### 2. 샘플 앱 빌드 + 정적 분석 (Trivy)

```bash
cd sample-app
../gradlew bootJar
trivy rootfs --format json -o static.json build/libs/sample-app.jar
```

### 3. 동적 분석 (Agent 붙여서 실행)

```bash
java -javaagent:../agent/build/libs/depscan-agent.jar -jar build/libs/sample-app.jar

# 다른 터미널에서 API 호출
curl -X POST -H "Content-Type: text/plain" --data "name: depscan" http://localhost:8081/api/yaml
curl http://localhost:8081/api/hello

# Ctrl+C 로 종료 → loaded-jars.txt 생성
```

### 4. 교차 분석

```bash
cd ..
./gradlew :server:bootRun

# 다른 터미널에서 업로드
curl -F "staticReport=@sample-app/static.json" \
     -F "loadedJars=@sample-app/loaded-jars.txt" \
     http://localhost:8080/api/analyses
```

<br>

## 🎬 데모 시나리오

1. **취약점이 알려진 구버전 Spring Boot**로 샘플 앱 구성
   - 전이 의존성을 포함해 취약 라이브러리 약 30개가 포함되도록 설정
   - 그중 2~3개만 실제 API에서 호출 (예: YAML 파싱, JSON 역직렬화)
2. Trivy 정적 분석 → **경고 30개**
3. Agent 부착 후 앱 실행 → API 호출
4. 대시보드에서 교차 분석 → **실제 위험 3개** 강조 표시
5. 실제 오픈소스 프로젝트(Spring PetClinic 등)에도 적용해 실효성 검증

> 샘플 앱은 정확도 검증을 위해 **취약점이 알려진 버전으로 의도적으로 구성**했습니다.
> (OWASP WebGoat, DVWA와 같은 보안 도구 검증 방식)

<br>

## 📁 프로젝트 구조

```
backend/
├── settings.gradle            # 루트 빌드: agent + server
├── build.gradle               # 공통 설정 (Java 17, UTF-8)
│
├── agent/                     # [동적 탐지] DepScan Java Agent — 외부 의존성 없음
│   └── src/main/java/com/teams/depscan/agent/
│       ├── DepScanAgent.java          # 진입점 (premain), 감시자 등록 + 종료 시 저장
│       ├── JarTrackingTransformer.java# 클래스 로드마다 출처 jar 경로 수집
│       ├── LoadedJarRecorder.java     # 수집 결과 저장소 (thread-safe)
│       ├── JarNameParser.java         # jar 경로 → 이름/버전 추출
│       └── ReportWriter.java          # loaded-jars.txt 파일로 저장
│
├── server/                    # [교차 분석] Spring Boot 3.x 분석 서버
│   └── src/main/java/com/teams/depscan/
│       ├── controller/AnalysisController.java   # REST API
│       ├── service/AnalysisService.java         # 분석 흐름 조율 + 임시 저장
│       ├── service/ReachabilityMatcher.java     # ★ 정적 × 동적 교차 분석 핵심
│       ├── parser/TrivyReportParser.java        # static.json 파싱
│       ├── parser/LoadedJarParser.java          # loaded-jars.txt 파싱
│       ├── domain/                              # Vulnerability, LoadedLibrary, Finding ...
│       ├── dto/                                 # 대시보드 응답 형식
│       ├── config/WebConfig.java                # CORS (프론트 localhost:5173 허용)
│       └── exception/                           # 에러 응답 처리
│
└── sample-app/                # [데모 정답지] 구버전 Spring Boot 2.7 취약 앱 (독립 빌드)
    └── src/main/java/com/teams/sample/controller/
        ├── YamlController.java    # SnakeYAML 실행 → REACHABLE 대상
        ├── JsonController.java    # jackson-databind 실행
        └── HelloController.java   # 취약 라이브러리 미사용 (비교용)
```

### 파일 형식 약속 (agent ↔ server)

`loaded-jars.txt`는 탭(`\t`)으로 구분합니다. 형식을 바꾸면 `ReportWriter`와 `LoadedJarParser`를 **같이** 수정하세요.

```
# 주석
artifactId	version	loadedClassCount	location
snakeyaml	1.30	42	jar:nested:/app.jar/!BOOT-INF/lib/snakeyaml-1.30.jar!/
```

### API

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/analyses` | `staticReport`(static.json), `loadedJars`(loaded-jars.txt) 업로드 → 교차 분석 |
| GET | `/api/analyses/latest` | 가장 최근 분석 결과 |
| GET | `/api/analyses/{id}` | 특정 분석 결과 |

응답 예시:

```json
{
  "id": 1,
  "summary": { "totalCount": 30, "reachableCount": 3, "notLoadedCount": 27 },
  "findings": [
    { "cveId": "CVE-2022-1471", "library": "org.yaml:snakeyaml", "installedVersion": "1.30",
      "severity": "HIGH", "reachability": "REACHABLE", "loadedClassCount": 42 }
  ]
}
```

<br>

## 🗺 향후 계획

- **배포 차단 (CI 연동)**: GitHub Actions에서 실행해 "실제 실행되는 심각한 취약점"이 있으면 빌드 실패 처리
  - 기존 도구는 경고가 너무 많아 빌드가 매번 실패하지만, DepScan은 진짜 위험만 기준으로 차단
- **Gradle 플러그인화**: `./gradlew depscan` 한 줄로 실행
- **메서드 단위 추적**: 클래스 로드를 넘어 취약 함수의 실제 호출 여부까지 확인
- **ASPM으로 확장**: SAST, 시크릿 탐지 등 다른 보안 도구 결과까지 통합해 애플리케이션 전체 보안 우선순위화

<br>

## 🔐 프로젝트 분류

| 항목 | 내용 |
|---|---|
| 분야 | 소프트웨어 공급망 보안 |
| 유형 | 런타임 기반 SCA (Software Composition Analysis) |
| 관련 개념 | IAST(에이전트 기반 런타임 분석), ASPM(취약점 우선순위화) |

<br>

## 👥 팀 구성

| 이름 | 역할 | 담당 |
|---|---|---|
| | PM / 발표 | 일정 관리, 기능 범위 결정, 최종 발표 |
| | Agent 개발 | Java Agent 제작, jar 이름·버전 추출 |
| | 분석 로직 | Trivy 결과 파싱, Agent 기록 매칭 |
| | 프론트엔드 | 대시보드 화면, 의존성 그래프 시각화 |
| | 샘플 앱 / 통합 | 취약 샘플 앱, 전체 흐름 연결 |
| | 리서치 / 검증 | CVE 조사, 실제 프로젝트 검증 |
| | 발표 자료 / 디자인 | 슬라이드, 데모 영상 |
