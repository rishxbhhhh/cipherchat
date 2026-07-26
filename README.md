# CipherChat

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61dafb.svg)](https://react.dev)
[![Vite](https://img.shields.io/badge/Vite-8-646cff.svg)](https://vite.dev)
[![Tailwind](https://img.shields.io/badge/Tailwind-4-38bdf8.svg)](https://tailwindcss.com)
[![Render](https://img.shields.io/badge/Render-deploy-46e3b7.svg)](https://render.com)

End-to-end encrypted real-time chat. Messages at rest: **AES-256-GCM**. Keys: **RSA-2048** per user. Username-based auth (`@cc.io` domain). Admin panel with user enable/disable.

---

## Architecture

```
Browser (React SPA) ── HTTPS + WSS ── Spring Boot :8080
                                        ├─ REST (/api/auth, /api/conversations,
                                        │         /api/messages, /api/admin)
                                        ├─ WebSocket (/ws — STOMP)
                                        ├─ Static files (React build)
                                        └─ JWT + token blacklist
                                              │
                                        PostgreSQL (prod) / H2 (dev)
```

### Encryption Model

```
Registration:   RSA-2048 keypair → public key stored, private key AES-256-GCM wrapped
Conversation:   Per-conversation AES-256 key → RSA-OAEP wrapped per participant
Message:        AES-256-GCM (random IV, authenticated)
Admin:          No encryption keys, cannot chat
```

---

## Quick Start

**Prerequisites:** Java 21, Node.js 20+

```bash
# Terminal 1 — backend (dev profile, H2 in-memory)
./gradlew bootRun --args='--spring.profiles.active=dev'

# Terminal 2 — frontend (Vite HMR, proxies API → :8080)
cd frontend && npm install && npm run dev -- --host 0.0.0.0
```

Open `http://localhost:5173` (with Vite) or `http://localhost:8080` (served from Spring Boot static).

### Default admin

Admin email/password set via env vars. Dev defaults: `admin@cipherchat.io` / `admin`.

### Username-based auth

Users enter a **username** (e.g. `alice`). The frontend appends `@cc.io` → `alice@cc.io`. No email validation. Usernames are free-text strings.

---

## API Reference

### Health

```
GET /health/ping                    → 200 "Cipher Chat v1.0 is up."
GET /health/test                    → 200 (requires JWT)
```

### Auth

```
POST /api/auth/register             { "email": "alice@cc.io", "password": "..." }
POST /api/auth/login                { "email": "alice@cc.io", "password": "..." }
    → { "accessToken", "tokenType": "Bearer ", "expiresIn": 3600, "refreshToken" }
POST /api/auth/refresh              { "refreshToken": "..." }
    → { "accessToken", "tokenType": "Bearer ", "expiresIn": 3600 }
POST /api/auth/logout               Authorization: Bearer <token>
    → blacklists current JWT
```

### Conversations

```
GET  /api/conversations             → list user's conversations
    → [{ "id", "type", "name", "createdAt", "lastMessage", "lastMessageAt" }]
    Private chats show other user's email. Groups show custom name or "Group:<id>".

POST /api/conversations/create      { "type": "PRIVATE|GROUP", "participantEmails": ["bob@cc.io"] }
    → { "conversationId": 1 }
    Private chats are idempotent — same pair returns existing conversation.

PUT  /api/conversations/{id}/rename { "name": "New Group Name" }
    → Rename group (participants only, group type only)
```

### Messages

```
POST /api/messages/send             { "conversationId": 1, "content": "Hello" }
    → { "messageId": 1 }

GET  /api/messages/history?conversationId=1&page=0&size=20
    → Paginated, decrypted on server
```

### WebSocket (STOMP)

```
Connect:  wss://<host>/ws
Headers:  Authorization: Bearer <token>

Subscribe: /topic/conversation/{id}    (real-time messages with timestamps)
Send:      /app/chat                   { "conversationId": 1, "content": "Hi" }
```

### Admin (ADMIN role)

```
GET  /api/admin/users?page=0&size=15&search=alice
    → Paginated user list with search by email

PUT  /api/admin/users/{id}/toggle
    → Enable/disable user. Cannot toggle admin accounts.
    Disabled users get 401 on login.
```

---

## Configuration

### Environment Variables

| Variable | Default (dev) | Description |
|----------|--------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` or `prod` |
| `CIPHERCHAT_MASTER_KEY` | `dev-only-change-in-production` | 32-char AES key for private key wrapping |
| `PGHOST` | — | PostgreSQL host (Render auto-injects) |
| `PGPORT` | `5432` | PostgreSQL port |
| `PGUSER` | — | PostgreSQL user |
| `PGPASSWORD` | — | PostgreSQL password |
| `PGDATABASE` | — | PostgreSQL database name |
| `CIPHERCHAT_ADMIN_EMAIL` | `admin@cipherchat.io` | Seeded admin email |
| `CIPHERCHAT_ADMIN_PASSWORD` | `admin` | Seeded admin password |
| `CIPHERCHAT_DB_USERNAME` | `cipherchat` | DB username (H2 file-based) |
| `CIPHERCHAT_DB_PASSWORD` | `change-me-in-production` | DB password (H2 file-based) |

### Profiles

- **`dev`** — H2 in-memory, console at `/h2`, verbose actuator
- **`prod`** — PostgreSQL via `PGHOST`/`PGPORT`/`PGUSER`/`PGPASSWORD`/`PGDATABASE`, health+info actuator

---

## Deploy to Render

### Setup

1. Create Render account, link GitHub repo
2. Create **Web Service** pointing to `https://github.com/rishxbhhhh/cipherchat`
3. Runtime: Docker, plan: Free (512MB)
4. Create **PostgreSQL** instance on Render (free dev tier)
5. Set environment variables:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `CIPHERCHAT_MASTER_KEY` | 32-char random string |
| `CIPHERCHAT_ADMIN_EMAIL` | `admin@cc.io` |
| `CIPHERCHAT_ADMIN_PASSWORD` | your-secure-password |

6. Render auto-injects `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` from the linked PostgreSQL

### Deploy

Push to `main` → Render auto-deploys. First deploy takes ~4 min (frontend + backend build). Data persists across restarts (PostgreSQL).

---

## UI Features

| Feature | Detail |
|---------|--------|
| Username auth | Enter `alice` → becomes `alice@cc.io` |
| Responsive | Mobile sidebar overlay, fixed header + input, `100dvh` viewport |
| Message grouping | Consecutive same-sender messages merged visually |
| Date separators | Today / Yesterday / date labels between days |
| Last message preview | Shown under conversation name in sidebar |
| Group rename | Pencil icon → inline edit → ✓ save |
| Private chat names | Shows other user's email automatically |
| Admin panel | User list with search, enable/disable toggle |
| Token revocation | Logout blacklists JWT, checked on every request |
| Deduplication | STOMP double-delivery on reconnect handled |

---

## Security

| Measure | Detail |
|---------|--------|
| Passwords | BCrypt(12) |
| Tokens | JWT (HS512), 1h expiry + 14-day refresh + `jti` blacklist |
| Private keys | AES-256-GCM wrapped, master key in env var |
| Messages | AES-256-GCM per message (random IV, GCM authenticated) |
| Conversation keys | RSA-2048-OAEP wrapped per participant |
| Disabled accounts | Spring Security `DisabledException` → 401 |
| Master key | Never in git, passed via env var |
| DB creds + admin creds | All externalized via env vars |

---

## Project Structure

```
cipherchat/
├── src/main/java/com/rishabh/cipherchat/
│   ├── CipherchatApplication.java         # Entry + admin seeder (env var creds)
│   ├── config/WebSocketConfig.java        # STOMP broker
│   ├── controller/
│   │   ├── AdminController.java           # User list + toggle
│   │   ├── AuthController.java            # Register, login, refresh, logout
│   │   ├── ChatWebSocketController.java   # Real-time with timestamp
│   │   ├── ConversationController.java    # CRUD + rename
│   │   ├── HealthController.java
│   │   ├── MessageController.java
│   │   ├── RootController.java
│   │   └── SpaFallbackController.java     # SPA route forwarding
│   ├── dto/                               # Request/response records
│   ├── entity/                            # User, Conversation, Message, TokenBlacklist...
│   ├── exception/                         # Custom exceptions + @RestControllerAdvice
│   ├── repository/                        # Spring Data JPA repos
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java   # JWT + blacklist check
│   │   ├── SecurityConfig.java            # CORS, stateless, role-based
│   │   └── WebSocketAuthInterceptor.java  # STOMP JWT auth
│   └── service/impl/                      # Auth, Conversation, Encryption, JWT, Key, Message
├── src/main/resources/
│   ├── application.properties             # Shared config, master key env var
│   ├── application-dev.properties         # H2, console, dev datasource
│   ├── application-prod.properties        # PostgreSQL via PGHOST/PGUSER/PGPASSWORD
│   ├── db/changelog/                      # 12 Liquibase migrations
│   └── static/                            # React build output (gitignored)
├── frontend/
│   ├── src/
│   │   ├── api/client.js                  # Axios + JWT interceptor + auto-refresh
│   │   ├── context/AuthContext.jsx        # Auth state, @cc.io append
│   │   ├── hooks/useStomp.js              # STOMP (wss:// auto-detect)
│   │   ├── pages/
│   │   │   ├── Login.jsx                  # Username input
│   │   │   ├── Register.jsx               # Username input
│   │   │   ├── Chat.jsx                   # Full chat (group + private create)
│   │   │   └── Admin.jsx                  # User management panel
│   │   └── components/
│   │       ├── ConversationList.jsx       # Sidebar + rename + last message
│   │       ├── MessageList.jsx            # Grouped messages + date separators
│   │       └── MessageInput.jsx           # Input + send
│   ├── vite.config.js                     # outDir: dist, proxy to :8080
│   └── package.json
├── Dockerfile                             # 3-stage: Node → JDK → JRE (500MB tuned)
├── render.yaml                            # Render Blueprint config
├── build.gradle                           # Spring Boot 3.5.9, Java 21
└── .dockerignore
```

---

## Development Commands

```bash
# Backend
./gradlew bootRun --args='--spring.profiles.active=dev'   # Start with H2
./gradlew test                                              # 16 tests
./gradlew bootJar                                           # Fat JAR

# Frontend
cd frontend
npm run dev -- --host 0.0.0.0    # Vite HMR on :5173
npm run build                     # Production build → dist/
npm run preview                   # Preview production build
```

---

## Roadmap

- [x] JWT auth + refresh tokens
- [x] Token revocation (real logout)
- [x] Private + group conversations
- [x] End-to-end encryption (RSA-2048 + AES-256-GCM)
- [x] WebSocket real-time messaging (STOMP + timestamp)
- [x] Admin panel — user enable/disable + search
- [x] Username-based auth (@cc.io)
- [x] Message grouping + date separators
- [x] Conversation naming (private: other user, group: editable)
- [x] Last message preview in sidebar
- [x] Responsive mobile UI (sidebar overlay, fixed header+input)
- [x] Spring profiles (dev/prod)
- [x] Docker + Render deployment
- [x] Externalized config (DB, admin, master key)
- [x] PostgreSQL persistent storage
- [x] Integration tests (16)
- [ ] Typing indicators
- [ ] Read receipts
- [ ] File / image sharing
- [ ] Push notifications
- [ ] Member add/remove for groups
