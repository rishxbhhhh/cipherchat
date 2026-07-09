# CipherChat

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61dafb.svg)](https://react.dev)
[![Vite](https://img.shields.io/badge/Vite-8-646cff.svg)](https://vite.dev)
[![Tailwind](https://img.shields.io/badge/Tailwind-4-38bdf8.svg)](https://tailwindcss.com)
[![Fly.io](https://img.shields.io/badge/Fly.io-deploy-8b5cf6.svg)](https://fly.io)

End-to-end encrypted real-time chat. Messages are encrypted at rest with **AES-256-GCM** and secured with **RSA-2048** keypairs per user. Built with Spring Boot on the backend and React + Tailwind on the frontend — deployable as a single JAR.

---

## Architecture

```
┌──────────────────────────────────────────────┐
│  Browser (React SPA)                         │
│  Login → JWT → WebSocket (STOMP)             │
└──────────────────┬───────────────────────────┘
                   │ HTTPS + WSS
┌──────────────────▼───────────────────────────┐
│  Spring Boot (port 8080)                     │
│  ├─ REST API  (/api/auth, /api/conversations,│
│  │             /api/messages, /api/admin)    │
│  ├─ WebSocket (/ws — STOMP)                  │
│  ├─ Static files (React build)               │
│  └─ JWT auth filter + token blacklist        │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│  H2 (dev) / PostgreSQL (prod)                │
│  Liquibase migrations                        │
└──────────────────────────────────────────────┘
```

### Encryption Model

```
User Registration:
  RSA-2048 keypair generated
  Public key  → stored as plaintext
  Private key → AES-256-GCM encrypted with master key → stored

Conversation Creation:
  Per-conversation AES-256 key generated
  For each participant: AES key wrapped with their RSA public key

Message Send / Receive:
  Decrypt user private key (master key)
  Unwrap conversation AES key (RSA private)
  Encrypt/decrypt message body (AES-256-GCM, random IV)
```

Admins have **no encryption keys** — they cannot chat, only manage users.

---

## Quick Start

### Prerequisites

- Java 21
- Node.js 20+ (for frontend dev)
- Gradle (wrapper included)

### Run (development)

```bash
# Terminal 1 — backend
./gradlew bootRun
# Runs on http://localhost:8080

# Terminal 2 — frontend (Vite dev server with hot reload)
cd frontend
npm install
npm run dev
# Runs on http://localhost:5173, proxies API calls to :8080
```

### Run (production-like — single JAR)

```bash
# Build frontend into Spring Boot's static directory
cd frontend && npm install && npm run build

# Build and run the Spring Boot JAR
cd ..
./gradlew bootJar
java -jar build/libs/*.jar
# Open http://localhost:8080
```

### Default credentials

| Email | Password | Role | Notes |
|-------|----------|------|-------|
| `admin@cipherchat.io` | `admin` | ADMIN | User management only |
| *(register via UI)* | — | USER | Full chat access |

---

## API Reference

### Health

```
GET /health/ping                    → 200 "Cipher Chat v1.0 is up."
GET /health/test                    → 200 (requires valid JWT)
```

### Auth

```
POST /api/auth/register             { "email": "...", "password": "..." }
POST /api/auth/login                { "email": "...", "password": "..." }
    → { "accessToken": "...", "tokenType": "Bearer ", "expiresIn": 3600, "refreshToken": "..." }
POST /api/auth/refresh              { "refreshToken": "..." }
    → { "accessToken": "...", "tokenType": "Bearer ", "expiresIn": 3600 }
POST /api/auth/logout               (Authorization: Bearer *** — blacklists current token)
```

### Conversations

```
GET  /api/conversations             → List user's conversations (requires JWT)
POST /api/conversations/create      { "type": "PRIVATE|GROUP", "participantEmails": ["..."] }
    → { "conversationId": 1 }
```

### Messages

```
POST /api/messages/send             { "conversationId": 1, "content": "Hello" }
    → { "messageId": 1 }
GET  /api/messages/history?conversationId=1&page=0&size=20
    → Paginated message list (decrypted on server)
```

### WebSocket (STOMP)

```
Connect:  ws://<host>/ws
Headers:  Authorization: Bearer <jwt>

Subscribe:  /topic/conversation/{id}     (receive real-time messages)
Send:       /app/chat                     { "conversationId": 1, "content": "Hi" }
```

### Admin (requires ADMIN role)

```
GET  /api/admin/users?page=0&size=15&search=email
    → Paginated user list with search

PUT  /api/admin/users/{id}/toggle
    → Enable/disable user. Cannot toggle admin accounts.
```

---

## Project Structure

```
cipherchat/
├── src/main/java/com/rishabh/cipherchat/
│   ├── CipherchatApplication.java      # Entry point + admin seeder
│   ├── config/
│   │   └── WebSocketConfig.java        # STOMP broker, /ws endpoint
│   ├── controller/
│   │   ├── AdminController.java        # User management (ADMIN only)
│   │   ├── AuthController.java         # Register, login, refresh, logout
│   │   ├── ChatWebSocketController.java # Real-time chat handler
│   │   ├── ConversationController.java  # Create + list conversations
│   │   ├── HealthController.java       # Health check
│   │   ├── MessageController.java      # Send + history
│   │   ├── RootController.java         # Serves index.html
│   │   └── SpaFallbackController.java  # Client-side route forwarding
│   ├── dto/                            # Request/response objects
│   ├── entity/                         # JPA entities (+ TokenBlacklist)
│   ├── exception/                      # Custom exceptions + global handler
│   ├── repository/                     # Spring Data repositories
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java    # JWT validation + blacklist check
│   │   ├── SecurityConfig.java            # CORS, CSRF, role-based authz
│   │   └── WebSocketAuthInterceptor.java  # STOMP JWT validation
│   └── service/
│       ├── impl/
│       │   ├── AuthServiceImpl.java        # Registration + login + keygen
│       │   ├── ConversationServiceImpl.java # Conversation creation + list
│       │   ├── EncryptionServiceImpl.java  # AES-256-GCM + RSA-OAEP
│       │   ├── JwtServiceImpl.java         # JWT generation + validation
│       │   ├── KeyServiceImpl.java         # RSA keygen + AES-GCM wrap
│       │   ├── MessageServiceImpl.java     # Message send + history
│       │   └── TokenBlacklistPurgeService.java # Hourly expired token cleanup
│       └── interfaces/
├── src/main/resources/
│   ├── application.properties          # Shared config
│   ├── application-dev.properties      # H2, dev settings
│   ├── application-prod.properties     # PostgreSQL, production
│   ├── db/changelog/                   # Liquibase migrations (10 files)
│   └── static/                         # React build output (gitignored)
├── frontend/                           # React + Vite + Tailwind
│   ├── src/
│   │   ├── api/client.js              # Axios + JWT interceptor + auto-refresh
│   │   ├── context/AuthContext.jsx     # Auth state, JWT role parsing
│   │   ├── hooks/useStomp.js           # STOMP connection manager
│   │   ├── pages/
│   │   │   ├── Login.jsx               # Sign-in form
│   │   │   ├── Register.jsx            # Registration form
│   │   │   ├── Chat.jsx                # Full chat interface (responsive)
│   │   │   └── Admin.jsx               # User management panel
│   │   └── components/
│   │       ├── ConversationList.jsx     # Mobile-optimized sidebar
│   │       ├── MessageList.jsx          # Auto-scrolling messages
│   │       └── MessageInput.jsx         # Text input + send button
│   ├── vite.config.js                  # Proxy + build → static/
│   └── package.json
├── Dockerfile                          # Multi-stage (eclipse-temurin:21)
├── fly.toml                            # Fly.io config (Mumbai, auto HTTPS)
├── build.gradle                        # Spring Boot 3.5, Java 21, Gradle 8
└── .dockerignore
```

---

## Security

| Measure | Detail |
|---------|--------|
| Passwords | BCrypt(12) |
| Tokens | JWT (HS512), 1h expiry + 14-day refresh |
| Token revocation | Blacklist table, checked in filter, hourly purge |
| Private keys | AES-256-GCM wrapped with master key (env var) |
| Messages | AES-256-GCM per message (random IV, authenticated) |
| Conversation keys | RSA-2048-OAEP wrapped per participant |
| Disabled accounts | Rejected at authentication, clear error message |
| Master key | Externalized via `CIPHERCHAT_MASTER_KEY` env var — never in git |

---

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `CIPHERCHAT_MASTER_KEY` | **prod** | `dev-only-change-in-production` | 32-char key for private key encryption |
| `DATABASE_URL` | prod | `jdbc:postgresql://localhost:5432/cipherchat` | JDBC URL |
| `DB_USERNAME` | prod | `cipherchat` | Database user |
| `DB_PASSWORD` | prod | `cipherchat` | Database password |
| `SPRING_PROFILES_ACTIVE` | — | — | `dev` or `prod` |

### Profiles

- **`dev`** (default) — H2 in-memory, H2 console enabled, verbose actuator
- **`prod`** — PostgreSQL, minimal actuator (health + info)

---

## Deploy to Fly.io

Fly.io free tier: 3×256MB VMs, 3GB persistent volume, auto HTTPS, Mumbai region available.

### One-time setup

```powershell
# Install Fly CLI (Windows PowerShell)
iwr https://fly.io/install.ps1 -useb | iex

# Login
fly auth signup
```

### Deploy

```bash
# 1. Build frontend
cd frontend && npm install && npm run build && cd ..

# 2. Launch the app (first time)
fly launch
#   → Choose app name: cipherchat
#   → Choose region: bom (Mumbai)
#   → Choose Postgres: No (use SQLite on volume, or add Fly Postgres later)
#   → Deploy now: No

# 3. Create persistent volume for data
fly volumes create cipherchat_data --region bom --size 1

# 4. Set secrets
fly secrets set CIPHERCHAT_MASTER_KEY="your-32-char-secure-random-key-here"

# 5. Deploy
fly deploy

# 6. Open
fly open
```

### Upgrade to Fly Postgres (when needed)

```bash
fly postgres create --name cipherchat-db --region bom
fly postgres attach cipherchat-db --app cipherchat
# Connection string auto-injected as DATABASE_URL
fly deploy
```

### Useful commands

```bash
fly logs                          # Tail logs
fly status                        # App health
fly ssh console                   # SSH into VM
fly secrets list                  # View secrets
fly volumes list                  # Storage usage
fly scale memory 512              # Upgrade from 256MB free tier
```

---

## Development Commands

```bash
# Backend
./gradlew bootRun                 # Start server
./gradlew test                    # Run all tests (16)
./gradlew bootJar                 # Build fat JAR

# Frontend
cd frontend
npm run dev                       # Vite dev server (port 5173)
npm run build                     # Production build → ../src/main/resources/static/
npm run preview                   # Preview production build locally
```

---

## Tests

16 tests covering encryption round-trips and auth flows:

```
EncryptionServiceTest (10 tests):
  AES-GCM encrypt/decrypt, IV uniqueness, RSA wrap/unwrap,
  private key encrypt/decrypt, empty/long/Unicode messages

AuthControllerTest (6 tests):
  Register, duplicate rejection, login, bad credentials, token refresh
```

---

## Roadmap

- [x] JWT auth + refresh tokens
- [x] Token revocation (real logout)
- [x] Conversations + messages
- [x] End-to-end encryption (RSA + AES-GCM)
- [x] WebSocket real-time messaging (STOMP)
- [x] Admin panel — user management
- [x] Account disable/enable
- [x] React frontend (responsive, mobile-first)
- [x] Spring profiles (dev/prod)
- [x] Docker + Fly.io deployment
- [x] Integration tests
- [ ] File / image sharing with encryption
- [ ] Group chat enhancements (add/remove members)
- [ ] Read receipts + typing indicators
- [ ] Push notifications
- [ ] Mobile app (Flutter or React Native)
