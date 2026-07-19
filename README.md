# Webhook Receiver

SAP BTP Integration Suite가 호출하는 **리시버 엔드포인트**. 들어온 HTTP 요청을 수신하고 `200 OK`와 수신 확인 JSON을 반환합니다. Spring Security 없이 동작하며, 대용량 페이로드가 들어와도 **바디를 힙에 쌓지 않아** GC 부담이 없습니다.

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

## 엔드포인트

| 메서드 | 경로 | 설명 |
|--------|------|------|
| ANY | `/webhook/**` | 모든 HTTP 메서드를 수신하고 `200 OK` + 수신 확인 JSON 반환 |

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
├── WebhookController.java     # /webhook/** 리시버 (InputStream 드레인)
└── ReceiptResponse.java       # 수신 확인 응답 레코드
src/main/resources/
└── application.properties
src/test/java/com/example/webhook/
└── WebhookControllerTests.java
```
