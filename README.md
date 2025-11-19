# Task Management System

RESTful API for managing projects and tasks with AWS Cognito authentication.

## Tech Stack

- Java 17 + Spring Boot 3.2
- PostgreSQL
- AWS Cognito for authentication
- Maven

## Features

- JWT-based authentication via AWS Cognito
- Project and Task CRUD operations
- Pagination on list endpoints
- Role-based access (ADMIN/USER)
- Input validation and error handling
- Unit tests for services and controllers
- Swagger/OpenAPI documentation

## Quick Start

### Option 1: Docker 

```bash
docker-compose up -d
```

This starts both PostgreSQL and the application. The API will be available at `http://localhost:8080`.

**Note:** You'll still need to set up AWS Cognito (see below) and update the Cognito credentials in `docker-compose.yml` or pass them as environment variables.

### Option 2: Run Locally

**1. Database Setup**

Create the PostgreSQL database:
```bash
createdb task_management_dev
```


**2. AWS Cognito Setup**

Create a User Pool in AWS Cognito:
- Go to AWS Console → Cognito → Create User Pool
- Configure sign-in: Choose "Email" or "Username"
- Create an App Client (disable client secret)
- Add custom attribute: `custom:role` (type: String)
- Create test users manually with emails
- Note down: User Pool ID, Client ID, and Region

**3. Configuration**

Update `src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/task_management_dev
    username: your_username
    password: your_password

aws:
  cognito:
    region: eu-north-1
    user-pool-id: your-pool-id
    client-id: your-client-id
```

**4. Run the Application**

```bash
mvn clean install
mvn spring-boot:run
```

API runs on `http://localhost:8080`

## API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

## Authentication

Get a token from Cognito:

```bash
aws cognito-idp initiate-auth \
  --auth-flow USER_PASSWORD_AUTH \
  --client-id YOUR_CLIENT_ID \
  --auth-parameters USERNAME=user@example.com,PASSWORD=YourPassword123!
```

Use the `IdToken` in your API requests:

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
     http://localhost:8080/api/projects
```

## API Examples

**Create a project:**
```bash
POST /api/projects
{
  "name": "Website Redesign",
  "description": "Revamp company website"
}
```

**Add a task to a project:**
```bash
POST /api/projects/1/tasks
{
  "title": "Design mockups",
  "description": "Create UI mockups in Figma",
  "status": "TODO"
}
```

**List projects with pagination:**
```bash
GET /api/projects?page=0&size=20
```

**Update task status:**
```bash
PATCH /api/tasks/1/status
{
  "status": "IN_PROGRESS"
}
```

## Project Structure

```
src/main/java/com/taskmanagement/
├── config/         - Security and app configuration
├── controller/     - REST endpoints
├── dto/            - Request/response objects
├── exception/      - Error handling
├── model/          - JPA entities (User, Project, Task)
├── repository/     - Database access
├── security/       - JWT validation and auth filter
└── service/        - Business logic
```

## Security

- All endpoints require authentication (except Swagger docs)
- JWT tokens are validated against AWS Cognito's public keys
- Stateless session management (no server-side sessions)
- Role-based access control:
    - **USER**: Can manage their own projects and tasks
    - **ADMIN**: Can manage all projects and tasks

## Testing

Run tests:
```bash
mvn test
```

Tests include unit tests for services and controllers, plus repository tests.

## Deployment Strategy

For 10k users/day (roughly 400-500 concurrent users during peak hours), here's what I'd recommend:

### Infrastructure

**Application:**
- Deploy 2-3 instances on AWS Elastic Beanstalk or ECS (t3.small/medium)
- Application Load Balancer distributes traffic across instances
- Auto-scaling adds more instances during peak hours, removes them at night

**Database:**
- AWS RDS PostgreSQL (db.t3.medium to start)
- Enable automated backups
- Add a read replica if most operations are reads (listing projects/tasks)
- Multi-AZ deployment means automatic failover if main DB fails

**Authentication:**
- AWS Cognito already handles this
- Scales automatically with usage

**Frontend (if needed):**
- Host static files on S3
- CloudFront CDN distributes files globally for faster loading

### Why This Setup?

- **Load Balancer:** If one server crashes, others keep handling requests
- **Auto-scaling:** Pay only for what you use - scale up at 9am, down at night
- **RDS Multi-AZ:** Automatic failover means no downtime if database fails
- **Read Replica:** Offload SELECT queries to reduce load on main database
- **Stateless design:** Any server can handle any request (JWT contains all needed info)

### Monitoring

- CloudWatch for logs and metrics
- Set up alarms for high CPU, memory, or DB connection issues
- Track API response times to catch performance problems early

### Estimated Monthly Cost

- Application servers: ~$70 (2 t3.small instances)
- Database: ~$80 (db.t3.medium)
- Load Balancer: ~$20
- Cognito: Free tier covers up to 50k monthly active users
- S3 + CloudFront: ~$10

**Total: Around $180-200/month**

This setup easily handles 50k+ users/day. For more scale, just add instances or upgrade the database.

### Why Not Serverless (Lambda)?

- Spring Boot has slow cold starts on Lambda
- Traditional servers are simpler for this architecture
- More predictable costs with steady traffic

## Design Decisions

**Cognito over custom auth:** Why build login, password reset, and MFA when AWS already does it securely? Also handles compliance requirements.

**PostgreSQL over NoSQL:** Projects and tasks have clear relationships. A relational database makes sense and supports complex queries if needed later.

**Stateless JWT:** No session storage on servers. Makes horizontal scaling simple since any server can handle any request.

**Pagination:** Returns 20 items by default instead of dumping the entire database. Prevents slow queries and massive responses.

## Known Limitations

- Users must be created manually in Cognito (no signup endpoint)
- No password reset flow implemented
- Deletes are permanent (no soft delete)
- Only two roles: ADMIN and USER

## Notes

This was built as a backend developer assignment with focus on clean code, proper security, and scalability considerations. Docker setup included for easy local testing.
