# Webhook Receiver

SAP BTP Integration Suite가 호출하는 **리시버 엔드포인트** + **인스펙터 UI**. 들어온 HTTP 요청을 수신하고 `200 OK`와 수신 확인 JSON을 반환하며, webhook.site처럼 브라우저에서 요청 내용(메서드·헤더·바디)을 실시간으로 볼 수 있습니다. Spring Security 없이 동작하며, 대용량 페이로드가 들어와도 **바디를 힙에 쌓지 않아** GC 부담이 없습니다.

- **Spring Boot 4.1.0** / **Java 17** / Gradle
- 인증 없음

---

## 실행

```bash
./gradlew bootRun
```

앱은 `http://localhost:8080` 에서 뜹니다.

```bash
curl -X POST localhost:8080/webhook/receive \
  -H "Content-Type: application/json" \
  -d '{"orderId":42}'
```

응답:

```json
{
  "status": "received",
  "method": "POST",
  "path": "/webhook/receive",
  "contentType": "application/json",
  "bytesReceived": 14,
  "receivedAt": "2026-07-19T13:24:40.303456800Z"
}
```

---

## 인스펙터 UI

브라우저에서 **`http://localhost:8080/`** 에 접속하면 받은 요청들을 실시간으로 볼 수 있습니다 (webhook.site 스타일).

- 왼쪽: 요청 목록 (메서드·경로·시각·크기, 최신순)
- 오른쪽: 선택한 요청의 상세 — 요약, 헤더, 본문 (JSON은 자동 정렬)
- 2초마다 자동 새로고침, **비우기** 버튼으로 목록 초기화
- 최근 **200건**만 유지, 본문은 요청당 **64KB**까지만 표시 (초과 시 잘림 표시)

---

## 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| ANY | `/webhook/**` | 모든 HTTP 메서드를 수신하고 `200 OK` + 수신 확인 JSON 반환. 요청은 인스펙터에 기록됨 |
| GET | `/` | 인스펙터 UI (정적 페이지) |
| GET | `/api/requests` | 기록된 요청 목록 (JSON, 최신순) |
| DELETE | `/api/requests` | 기록된 요청 전체 삭제 |

`/webhook/` 아래 경로는 무엇이든 매칭됩니다. 예: `/webhook/receive`, `/webhook/a/b/c`.

### 응답 필드

| 필드 | 설명 |
|------|------|
| `status` | 항상 `"received"` |
| `method` | 수신한 HTTP 메서드 |
| `path` | 요청 경로 |
| `contentType` | 요청 Content-Type |
| `bytesReceived` | 수신한 바디의 총 바이트 수 |
| `receivedAt` | 수신 시각 (UTC) |

---

## 메모리 설계

대용량 페이로드가 들어와도 힙에 바디를 쌓지 않습니다.

- **`@RequestBody String`을 쓰지 않습니다.** 바디를 String으로 받으면 예: 5MB 바디가 통째로 힙에 올라가 고부하 상황에서 GC를 유발합니다.
- **`InputStream`을 직접 드레인**해 8KB 버퍼로 읽어 흘려보내며 바이트 수만 카운트합니다. 바디 크기에 비례하는 힙 할당이 없습니다.
- **드레인 버퍼는 `ThreadLocal`로 스레드당 하나만 재사용**하므로 전체 버퍼 할당량이 `스레드풀 크기 × 8KB`로 고정됩니다.
- `spring.servlet.multipart.enabled=false` 로 멀티파트 버퍼링도 차단합니다.

---

## 빌드 & 테스트

```bash
./gradlew build     # 빌드
./gradlew test      # 테스트
./gradlew bootRun   # 실행
```

---

## 프로젝트 구조

```
src/main/java/com/example/webhook/
├── WebhookApplication.java   # 부트 진입점
├── WebhookController.java     # /webhook/** 리시버 (InputStream 드레인 + 캡처)
├── ReceiptResponse.java       # 수신 확인 응답 레코드
├── CapturedRequest.java       # 캡처된 요청 모델
├── RequestStore.java          # 최근 200건 보관 (스레드 안전, 링버퍼)
└── InspectorController.java    # /api/requests (목록/삭제)
src/main/resources/
├── application.properties
└── static/index.html          # 인스펙터 UI
src/test/java/com/example/webhook/
├── WebhookControllerTests.java
└── InspectorControllerTests.java
```
