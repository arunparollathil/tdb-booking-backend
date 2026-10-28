# TDB Darshan Booking Service

A Spring Boot REST API for managing temple darshan slots and ticket bookings. The service supports slot capacity management, concurrent booking requests, idempotent retries, booking cancellation, and financial-year-based receipt numbering.

## Features

- **Temple Management:** Maintain temple information and temple codes.
- **Darshan Slot Management:** Create date-specific slots with configurable ticket capacity.
- **Ticket Booking:** Book one or more tickets for a specific darshan slot.
- **Concurrency Control:** Prevent overbooking using database-level pessimistic locking.
- **Safe Retries:** Use idempotency keys to prevent duplicate bookings when requests are retried.
- **Booking Cancellation:** Cancel eligible bookings and release the associated ticket capacity.
- **Receipt Number Generation:** Generate unique, sequential receipt numbers for each temple and financial year.
- **Validation and Error Handling:** Validate requests and return consistent API error responses.

## Technology Stack

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- PostgreSQL
- Maven
- JUnit 5
- Mockito

> Update this list if the project uses additional technologies or different dependency versions.

## Prerequisites

Install the following before running the application:

- JDK 17 or compatible Java 17 distribution
- Maven 3.8+
- PostgreSQL
- Git

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd tdb-darshan-booking-service
```

Replace `<repository-url>` with the actual Git repository URL.

### 2. Create the Database

Create a PostgreSQL database:

```sql
CREATE DATABASE tdb_darshan_booking;
```

Configure the database connection in `src/main/resources/application.yml` or `application.properties`.

Example configuration:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tdb_darshan_booking
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
```

Configure the actual database credentials using environment variables. Ensure the database schema and migrations are applied before starting the application.

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Alternatively, run the generated JAR:

```bash
java -jar target/tdb-darshan-booking-service.jar
```

The default Spring Boot port is `8080`, unless overridden by the project configuration.

## API Endpoints

The following endpoints describe the intended API structure. Confirm the exact paths and request/response DTOs against the implemented controllers.

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/darshan-slots` | Create a date-specific darshan slot |
| `GET` | `/api/v1/darshan-slots/{slotId}` | Retrieve slot details and availability |
| `POST` | `/api/v1/darshan-bookings` | Create a booking |
| `GET` | `/api/v1/darshan-bookings/{bookingId}` | Retrieve booking details |
| `PATCH` | `/api/v1/darshan-bookings/{bookingId}/cancel` | Cancel an eligible booking |

### Slot Availability

Slot details should include:

- Capacity
- Booked ticket count
- Available ticket count
- Slot date and timing
- Active status

Available tickets are calculated as:

```text
availableCount = capacity - bookedCount
```

The available count should be calculated from the current database state rather than maintained as a separate counter.

## Core Business Rules

### 1. Prevent Overbooking

Multiple clients, including online portals and temple counters, may attempt to book the same slot simultaneously.

The service uses pessimistic database locking to serialize booking operations for the same slot.

The booking flow is:

1. Begin a database transaction.
2. Acquire a write lock on the selected slot.
3. Validate the slot and requested ticket count.
4. Check available capacity.
5. Update the booked count.
6. Create the booking.
7. Commit the transaction.

If insufficient capacity remains, reject the booking. Booking creation and the booked count update must succeed or fail together.

### 2. Safe Retries and Idempotency

Clients must provide an idempotency key for each new booking operation.

When a request is retried because of a network failure:

- The client must reuse the original key and payload.
- The service identifies an existing booking associated with the key.
- The original booking result is returned.
- The booked count is not incremented again.

A database-level unique constraint on the idempotency key prevents duplicate booking records.

Reusing a key with different booking details should be rejected.

### 3. Booking Cancellation

A booking can be cancelled only before its associated slot starts.

Cancellation rules:

- A confirmed booking can be cancelled before the slot starts.
- The booked count is decreased by the booking's ticket count.
- The booking status is changed to `CANCELLED`.
- Both updates occur within the same database transaction.
- A booking cannot be cancelled once its slot has started.
- Repeated cancellation returns the existing cancelled booking without releasing tickets again.

Cancellation must use appropriate database locking to prevent concurrent operations from releasing the same tickets more than once.

### 4. Receipt Number Generation

Each booking receives a unique receipt number in the following format:

```text
{TEMPLE_CODE}/{FINANCIAL_YEAR}/{SEQUENCE}
```

Example:

```text
SBM/2026-27/000123
```

Financial years run from April through March.

| Booking Date | Financial Year |
|---|---|
| 15 March 2027 | `2026-27` |
| 1 April 2027 | `2027-28` |
| 28 October 2026 | `2026-27` |

Receipt sequences are maintained independently for each temple and financial year.

The proposed implementation uses a dedicated `temple_receipt_sequence` table with pessimistic locking and a unique constraint on the temple and financial-year combination.

Receipt number allocation and booking creation should occur in the same database transaction. Issued receipt numbers must never be reused, including for cancelled bookings.

## Database Design

The core database tables are expected to include the following.

### `temple_master`

Stores temple information, including the temple code used for receipt generation.

### `slot_master`

Stores reusable slot definitions, such as slot names, start times, end times, and default capacity.

### `darshan_slot`

Stores date-specific slots, including temple, slot definition, capacity, booked count, and active status.

### `darshan_booking`

Stores booking information, including devotee details, ticket count, booking status, idempotency key, and receipt number.

### `temple_receipt_sequence`

Maintains the last allocated receipt sequence for each temple and financial year.

**Important constraints:**

- Slot definitions must belong to the correct temple.
- A date-specific slot must not be duplicated for the same slot definition and date.
- The booked count must not exceed slot capacity or fall below zero.
- Each idempotency key must be unique.
- Each receipt number must be unique.
- Each temple and financial year must have only one sequence record.

Apply these constraints through database migrations and validate that the existing schema matches the application entities.

## Error Handling

The API should return appropriate HTTP status codes for common errors.

| HTTP Status | Scenario |
|---|---|
| `400 Bad Request` | Invalid request or ticket count |
| `404 Not Found` | Slot or booking does not exist |
| `409 Conflict` | Insufficient capacity, slot inactive, cancellation not permitted, or idempotency key reused with different details |
| `500 Internal Server Error` | Unexpected server or database failure |

Use a centralized exception handler, such as `@RestControllerAdvice`, to maintain a consistent error response structure.

## Testing

Run the unit and integration tests with:

```bash
mvn test
```

Build and verify the application with:

```bash
mvn clean verify
```

Important test scenarios include:

- Creating slots and bookings.
- Rejecting bookings that exceed available capacity.
- Concurrent booking requests against a limited-capacity slot.
- Retrying the same booking request without creating a duplicate.
- Rejecting reuse of an idempotency key with different booking details.
- Cancelling a booking and restoring ticket availability.
- Rejecting cancellation after the slot starts.
- Preventing duplicate ticket release on repeated cancellation.
- Generating receipt numbers for the correct financial year.
- Maintaining independent receipt sequences per temple and financial year.
- Handling concurrent receipt allocation.
- Rolling back booking and capacity changes when a transaction fails.

For concurrency tests, use independent transactions against the actual database implementation where practical.

## Design Decisions

| Requirement | Approach | Reason |
|---|---|---|
| Prevent overbooking | Pessimistic database locking | Protects slot capacity across application instances sharing the database |
| Prevent duplicate bookings | Idempotency key and unique constraint | Makes retries safe after network failures |
| Safe cancellation | Transactional status and counter updates | Prevents partial updates and duplicate ticket release |
| Unique receipt numbers | Dedicated sequence table and pessimistic locking | Maintains independent sequences per temple and financial year |
| Data consistency | Database constraints and transactions | Protects business rules at the persistence layer |

## Future Enhancements

Potential enhancements include:

- API authentication and authorization.
- OpenAPI/Swagger documentation.
- Docker and Docker Compose support.
- CI/CD pipeline integration.
- Structured logging and monitoring.
- Rate limiting and request tracing.
- Audit reporting for receipt issuance and cancellation.
- Integration with payment services.

## Development Guidelines

- Follow existing package and naming conventions.
- Keep controllers thin and business logic in service classes.
- Use DTOs for request and response payloads.
- Apply transactions to operations that modify related database records.
- Enforce critical business constraints at the database level.
- Avoid in-memory synchronization for operations that must work across multiple application instances.
- Add tests for new business rules and concurrency-sensitive operations.
- Never commit credentials, passwords, or production configuration secrets.

## License

Add the applicable license information for the project.