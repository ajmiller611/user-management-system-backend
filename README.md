# User Management System (Backend)

Backend service for a secure, role-based user management system built with Spring Boot.  
Focus: authentication, authorization, testing strategy, and production-style backend engineering.

---

## Quick Highlights

- JWT authentication with RSA-signed tokens
- Access + refresh token system (HTTP-only cookies)
- Role-based authorization (ADMIN, USER)
- Layered architecture (Controller → Service → Repository)
- Unit + integration testing (H2 + PostgreSQL)
- Fully containerized with Docker Compose
- CI pipeline with tests, linting, coverage, and SonarCloud

---

## Tech Stack

### Backend
- Java 21
- Spring Boot
- Spring Security (OAuth2 Resource Server)
- Spring Data JPA
- Gradle

### Security
- JWT (RSA signing)
- BCrypt password hashing
- Stateless authentication
- HTTP-only refresh token cookies

### Database
- PostgreSQL (runtime + integration tests)
- H2 (unit tests)

### DevOps
- Docker & Docker Compose
- GitHub Actions CI
- SonarCloud, JaCoCo, Checkstyle

---

## Features

- User authentication (login/logout)
- JWT-based access control
- Refresh token workflow
- Role-based access control (ADMIN, USER)
- Full CRUD user management API
- Centralized validation and exception handling
- CORS configuration for frontend integration

---

## Architecture

- Layered design:
   - Controller → API layer
   - Service → business logic
   - Repository → data access
- Stateless authentication using JWT
- Spring Security filter chain for request protection
- Custom JWT claims for role extraction
- Global exception handling (`@ControllerAdvice`)
- Validation using Jakarta Bean Validation + custom rules

---

## Project Structure

```
src/
  main/
    java/io/github/ajmiller611/usermanagement/
      controller/    # REST API endpoints
      service/       # Business logic
      repository/    # Data access layer (Spring Data JPA)
      model/         # Entity classes
      dto/           # Request/response data transfer objects
      security/      # JWT, authentication, and security configuration
      config/        # Application and security configuration
      exception/     # Global exception handling
      util/          # Utility/helper classes
      
    resources/
      application.properties  # Base configuration
      
  test/              # Unit tests (H2)
  integrationTest/   # Integration tests (PostgreSQL)
```

---

## Authentication Flow

1. User logs in via `POST /auth/login`
2. Backend authenticates credentials using Spring Security
3. Returns:
   - JWT access token (Authorization header)
   - Refresh token (HTTP-only cookie)
   - User details (id, username, email, roles)
4. Frontend uses access token for requests
5. Backend validates JWT on each request
6. On expiration:
   - `POST /auth/refresh-token` issues new tokens
7. `POST /auth/logout` clears refresh cookie

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

### Prerequisites
- Docker
- Docker Compose

---

### Run Project

```bash
git clone https://github.com/ajmiller611/user-management-system-backend.git
cd user-management-system-backend
cp .env.example .env
docker compose up --build
```

Backend runs on:
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

## Testing

- Unit tests → H2 database
- Integration tests → PostgreSQL container
- CI runs full test suite automatically

```bash
./gradlew test
./gradlew integrationTest
```

---

## CI/CD

Automated GitHub Actions pipeline:

- Build + dependency resolution
- Unit tests (H2)
- Integration tests (PostgreSQL container)
- Checkstyle (Google Java Style)
- JaCoCo coverage reporting
- SonarCloud quality gate

---

## Project Goals

This project demonstrates:

- Secure backend architecture with Spring Security
- Real-world JWT authentication with refresh tokens
- Production-style system design
- Testing strategy separation (unit vs integration)
- CI/CD pipeline integration
- Containerized deployment with Docker

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