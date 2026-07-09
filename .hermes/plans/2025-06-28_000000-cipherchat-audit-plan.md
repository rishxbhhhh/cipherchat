# CipherChat Audit: Improvements, Next Plans & Fly.io Deployment Strategy

> **For Hermes:** Use subagent-driven-development skill to implement this plan task-by-task.

**Goal:** Hardened, production-ready encrypted chat backend deployable on Fly.io free tier.

**Architecture:** Spring Boot 3.5 + Java 21 + JPA/Hibernate + Liquibase + JWT. RSA-2048 keypairs per user, per-conversation AES-256-GCM keys wrapped with RSA-OAEP. Messages encrypted at rest with AES-GCM. H2 in dev → PostgreSQL in prod.

**Tech Stack:** Java 21, Spring Boot 3.5.9, Spring Security 6.x, JPA, Liquibase, H2, PostgreSQL, Gradle 8.x, jjwt 0.11.5, Lombok

---

## Codebase Summary

```
src/main/java/com/rishabh/cipherchat/
├── CipherchatApplication.java          # Entry point
├── controller/
│   ├── AuthController.java             # /api/auth/register, /login, /refresh
│   ├── ConversationController.java     # /api/conversations/create
│   ├── HealthController.java           # /health/ping, /health/test
│   ├── MessageController.java          # /api/messages/send, /history
│   └── RootController.java             # /
├── dto/
│   ├── CreateConversationRequest.java
│   ├── LoginRequest.java / LoginResponse.java
│   ├── MessageResponse.java
│   ├── RefreshRequest.java
│   ├── RegisterRequest.java
│   └── SendMessageRequest.java
├── entity/
│   ├── Conversation.java               # id, type(PRIVATE/GROUP), created_at
│   ├── ConversationKey.java            # conversation_id, user_id, encrypted_aes_key
│   ├── ConversationParticipant.java
│   ├── ConversationType.java           # enum: PRIVATE, GROUP
│   ├── Message.java                    # conversation, sender, content(encrypted), sent_at
│   ├── RefreshToken.java
│   ├── Role.java                       # enum: USER, ADMIN
│   └── User.java                       # email, password(bcrypt), public_key, private_key_encrypted
├── exception/
│   ├── BadRequestException.java
│   ├── ConflictException.java
│   ├── ForbiddenException.java
│   ├── GlobalExceptionHandler.java
│   └── ResourceNotFoundException.java
├── repository/
│   ├── ConversationKeyRepository.java
│   ├── ConversationParticipantRepository.java
│   ├── ConversationRepository.java     # custom query: findPrivateConversationByParticipants
│   ├── MessageRepository.java
│   ├── RefreshTokenRepository.java
│   └── UserRepository.java
├── security/
│   ├── JwtAuthenticationFilter.java    # OncePerRequestFilter, validates JWT, sets SecurityContext
│   └── SecurityConfig.java             # CORS, CSRF disabled, STATELESS, role-based authz
└── service/
    ├── AuthService / impl/AuthServiceImpl.java       # register (keygen+encrypt), login
    ├── ConversationService / impl/ConversationServiceImpl.java  # create, idempotent private chats
    ├── CustomUserDetailsService / impl/...
    ├── EncryptionService / impl/EncryptionServiceImpl.java    # AES-256-GCM + RSA-OAEP
    ├── JwtService / impl/JwtServiceImpl.java
    ├── KeyService / impl/KeyServiceImpl.java                  # RSA keygen, master-key AES(ECB) wrap
    ├── MessageService / impl/MessageServiceImpl.java          # send + getHistory with decryption
    └── RefreshTokenService / impl/RefreshTokenServiceImpl.java
```

### Encryption Flow (well-designed)

```
Registration:
  1. Generate RSA-2048 keypair for user
  2. Encrypt private key with master-key (AES) → stored as private_key_encrypted
  3. Store public key as plaintext base64

Conversation creation:
  1. Generate per-conversation AES-256 key
  2. For each participant: encrypt AES key with their RSA public key → ConversationKey row
  3. Each participant gets their own encrypted copy of the shared AES key

Message send:
  1. Decrypt user's private key (master-key AES unwrap)
  2. Reconstruct PrivateKey object
  3. Decrypt conversation AES key (RSA private key unwrap)
  4. Encrypt message with AES-256-GCM (random IV, authenticated)
  5. Store ciphertext in messages.content

Message read (history):
  1-3. Same key unwrapping
  4. Decrypt each message with AES-256-GCM
  5. Return plaintext in MessageResponse
```

### What's Working Well

- Clean layered architecture (controller → service → repository)
- Proper Spring Security with JWT stateless auth
- Strong encryption: AES-256-GCM (authenticated) + RSA-2048-OAEP
- Liquibase migrations (8 changelogs), versioned DB schema
- Custom exception hierarchy with @RestControllerAdvice
- Idempotent private conversation creation
- Pageable message history
- BCrypt(12) password hashing
- CORS configured for allowed origins

---

## Issues Found

### 1. [SECURITY] KeyServiceImpl uses ECB mode (no IV, no authentication)

File: `service/impl/KeyServiceImpl.java:37-43`

```java
var cipher = Cipher.getInstance("AES");  // Defaults to AES/ECB/PKCS5Padding
cipher.init(Cipher.ENCRYPT_MODE, secret);
```

**Problem:** ECB mode is deterministic and unauthenticated. Same plaintext always produces same ciphertext. No integrity protection.

**Fix:** Switch to AES-GCM (like EncryptionServiceImpl already does) or at minimum AES-CBC with random IV.

### 2. [BUG] BadRequestException maps to wrong HTTP status

File: `exception/GlobalExceptionHandler.java:22`

```java
return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error(HttpStatus.NOT_FOUND, ex.getMessage()));
```

**Problem:** Error body says `404 Not Found` but HTTP status is `400 Bad Request`.

**Fix:** Change second `HttpStatus.NOT_FOUND` to `HttpStatus.BAD_REQUEST`.

### 3. [SECURITY] Master key hardcoded in application.properties

File: `src/main/resources/application.properties:15`

```
cipherchat.crypto.master-key=abcdefghijklmnopqrstuvwxyz123456
```

**Problem:** 32-char key committed to git. Anyone with repo access can decrypt all users' private keys.

**Fix:** Externalize via environment variable, never commit. Use `System.getenv()` or Spring's `@Value` with a default that only works in dev.

### 4. [SECURITY] Token revocation is cosmetic only

File: `security/SecurityConfig.java:50-51` (TODO comment)

**Problem:** `POST /api/auth/logout` returns 200 but doesn't invalidate tokens. Stolen JWT remains valid for 1 hour.

**Fix:** Token blacklist table (jti + expiry) checked in JwtAuthenticationFilter, auto-purged on expiry.

### 5. [FEATURE GAP] No WebSocket for real-time messaging

**Problem:** Messages require polling `/api/messages/history` — not a real chat experience.

**Fix:** Add STOMP over WebSocket with Spring's built-in support. Subscribe to `/topic/conversation/{id}`, send to `/app/chat`.

### 6. [DEV] No tests

**Problem:** Zero unit or integration tests. Entire codebase validated only via manual curl scripts.

**Fix:** Add Spring Boot test slices (@WebMvcTest, @DataJpaTest) plus integration tests (@SpringBootTest) with Testcontainers or H2.

### 7. [PROD] H2 in-memory only, no production DB configured

**Problem:** application.properties hardcodes H2. Postgres driver is on classpath but unused.

**Fix:** Spring profiles (application-dev.properties, application-prod.properties) with Postgres config.

### 8. [ARCH] Private key decryption + reconstruction repeated in sendMessage and getHistory

Files: `MessageServiceImpl.java:76-84` and `109-125`

**Problem:** Same 15-line decrypt-and-reconstruct block duplicated. Will be worse when WebSocket messages need it too.

**Fix:** Extract to `KeyService.reconstructPrivateKey(User)`.

### 9. [OPS] No Dockerfile

**Problem:** Java apps need a JDK container image + build process for deployment.

**Fix:** Add multi-stage Dockerfile using eclipse-temurin:21-jdk.

---

## Fly.io Deployment: Feasibility & Plan

### Is Fly.io a good fit? **YES.**

| Factor | Fit |
|--------|-----|
| Java/Spring Boot | Fly.io runs any Docker container — Java works fine |
| Free tier specs | 3×256MB VMs — tight for a full JVM but workable with tuning |
| Postgres | Fly.io offers managed Postgres (starts ~$1.94/mo) |
| Persistent volume | 3GB free — more than enough for a chat DB |
| Mumbai region | Available (bom) — low latency for India |
| HTTPS | Automatic cert provisioning via Fly.io |
| Build | Native Gradle support via Dockerfile |

### Architecture for Fly.io

```
┌─────────────────────────────────────┐
│  Fly.io (bom region)                │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ cipherchat (Spring Boot)      │  │
│  │ - JVM 21, 256MB heap          │  │
│  │ - JWT stateless auth          │  │
│  │ - WebSocket on same port      │  │
│  └──────────┬────────────────────┘  │
│             │                       │
│  ┌──────────▼────────────────────┐  │
│  │ Fly Postgres (or SQLite)      │  │
│  │ - 256MB, 1GB disk             │  │
│  └───────────────────────────────┘  │
│                                     │
│  ┌───────────────────────────────┐  │
│  │ Volume: cipherchat_data (3GB) │  │
│  │ - DB files (if SQLite)        │  │
│  │ - Uploads, logs               │  │
│  └───────────────────────────────┘  │
└─────────────────────────────────────┘
```

### Two DB options on Fly.io

**Option A: Fly Postgres (recommended)**
- Managed, backups, connection pooling
- ~$1.94/month for smallest instance
- Spring Boot + PostgreSQL = standard, well-tested
- Liquibase migrations already written for Postgres-compatible SQL

**Option B: SQLite on persistent volume (free)**
- Zero additional cost
- Single-writer limitation — fine for <100 concurrent users
- Need `spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect`
- Keep H2 for dev, SQLite for prod on Fly.io

**Recommendation:** Start with Option B (SQLite, $0 total) to validate. Graduate to Fly Postgres when user count demands it. The code already has `runtimeOnly 'org.postgresql:postgresql'`.

### JVM Memory Constraints

Free tier gives 256MB RAM. Spring Boot + JPA + Hibernate needs tuning:

```properties
# JVM flags for Dockerfile
JAVA_TOOL_OPTIONS=-Xmx192m -Xss256k -XX:+UseSerialGC -XX:MaxRAM=200m
```

This leaves ~64MB for the OS + Fly.io sidecar. SerialGC is fine for low-throughput chat workloads.

---

## Prioritized Implementation Plan

### Phase 1: Critical Fixes (today)

#### Task 1: Fix KeyServiceImpl ECB → AES-GCM

**Objective:** Replace insecure ECB mode with authenticated AES-GCM for private key wrapping.

**Files:**
- Modify: `src/main/java/com/rishabh/cipherchat/service/impl/KeyServiceImpl.java`
- Modify: `src/main/java/com/rishabh/cipherchat/service/KeyService.java` (update javadoc)

**Details:**
```java
// Replace ECB encrypt:
public String encryptPrivateKey(byte[] privateKeyBytes) {
    byte[] iv = new byte[12];
    random.nextBytes(iv);
    var secret = new SecretKeySpec(masterKey.getBytes(), 0, 32, "AES");
    var cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, secret, new GCMParameterSpec(128, iv));
    byte[] encrypted = cipher.doFinal(privateKeyBytes);
    byte[] combined = new byte[iv.length + encrypted.length];
    System.arraycopy(iv, 0, combined, 0, iv.length);
    System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
    return Base64.getEncoder().encodeToString(combined);
}
```

**⚠️ WARNING:** This change breaks existing encrypted private keys in the DB. Must clear DB or run a migration script.

#### Task 2: Fix GlobalExceptionHandler status bug

**Objective:** BadRequestException returns correct status in both HTTP response and error body.

**Files:** `src/main/java/com/rishabh/cipherchat/exception/GlobalExceptionHandler.java:22`

**Change:** `HttpStatus.NOT_FOUND` → `HttpStatus.BAD_REQUEST`

#### Task 3: Externalize master key

**Objective:** Move master key out of git-committed config.

**Files:**
- Modify: `src/main/resources/application.properties`
- Create: `src/main/resources/application-dev.properties`

**Details:**
```properties
# application.properties (default — safe for dev only)
cipherchat.crypto.master-key=dev-only-change-in-production

# application-dev.properties (gitignored or from env var)
cipherchat.crypto.master-key=${CIPHERCHAT_MASTER_KEY:dev-only-change-in-production}
```

Never commit the real key. In production, set `CIPHERCHAT_MASTER_KEY` environment variable.

#### Task 4: Extract duplicate private key reconstruction

**Objective:** DRY up the decrypt+reconstruct pattern.

**Files:**
- Modify: `src/main/java/com/rishabh/cipherchat/service/impl/MessageServiceImpl.java`
- Modify: `src/main/java/com/rishabh/cipherchat/service/impl/KeyServiceImpl.java`

**New method:**
```java
// KeyService interface
PrivateKey reconstructPrivateKey(User user);

// KeyServiceImpl
public PrivateKey reconstructPrivateKey(User user) {
    byte[] keyBytes = decryptPrivateKey(user.getPrivateKeyEncrypted());
    KeyFactory kf = KeyFactory.getInstance("RSA");
    return kf.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
}
```

Then in MessageServiceImpl both sendMessage and getHistory call `keyService.reconstructPrivateKey(sender)`.

### Phase 2: Real-time Messaging

#### Task 5: Add WebSocket support

**Files:**
- Create: `src/main/java/com/rishabh/cipherchat/config/WebSocketConfig.java`
- Create: `src/main/java/com/rishabh/cipherchat/controller/ChatWebSocketController.java`
- Modify: `build.gradle` (spring-boot-starter-websocket already present!)
- Modify: `src/main/java/com/rishabh/cipherchat/security/SecurityConfig.java`

**Details:** STOMP over WebSocket. Authenticate via JWT in CONNECT headers. On message receive: encrypt+persist (reuse MessageService), then broadcast to topic.

#### Task 6: Token revocation (real logout)

**Files:**
- Create: `src/main/java/com/rishabh/cipherchat/entity/TokenBlacklist.java`
- Create: `src/main/java/com/rishabh/cipherchat/repository/TokenBlacklistRepository.java`
- Modify: `src/main/java/com/rishabh/cipherchat/security/JwtAuthenticationFilter.java`
- Modify: `src/main/java/com/rishabh/cipherchat/controller/AuthController.java`

**Details:** On logout: save JWT `jti` + expiry to blacklist table. Filter checks blacklist before accepting token. Spring `@Scheduled` job purges expired entries hourly.

### Phase 3: Production Readiness

#### Task 7: Spring profiles + Postgres config

**Files:**
- Create: `src/main/resources/application-prod.properties`
- Modify: `src/main/resources/application.properties` (move H2-specific to dev)

#### Task 8: Dockerfile for Fly.io

**Files:**
- Create: `Dockerfile`
- Create: `fly.toml`

```dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
RUN ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-Xmx192m -Xss256k -XX:+UseSerialGC -XX:MaxRAM=200m"
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

#### Task 9: Add integration tests

**Files:**
- Create: `src/test/java/com/rishabh/cipherchat/controller/AuthControllerTest.java`
- Create: `src/test/java/com/rishabh/cipherchat/controller/MessageControllerTest.java`
- Create: `src/test/java/com/rishabh/cipherchat/service/EncryptionServiceTest.java`

**Details:** Use @SpringBootTest with H2, TestRestTemplate for API tests, pure JUnit for crypto tests.

---

## Risks & Tradeoffs

| Risk | Mitigation |
|------|-----------|
| KeyServiceImpl mode change breaks existing keys | Requires fresh DB — acceptable in dev/pre-launch |
| 256MB JVM may OOM under load | Tune heap, use SerialGC, monitor with actuator metrics |
| WebSocket complicates stateless auth | Use JWT in STOMP CONNECT frame, validate once per connection |
| SQLite concurrent write limit | Acceptable for MVP (<100 users). Graduate to Postgres later |
| Master key externalization | Use Fly.io secrets: `fly secrets set CIPHERCHAT_MASTER_KEY=...` |

---

## Files Changed Summary

| File | Action | Phase |
|------|--------|-------|
| `service/impl/KeyServiceImpl.java` | Fix ECB→GCM, add reconstructPrivateKey | 1 |
| `service/KeyService.java` | Add reconstructPrivateKey signature | 1 |
| `exception/GlobalExceptionHandler.java` | Fix bad status code | 1 |
| `application.properties` | Externalize master-key | 1 |
| `application-dev.properties` | New: dev profile | 1 |
| `service/impl/MessageServiceImpl.java` | Use reconstructPrivateKey | 1 |
| `config/WebSocketConfig.java` | New: STOMP config | 2 |
| `controller/ChatWebSocketController.java` | New: WebSocket handler | 2 |
| `entity/TokenBlacklist.java` | New: blacklist entity | 2 |
| `repository/TokenBlacklistRepository.java` | New: blacklist repo | 2 |
| `security/JwtAuthenticationFilter.java` | Check blacklist | 2 |
| `controller/AuthController.java` | Add logout blacklisting | 2 |
| `application-prod.properties` | New: prod profile | 3 |
| `Dockerfile` | New: multi-stage build | 3 |
| `fly.toml` | New: Fly.io config | 3 |
| `src/test/...` | New: test suite | 3 |
