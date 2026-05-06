# User Management System (Backend API)

A production-style Spring Boot backend demonstrating secure authentication,
role-based authorization, and real-world backend system design.

Includes JWT authentication, CI/CD, and admin-controlled system operations
to simulate a production-ready user management system.

---

## Engineering Highlights

### Security & Authentication
- JWT authentication with access + refresh tokens (HTTP-only cookies)
- Role-Based Access Control (ADMIN / USER)

### Backend Architecture
- Layered architecture (Controller → Service → Repository)
- Stateless REST API design using Spring Security
- Global exception handling with @ControllerAdvice

### System Design Patterns
- Application bootstrap initialization (roles + admin user)
- Admin-controlled demo reset endpoint for restoring system state

### Testing & Quality
- Unit tests (H2) + integration tests (PostgreSQL)
- CI pipeline with automated testing, Checkstyle, and SonarCloud

### DevOps
- Dockerized local development (Docker Compose)
- Production deployment (Render)

This design prioritizes security, scalability, and reproducibility across environments.

---

## Live Deployment

### Backend API

https://user-management-backend-6zmq.onrender.com

This backend is consumed by a Next.js frontend deployed on Vercel.

Note: Authentication required for most endpoints (JWT Bearer token).

---

## Tech Stack

### Backend
- Java 21
- Spring Boot
- Spring Security (OAuth2 Resource Server)
- Spring Data JPA
- Gradle

### Security
- JWT (RSA signed tokens)
- BCrypt password hashing
- Stateless authentication
- HTTP-only refresh token cookies

### Database
- PostgreSQL (production + integration tests)
- H2 (unit tests)

### DevOps
- Docker & Docker Compose
- GitHub Actions CI
- SonarCloud, JaCoCo, Checkstyle

---

## Key System Behaviors

### Authentication Flow
- Users authenticate via `/auth/login`
- Server issues:
    - JWT access token (Authorization header)
    - Refresh token (HTTP-only cookie)
- Access token is used for API authorization
- Refresh token silently renews sessions via `/auth/refresh-token`
- Logout invalidates refresh token

### Security Model
- Stateless authentication (no server session storage)
- Role-based authorization enforced at method level
- Sensitive endpoints protected via Spring Security filters

### System Management
- Application bootstraps required roles and admin user on startup
- Admin-only endpoint allows full demo reset of system state

---

## API Overview

### Authentication
```
POST /auth/login
POST /auth/refresh-token
GET  /auth/me
POST /auth/logout
```

### Users
```
GET    /users          (ADMIN, USER)
GET    /users/{id}     (ADMIN only)
POST   /users          (ADMIN only)
PUT    /users/{id}     (ADMIN only)
DELETE /users/{id}     (ADMIN only)
```

### Access Rules
- `/auth/**` → public
- `/auth/me` → authenticated users
- `GET /users/**` → ADMIN, USER
- All other `/users/**` → ADMIN only

---

## Getting Started

This backend can be accessed via a deployed production API or run locally using Docker.

---

## Production API (Recommended for Review)

The backend is deployed and available at:
https://user-management-backend-6zmq.onrender.com

Most endpoints require authentication via JWT.

---

## Local Development Setup

### Prerequisites

- Docker
- Docker Compose

### Setup & Run

```bash
git clone https://github.com/ajmiller611/user-management-system-backend.git
cd user-management-system-backend
cp .env.example .env
docker compose up --build
```

Backend will run at:
```
http://localhost:8080 (or value defined by SERVER_PORT in .env)
```

---

### Stop the Application

```bash
docker compose down
```

To reset everything (including database volume):

```bash
docker compose down -v
```

---

## CI/CD & Testing

- Unit tests (H2)
- Integration tests (PostgreSQL)
- GitHub Actions pipeline
- Checkstyle (Google Java Style)
- JaCoCo coverage reporting
- SonarCloud quality gate

---

## Related Projects

- Frontend: https://github.com/ajmiller611/user-management-system-frontend

---

## Disclaimer

Portfolio project demonstrating backend engineering, security, and system design.

---

## Contact

Andrew J. Miller

- GitHub: https://github.com/ajmiller611
- LinkedIn: https://linkedin.com/in/ajmiller611
- Email: ajmiller611@live.com