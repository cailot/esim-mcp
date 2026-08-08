# esim-mcp

한국 입국 전까지 매일 최적의 eSIM 요금제를 찾아 보고하는 Standalone Java 애플리케이션입니다.

## 목표

한국에 갈 때까지(2026년 9월 15일까지) 조건에 맞는 가장 좋은 SIM 카드 플랜을 찾아 **매일 보고**합니다.

## SIM 카드 선정 조건

1. **eSIM 지원** — 물리 SIM이 아닌 eSIM이 가능한 요금제
2. **해외 문자 수신** — 해외에서 사용해도 문자(SMS)를 받을 수 있을 것
3. **월 유지비 최저** — 월 유지 가격이 가장 싼 플랜
4. **일정한 유지 가격** — 몇 달만 프로모션으로 저렴하고 이후 가격이 오르는 구조가 아니라, **항상 유지 가격이 일정한** 플랜

## 기술 스택

### 애플리케이션

- **Standalone Java 17** 애플리케이션 (Maven)
- **MCP Java SDK** (`io.modelcontextprotocol.sdk:mcp`)로 MCP 서버에 STDIO 클라이언트로 연결

### MCP 연동

| MCP | 용도 |
| --- | --- |
| **Brave Search MCP** | 최신 eSIM 요금제 웹 검색 |
| **Puppeteer MCP** | 검색 결과/상품 페이지 크롤링·점검 |
| **Sequential Thinking MCP** | 선정 조건을 단계별로 검증 |
| **Supabase MCP** | 일일 결과를 DB에 저장 |
| **Gmail MCP** | 최종 리포트를 이메일로 발송 |

### 자동화

- 앱 기동 시 즉시 1회 실행 후, 매일 정해진 시각에 재실행
- 실행 기간: **2026년 9월 15일까지** (`report.end.date`)

## 일일 리포트 흐름

1. Brave Search / Puppeteer로 최신 eSIM 요금제 정보 수집
2. 선정 조건으로 필터링·최저가 선정
3. Sequential Thinking으로 단계별 검증
4. 결과를 Supabase에 저장
5. Gmail로 일일 리포트 발송

## 프로젝트 구조

```
src/main/java/com/esimmcp/
  EsimMcpApplication.java      # 진입점
  config/                      # application.properties / MCP 서버 스펙
  domain/                      # EsimPlan, PlanCriteria, DailyReport
  mcp/                         # MCP STDIO 클라이언트 매니저
  service/                     # 검색·평가·저장·이메일·파이프라인
  schedule/                    # 일일 스케줄러
supabase/schema.sql            # Supabase 테이블 DDL
.cursor/mcp.json               # Cursor IDE용 MCP 서버 설정
```

## 빠른 시작

### 1. 의존성 빌드

```bash
mvn -q test
```

### 2. Dry-run (기본값, 외부 MCP 없이 샘플 데이터로 동작)

```bash
mvn -q exec:java -Dexec.args=once
```

### 3. 실제 MCP 연동

모든 설정은 `src/main/resources/application.properties` 한 곳에 있습니다 (`.env` 없음).

1. `application.properties`에서 API 키 / DB / 이메일 값을 채웁니다.
2. Supabase에서 `supabase/schema.sql`을 실행합니다.
3. `mcp.dry.run=false` 로 변경합니다.
4. (선택) Cursor에서 `.cursor/mcp.json` 서버들도 활성화합니다.

```bash
mvn -q exec:java -Dexec.args=once   # 1회 실행
mvn -q exec:java                    # 매일 스케줄 (Ctrl+C로 종료)
```

### 4. 패키징

```bash
mvn -q package
java -jar target/esim-mcp-0.1.0-SNAPSHOT.jar once
```

## 설정

**단일 설정 파일:** `src/main/resources/application.properties`  
(`.env` / 환경변수 오버라이드 없음 — 여기에만 작성)

| 키 | 설명 | 기본값 |
| --- | --- | --- |
| `report.end.date` | 스케줄 종료일 | `2026-09-15` |
| `report.cron.hour` / `minute` | 매일 실행 시각 | `9` / `0` (Asia/Seoul) |
| `mcp.dry.run` | MCP 미연결 로컬 데모 | `true` |
| `report.email.to` | 리포트 수신 메일 | `dear.jinhyung@gmail.com` |
| `spring.datasource.*` | Supabase PostgreSQL JDBC | (파일 내 설정) |
| `mcp.*.env.*` | 각 MCP 서버에 전달할 키 | (파일 내 설정) |

## 다음 단계

- [ ] Brave/Puppeteer 응답을 구조화된 `EsimPlan` JSON으로 파싱하는 프롬프트/스키마 고도화
- [ ] Supabase upsert(하루 1행) 및 가격 변동 히스토리 테이블
- [ ] Gmail MCP 인증(OAuth) 완료 후 실메일 발송 검증
- [ ] launchd/cron 또는 CI로 프로세스 상시 기동
