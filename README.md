# Task Management System

RESTful API for managing projects and tasks with AWS Cognito authentication.

## Tech Stack

- Java 17 + Spring Boot 3.2
- PostgreSQL
- AWS Cognito
- Maven

## Features

- JWT authentication via AWS Cognito
- Project and Task CRUD operations
- Pagination on all list endpoints
- Role-based access (ADMIN/USER)
- Input validation and error handling
- Unit tests with Mockito
- Swagger/OpenAPI docs

## Start

### Using Docker 

```bash
docker-compose build
docker-compose up -d
```

API available at `http://localhost:8080`

You still need to configure AWS Cognito credentials (see below).

### Running Locally

**1. Database**

```bash
createdb task_management_dev
```

**2. AWS Cognito Setup**

- Create User Pool in AWS Console
- Add App Client (disable client secret)
- Note: User Pool ID, Client ID, Region
- Create test users manually

**3. Configure**

Update `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/task_management_dev
    username: your_username
    password: your_password

aws:
  cognito:
    region: your-region
    user-pool-id: your-pool-id
    client-id: your-client-id
```

**4. Run**

```bash
mvn clean install
mvn spring-boot:run
```

## API Documentation

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Authentication

Get JWT token from Cognito:

```bash
aws cognito-idp initiate-auth \
  --auth-flow USER_PASSWORD_AUTH \
  --client-id YOUR_CLIENT_ID \
  --auth-parameters USERNAME=user@example.com,PASSWORD=Password123!
```

Use the `IdToken`:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/api/projects
```

## Main Endpoints

```
POST   /api/projects                      - Create project
GET    /api/projects                      - List projects (paginated)
GET    /api/projects/{id}                 - Get project
PUT    /api/projects/{id}                 - Update project
DELETE /api/projects/{id}                 - Delete project

POST   /api/projects/{id}/tasks           - Create task
GET    /api/projects/{id}/tasks           - List tasks (paginated)
PATCH  /api/projects/{id}/tasks/{id}/status - Update task status

GET    /api/users/me                      - Get current user profile
GET    /api/users/me/projects             - Get my projects
GET    /api/users/me/tasks                - Get my tasks
```

Admin-only endpoints:
```
GET    /api/users                         - List all users
GET    /api/users/{id}/projects           - Get user's projects
```

## Project Structure

```
src/main/java/com/taskmanagement/
├── controller/     - REST endpoints
├── service/        - Business logic
├── repository/     - Database access
├── model/          - JPA entities
├── dto/            - Request/response DTOs
├── security/       - JWT validation
└── exception/      - Error handling
```

## Security

- All endpoints require authentication
- JWT validated against Cognito public keys
- Stateless (no sessions)
- Users can only access their own data
- Admins can access everything

## Testing

```bash
mvn test
```

Unit tests for service layer using Mockito.

## Deployment for 10k Users/Day

**Setup for 10k daily users:**

**Backend:**
- 2 Spring Boot instances on AWS Elastic Beanstalk (t3.small)
- Application Load Balancer distributes traffic
- Auto-scaling adds instances when CPU > 70%

**Database:**
- RDS PostgreSQL (db.t3.medium) with Multi-AZ for automatic failover
- Read replica if you add analytics/reporting features

**Frontend (React/Vue/etc):**
- Static files on S3
- CloudFront CDN for fast global delivery

This handles 10k+ users/day easily. Spring Boot is stateless so just add more instances to scale.

## Design Decisions

**Cognito?** Handles passwords, MFA, password resets. Free for small scale. (Assignment requirement)

**PostgreSQL?** Projects and tasks have clear relationships.

**Why stateless?** Any server can handle any request. Makes scaling simple.

**getOrCreateCurrentUser pattern:** Users auto-created on first API call after Cognito auth. No separate signup needed.

## Limitations

- No self-service signup (users created manually in Cognito)
- No soft deletes
- Two roles only: USER and ADMIN

