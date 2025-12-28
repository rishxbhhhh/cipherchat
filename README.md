# CipherChat (Backend)

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6.x-brightgreen.svg)](https://spring.io/projects/spring-security)
[![JWT](https://img.shields.io/badge/JWT-Stateless-orange.svg)](https://jwt.io)
[![Liquibase](https://img.shields.io/badge/Liquibase-Migrations-blue.svg)](https://www.liquibase.org/)
[![Gradle](https://img.shields.io/badge/Gradle-8.x-yellow.svg)](https://gradle.org/)

CipherChat is a secure, Spring Boot–based backend designed for a real-time chat application. The focus is on security first, with a clean architecture that is easy to maintain and extend.

The UI is not part of this project yet. This is a pure REST API meant to be consumed by a web or mobile client.

## 🚀 Tech Stack

*   **Java 21**
*   **Spring Boot**: For building the application framework.
*   **Spring Security**: For robust authentication and access control.
*   **JWT (jjwt)**: For stateless, token-based authentication.
*   **Liquibase**: For database schema migrations.
*   **H2 Database**: For development and testing environments.
*   **Gradle**: For dependency management and builds.
*   **Lombok**: To reduce boilerplate code.

## ⚙️ Running the project

To run the application, execute the following command from the root directory:

```bash
./gradlew bootRun
```

The application will start on the default port `8080`.
*   **URL**: `http://localhost:8080`

## 🔐 Authentication Model (Current)

The authentication system is built around JWTs for stateless sessions.

1.  **Registration**: Users register with an email and password. Passwords are never stored in plaintext and are securely hashed using **BCrypt**.
2.  **Login**: On successful login, the server returns a JWT access token.
3.  **Authenticated Requests**: The client must send the JWT in the `Authorization` header for all protected routes.
    ```
    Authorization: Bearer <token>
    ```
4.  **Logout**: Logout is currently cosmetic. Since the system is stateless, the JWT remains valid until it expires. The client is responsible for discarding the token. A future update will include a token revocation/blacklist system for a more secure logout.

## 📡 API Reference

### 1️⃣ Health Check

Checks if the server is alive and running. This is a public endpoint and requires no authentication.

*   **Endpoint**: `GET /health/ping`
*   **Response**:
    *   **Status**: `200 OK`
    *   **Body**:
        ```
        "Cipher Chat v1.0 is up."
        ```

### 2️⃣ Register User

Registers a new user in the system.

*   **Endpoint**: `POST /api/auth/register`
*   **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "password": "secret123"
    }
    ```
*   **Response**:
    *   **Status**: `200 OK`
    *   **Body**:
        ```
        "User registered successfully."
        ```
*   **Notes**:
    *   The `email` must be unique.
    *   The `password` is stored hashed, never in plaintext.

### 3️⃣ Login

Authenticates a user and returns a JWT access token.

*   **Endpoint**: `POST /api/auth/login`
*   **Request Body**:
    ```json
    {
      "email": "user@example.com",
      "password": "secret123"
    }
    ```
*   **Response**:
    *   **Status**: `200 OK`
    *   **Body**:
        ```json
        {
          "accessToken": "<jwt-token>",
          "tokenType": "Bearer",
          "expiresIn": 3600
        }
        ```

### 4️⃣ Logout (Cosmetic – Stateless)

Clears the security context for the current request.

*   **Endpoint**: `POST /api/auth/logout`
*   **Response**:
    *   **Status**: `200 OK`
    *   **Body**:
        ```
        "Logged out successfully."
        ```
*   **What it does NOT do (yet)**:
    *   It does NOT invalidate the JWT.
    *   It does NOT block the reuse of the token before expiration.

### 5️⃣ Protected Test Endpoint

An example of an endpoint that requires a valid JWT for access.

*   **Endpoint**: `GET /api/test`
*   **Headers**:
    ```
    Authorization: Bearer <jwt-token>
    ```
*   **Response**:
    *   **Status**: `200 OK`
    *   **Body**:
        ```
        "Jwt token working as expected."
        ```
*   **Error Response**: If the token is missing or invalid, the server will respond with `401 Unauthorized`.

## 🛢️ H2 Console (Dev Only)

The in-memory H2 database console is enabled for development and can be accessed at:

*   **URL**: `http://localhost:8080/h2`
*   **Default Credentials**:
    *   **User**: `sa`
    *   **Password**: (blank)
*   **Tables to expect**:
    *   `USERS`
    *   `DATABASECHANGELOG`
    *   `DATABASECHANGELOGLOCK`

## 📌 Roadmap

Planned features for future development include:

-   [ ] JWT refresh tokens
-   [ ] Token revocation for a secure logout
-   [ ] User profiles
-   [ ] Conversations & messages schema
-   [ ] Real-time messaging with WebSockets
-   [ ] RSA / hybrid encryption for chat messages
-   [ ] Audit logs and metrics via Actuator
-   [ ] Externalized configurations per environment (dev, prod)
-   [ ] Postgres setup for production

## 📝 Notes

This backend is being built incrementally with a focus on **correctness**, **security**, and **clarity**, ensuring that every feature is both understandable and maintainable.