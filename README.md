# CipherChat (Backend)

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-brightgreen.svg)](https://spring.io/projects/spring-security)
[![JWT](https://img.shields.io/badge/JWT-Stateless-orange.svg)](https://jwt.io)
[![Liquibase](https://img.shields.io/badge/Liquibase-Migrations-blue.svg)](https://www.liquibase.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-yellow.svg)](https://gradle.org/)

CipherChat is a secure Spring Boot backend designed for a real-time chat system.  
The focus is correctness first, security second, features third — built step-by-step so everything remains understandable.

UI is not included yet. This project is currently a REST API.

---

## 🚀 Tech Stack

- **Java 21**
- **Spring Boot**
- **Spring Security**
- **JWT (jjwt)**
- **Liquibase**
- **H2 (dev)**
- **Gradle**
- **Lombok**

---

## ⚙️ Run the project

```bash
./gradlew bootRun
```

App runs on:

```
http://localhost:8080
```

---

## 🔐 Authentication Model

Authentication is stateless using JWT.

1️⃣ Register  
2️⃣ Login → receive `accessToken` + `refreshToken`  
3️⃣ Use access token on protected endpoints  
4️⃣ Refresh token when access token expires  
5️⃣ Logout is cosmetic for now

Header format:

```
Authorization: Bearer <access-token>
```

Passwords are hashed using **BCrypt** — never stored in plaintext.

---

## 📡 API Reference (Implemented)

### Health

```
GET /health/ping
```

```text
Cipher Chat v1.0 is up.
```

Protected test:

```
GET /health/test
```

Requires valid JWT.

---

### Auth

#### Register
```
POST /api/auth/register
```

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

#### Login
```
POST /api/auth/login
```

Returns:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshToken": "<refresh-token>"
}
```

#### Refresh token
```
POST /api/auth/refresh
```

```json
{
  "refreshToken": "<refresh-token>"
}
```

Returns new access token.

#### Logout (cosmetic)

```
POST /api/auth/logout
```

Does not invalidate tokens yet.

---

## 💬 Conversations & Messages (Implemented)

### Create conversation
```
POST /api/conversations/create
```

Private or group:

```json
{
  "type": "PRIVATE",
  "participantEmails": ["user2@example.com"]
}
```

Returns conversation id.

### Send message
```
POST /api/messages/send
```

```json
{
  "conversationId": 1,
  "content": "Hello!"
}
```

Only conversation participants can send.

### Get message history
```
GET /api/messages/history?conversationId={id}&page=0&size=20
```

Only conversation participants can retrieve.

---

## 🛢 H2 Console (Dev only)

```
http://localhost:8080/h2
```

User: `sa`  
Password: *(blank)*

---

## ✅ API Test Summary (All Pass)

**Core flows working:**

- ✔ Health endpoints
- ✔ Registration
- ✔ Login + JWT + refresh tokens
- ✔ Authenticated access check
- ✔ Conversation Creation (Private): Successfully create a private chat between `user1` and `user2`. (Returns conversationId)
- ✔ Conversation Creation (Private): Attempt to create the same private chat; expect the same conversationId.
- ✔ Send messages (participants only)
- ✔ Prevent non-participants from messaging
- ✔ Actuator protected for normal users

**Edge cases now handled correctly:**

- ✔ Duplicate registration (`409 Conflict`)
- ✔ Incorrect credentials (`401 Unauthorized`)
- ✔ Conversation with invalid user (`404 Not Found`)
- ✔ Message to non-existing conversation (`404 Not Found`)
---

## 📌 Roadmap (Updated)

### Done
✔ JWT auth  
✔ Refresh tokens  
✔ Conversations  
✔ Message sending  
✔ Basic authorization rules  
✔ Actuator enabled  
✔ API testing via CLI  

### Next
- [ ] Message history API  
- [ ] Proper exception handling (`400 / 404 / 409` vs `403`)  
- [ ] Token revocation (real logout)  
- [ ] Encrypt chat messages  
- [ ] WebSockets for real-time messaging  
- [ ] User profiles  
- [ ] Postgres for production  
- [ ] Admin flows  
- [ ] Metrics dashboards  

---

## 📝 Notes

This backend is intentionally built incrementally.

> Every change should be clear, tested, and understood — not “magic”.

Suggestions / improvements are welcome.