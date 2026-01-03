# API Testing Protocol for CipherChat

This document provides a step-by-step guide for manually testing the core API functionality of the CipherChat application.

## Prerequisites

- The CipherChat Spring Boot application is running.
- The default base URL is `http://localhost:8080`.
- A command-line tool capable of making HTTP requests (e.g., `curl`) is available.

---

### Step 1: Health Check

Verify that the application is running and accessible.

**Request:**
```bash
curl -X GET http://localhost:8080/health/ping
```

**Expected Response:**
- **Status:** `200 OK`
- **Body:** `Cipher Chat v1.0 is up.`

---

### Step 2: User Registration

Register three test users. The database is in-memory, so this step must be performed after every application restart.

**Endpoint:** `POST /api/auth/register`
**Headers:** `Content-Type: application/json`

**Commands:**

1.  **Register user1:**
    ```bash
    curl -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email": "user1@example.com", "password": "user1"}'
    ```

2.  **Register user2:**
    ```bash
    curl -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email": "user2@example.com", "password": "user2"}'
    ```

3.  **Register user3:**
    ```bash
    curl -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email": "user3@example.com", "password": "user3"}'
    ```

**Expected Response (for each request):**
- **Status:** `200 OK`
- **Body:** `User registered successfully.`

---

### Step 3: User Login & Token Generation

Log in as a user to obtain a JWT `accessToken`. This token is required for all subsequent authenticated requests.

**Endpoint:** `POST /api/auth/login`
**Headers:** `Content-Type: application/json`

**Command (for user1):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{"email": "user1@example.com", "password": "user1"}'
```

**Expected Response:**
- **Status:** `200 OK`
- **Body (JSON):**
  ```json
  {
      "accessToken": "eyJhbGciOi...",
      "tokenType": "Bearer ",
      "expiresIn": 3600,
      "refreshToken": "..."
  }
  ```
**Action:** Copy the `accessToken` and `refreshToken` values. They will be referred to as `<USER1_ACCESS_TOKEN>` and `<USER1_REFRESH_TOKEN>` in the following steps.

---


### Step 4: Authenticated Endpoint Test

Verify that the obtained `accessToken` works on a protected endpoint.

**Endpoint:** `GET /health/test`
**Headers:** `Authorization: Bearer <ACCESS_TOKEN>`

**Command:**
```bash
curl -X GET http://localhost:8080/health/test \
-H "Authorization: Bearer <USER1_ACCESS_TOKEN>"
```

**Expected Response:**
- **Status:** `200 OK`
- **Body:** `Jwt token working as expected.`

---


### Step 5: Conversation Creation

Test the creation of private and group conversations. All requests must be authenticated.

**Endpoint:** `POST /api/conversations/create`
**Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <USER1_ACCESS_TOKEN>` (Requests are made by `user1`)

**Commands:**

1.  **Create Private Chat (user1 & user2):**
    ```bash
    curl -X POST http://localhost:8080/api/conversations/create \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER1_ACCESS_TOKEN>" \
    -d '{"type": "PRIVATE", "participantEmails": ["user2@example.com"]}'
    ```
    *Expected Response:* A JSON object with `conversationId: 1`.

2.  **Attempt to Re-create Private Chat (user1 & user2 - Idempotent Test):**
    ```bash
    curl -X POST http://localhost:8080/api/conversations/create \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER1_ACCESS_TOKEN>" \
    -d '{"type": "PRIVATE", "participantEmails": ["user2@example.com"]}'
    ```
    *Expected Response:* A JSON object with the *same* `conversationId` as the first attempt (e.g., `conversationId: 1`). This confirms the idempotent behavior.

3.  **Create Private Chat (user1 & user3):**
    ```bash
    curl -X POST http://localhost:8080/api/conversations/create \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER1_ACCESS_TOKEN>" \
    -d '{"type": "PRIVATE", "participantEmails": ["user3@example.com"]}'
    ```
    *Expected Response:* A JSON object with `conversationId: 2`.

3.  **Create Group Chat (user1, user2, user3):**
    ```bash
    curl -X POST http://localhost:8080/api/conversations/create \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER1_ACCESS_TOKEN>" \
    -d '{"type": "GROUP", "participantEmails": ["user2@example.com", "user3@example.com"]}'
    ```
    *Expected Response:* A JSON object with `conversationId: 3`.

---


### Step 6: Message Sending & Authorization

Test the sending of messages to the created conversations.

**Endpoint:** `POST /api/messages/send`
**Headers:**
- `Content-Type: application/json`
- `Authorization: Bearer <ACCESS_TOKEN>`

**Test Cases:**

1.  **Successful Message (Participant):** `user1` sends a message to conversation `1`.
    ```bash
    curl -X POST http://localhost:8080/api/messages/send \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER1_ACCESS_TOKEN>" \
    -d '{"conversationId": 1, "content": "first message from user1"}'
    ```
    *Expected Response:* A JSON object with a `messageId`.

2.  **Failed Message (Non-Participant):** `user3` attempts to send a message to conversation `1`.
    ```bash
    curl -X POST http://localhost:8080/api/messages/send \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer <USER3_ACCESS_TOKEN>" \
    -d '{"conversationId": 1, "content": "message from non-participant"}'
    ```
    *Expected Response:* `403 Forbidden`. This confirms the security rule is working.

---

### Step 7: Get Message History

Test retrieving message history for a conversation.

**Endpoint:** `GET /api/messages/history`
**Headers:** `Authorization: Bearer <ACCESS_TOKEN>`

**Test Cases:**

1.  **Successful History Retrieval (Participant):** `user1` retrieves the history for conversation `1`.
    ```bash
    curl -X GET "http://localhost:8080/api/messages/history?conversationId=1&page=0&size=10" \
    -H "Authorization: Bearer <USER1_ACCESS_TOKEN>"
    ```
    *Expected Response:* `200 OK` with a JSON object containing a list of messages.

2.  **Failed History Retrieval (Non-Participant):** `user3` attempts to retrieve the history for conversation `1`.
    ```bash
    curl -X GET "http://localhost:8080/api/messages/history?conversationId=1&page=0&size=10" \
    -H "Authorization: Bearer <USER3_ACCESS_TOKEN>"
    ```
    *Expected Response:* `403 Forbidden`.

---

## Failure & Edge Case Scenarios

This section covers tests for expected failures and edge cases.

### Authentication & Authorization Tests

1.  **Failed Registration (Duplicate User):**
    -   **Purpose:** To ensure the API prevents duplicate user registrations.
    -   **Action:** Attempt to register again with an email that already exists.
    -   **Command:**
        ```bash
        curl -X POST http://localhost:8080/api/auth/register \
        -H "Content-Type: application/json" \
        -d '{"email": "user1@example.com", "password": "newpassword"}'
        ```
    -   **Expected Result:** A `4xx` error (e.g., `409 Conflict` or `400 Bad Request`) with a message like "Email already registered."

2.  **Failed Login (Incorrect Credentials):**
    -   **Purpose:** To verify that the authentication manager rejects bad credentials.
    -   **Action:** Attempt to log in with a valid email but an incorrect password.
    -   **Command:**
        ```bash
        curl -X POST http://localhost:8080/api/auth/login \
        -H "Content-Type: application/json" \
        -d '{"email": "user1@example.com", "password": "wrongpassword"}'
        ```
    -   **Expected Result:** A `401 Unauthorized` or `403 Forbidden` error.

3.  **Token Refresh Functionality:**
    -   **Purpose:** To confirm that a valid refresh token can generate a new access token.
    -   **Action:** Use a `refreshToken` from a successful login to call the refresh endpoint.
    -   **Command:**
        ```bash
        curl -X POST http://localhost:8080/api/auth/refresh \
        -H "Content-Type: application/json" \
        -d '{"refreshToken": "<USER1_REFRESH_TOKEN>"}'
        ```
    -   **Expected Result:** A `200 OK` response containing a new `accessToken`.

4.  **Admin-Only Endpoint Access:**
    -   **Purpose:** To ensure that `hasAuthority('ADMIN')` security rules are enforced.
    -   **Action:** Attempt to access an admin-only endpoint as a regular user.
    -   **Command:**
        ```bash
        curl -X GET http://localhost:8080/actuator/beans \
        -H "Authorization: Bearer <USER1_ACCESS_TOKEN>"
        ```
    -   **Expected Result:** A `403 Forbidden` error.

### Business Logic Tests

1.  **Create Conversation with Invalid Participant:**
    -   **Purpose:** To check how the application handles requests with non-existent entities.
    -   **Action:** Attempt to create a private conversation with a user email that does not exist.
    -   **Command:**
        ```bash
        curl -X POST http://localhost:8080/api/conversations/create \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer <USER1_ACCESS_TOKEN>" \
        -d '{"type": "PRIVATE", "participantEmails": ["nouser@example.com"]}'
        ```
    -   **Expected Result:** A `4xx` error indicating the user was not found.

2.  **Send Message to Invalid Conversation:**
    -   **Purpose:** To ensure a user cannot send messages to a non-existent conversation.
    -   **Action:** Attempt to send a message to a `conversationId` that does not exist.
    -   **Command:**
        ```bash
        curl -X POST http://localhost:8080/api/messages/send \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer <USER1_ACCESS_TOKEN>" \
        -d '{"conversationId": 999, "content": "message to nowhere"}'
        ```
    -   **Expected Result:** A `4xx` error indicating the conversation was not found.
