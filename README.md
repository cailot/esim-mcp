# esim-mcp

한국 입국 전까지 매일 최적의 eSIM 요금제를 찾아 보고하는 Standalone Java 애플리케이션입니다.

## 목표

한국에 갈 때까지(2026년 9월 15일까지) 조건에 맞는 가장 좋은 SIM 카드 플랜을 찾아 **매일 보고**합니다.

## SIM 카드 선정 조건

1. **현재 가입 가능** — 마감/판매종료/가입불가(예: “해당 요금제는 마감되었습니다”)가 아닌 요금제만 대상
2. **eSIM 지원** — 물리 SIM이 아닌 eSIM이 가능한 요금제
3. **해외 문자 수신** — 해외에서 사용해도 문자(SMS)를 받을 수 있을 것
4. **월 유지비 최저** — 월 유지 가격이 가장 싼 플랜
5. **일정한 유지 가격** — 몇 달만 싸다가 이후 정상가로 오르는 구조는 제외. **특가/평생할인이 계속 같은 월 유지비**면 허용 (개통 기한, 최소 사용량 조건은 무시)

## 기술 스택

### 애플리케이션

- **Standalone Java 17** 애플리케이션 (Maven)
- **MCP Java SDK** (`io.modelcontextprotocol.sdk:mcp`)로 MCP 서버에 STDIO 클라이언트로 연결

### MCP 연동

| MCP | 용도 |
| --- | --- |
| **Brave Search MCP** | 최신 eSIM 요금제 웹 검색 |
| **Playwright MCP** | 검색 결과/상품 페이지 크롤링·점검 |
| **Sequential Thinking MCP** | 선정 조건을 단계별로 검증 |
| **Supabase MCP** | 일일 결과를 DB에 저장 |
| **Gmail SMTP** | 앱 비밀번호로 최종 리포트 이메일 발송 (MCP 아님) |

### 자동화

- **GitHub Actions** 매일 09:00 KST (`0 0 * * *` UTC) 1회 실행 — [`.github/workflows/daily-esim-report.yml`](.github/workflows/daily-esim-report.yml)
- 로컬/CI 기본: **1회 실행 후 자동 종료**
- (선택) `schedule` 모드로 매일 스케줄 상시 기동 가능
- 실행 기간: **2026년 9월 15일까지** (`report.end.date`) — 이후에는 no-op 종료

## 일일 리포트 흐름

1. Brave Search / Playwright로 최신 eSIM 요금제 정보 수집
2. 선정 조건으로 필터링·최저가 선정
3. Sequential Thinking으로 단계별 검증
4. 결과를 Supabase에 저장
5. Gmail SMTP(앱 비밀번호)로 일일 리포트 발송

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
.github/workflows/             # GitHub Actions 일일 리포트
.cursor/mcp.json               # Cursor IDE용 MCP 서버 설정
```

## 빠른 시작

### 1. 의존성 빌드

```bash
mvn -q test
```

### 2. 로컬 실행

1. `application.properties.example` → `application.properties` 복사 후 시크릿 채우기  
   (`application.properties`는 gitignore)
2. Supabase에서 `supabase/schema.sql` 실행
3. (선택) Cursor에서 `.cursor/mcp.json` 서버 활성화

```bash
./run.sh                                 # 권장: compile + 1회 실행 후 종료
mvn -q compile exec:java                 # 동일
mvn -q compile exec:java -Dexec.args=schedule  # 매일 스케줄 (Ctrl+C로 종료)
```

### 3. GitHub Actions 자동화

워크플로가 매일 리포트를 돌립니다. Repository **Settings → Secrets and variables → Actions**에 아래 Secret을 등록하세요.

| Secret | 용도 | 로컬 대응 |
| --- | --- | --- |
| `REPORT_EMAIL_TO` | 리포트 수신 메일 | `report.email.to` |
| `SPRING_DATASOURCE_URL` | Supabase JDBC URL | `spring.datasource.url` |
| `SPRING_DATASOURCE_USERNAME` | DB 사용자 | `spring.datasource.username` |
| `SPRING_DATASOURCE_PASSWORD` | DB 비밀번호 | `spring.datasource.password` |
| `BRAVE_API_KEY` | Brave Search | `mcp.brave.env.BRAVE_API_KEY` |
| `SUPABASE_ACCESS_TOKEN` | Supabase MCP | `mcp.supabase.env.SUPABASE_ACCESS_TOKEN` |
| `SUPABASE_PROJECT_REF` | Supabase project ref | `mcp.supabase.project.ref` / args |
| `SPRING_MAIL_USERNAME` | Gmail SMTP 계정 | `spring.mail.username` |
| `SPRING_MAIL_PASSWORD` | Gmail 앱 비밀번호 | `spring.mail.password` |

Gmail 앱 비밀번호는 Google 계정 → **보안 → 2단계 인증 → 앱 비밀번호**에서 만듭니다.

수동 실행: GitHub Actions 탭 → **Daily eSIM report** → **Run workflow**

### 4. 패키징

```bash
mvn -q package
java -jar target/esim-mcp-0.1.0-SNAPSHOT.jar        # 1회 후 종료
java -jar target/esim-mcp-0.1.0-SNAPSHOT.jar schedule
```

## 설정

| 환경 | 시크릿 위치 |
| --- | --- |
| **Local** | `src/main/resources/application.properties` |
| **GitHub Actions** | Repository Secrets → 환경변수로 주입 (비어 있지 않으면 properties보다 우선) |

| 키 | 설명 | 기본값 |
| --- | --- | --- |
| `report.end.date` | 스케줄 종료일 | `2026-09-15` |
| `report.cron.hour` / `minute` | 로컬 스케줄 시각 | `9` / `0` (Asia/Seoul) |
| `report.email.to` | 리포트 수신 메일 | (비움) |
| `spring.mail.username` / `password` | Gmail SMTP 앱 비밀번호 | (파일/Secret) |
| `spring.datasource.*` | Supabase PostgreSQL JDBC | (파일/Secret) |
| `mcp.*.env.*` | 각 MCP 서버에 전달할 키 | (파일/Secret) |

## 트러블슈팅

### MCP 연결 실패 (`Client failed to initialize`)

MCP Java SDK 2.x는 Jackson 3를 쓰고, `jackson-annotations` **2.20+** 가 필요합니다.
이 프로젝트는 `pom.xml`의 `jackson.version`을 2.20으로 고정해 두었습니다.
`NoSuchFieldError: POJO` 가 보이면 annotations 버전이 낮은 것입니다.

로컬/GitHub Secret 분리는 MCP stdio 연결과 무관합니다. properties의 키는 자식 프로세스 env로 전달됩니다.

### Gmail SMTP 실패 (`535` / `Username and Password not accepted`)

- Google 계정에서 **2단계 인증**이 켜져 있어야 앱 비밀번호를 만들 수 있습니다.
- `spring.mail.password`에는 계정 비밀번호가 아니라 **16자리 앱 비밀번호**를 넣습니다.
- GitHub Actions는 `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` Secret이 필요합니다. 메일 전송이 실패하면 이제 워크플로가 실패합니다.

## 다음 단계

- [ ] Brave/Playwright 응답을 구조화된 `EsimPlan` JSON으로 파싱하는 프롬프트/스키마 고도화
- [ ] Supabase upsert(하루 1행) 및 가격 변동 히스토리 테이블
- [ ] GitHub Actions에서 Playwright 브라우저 의존성 설치 최적화
- [ ] Actions 실행 로그/실패 알림 정리
