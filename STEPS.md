# Customer Registration Service

A Spring Boot microservice that registers customers via REST API, persists to PostgreSQL, and publishes events to Kafka (Redpanda) for downstream processing.

---

## Architecture

```
Client (curl / Postman / Frontend)
    │
    ▼
POST /api/customers/register
    │
    ▼
┌──────────────────────────────────────────────────┐
│              Spring Boot Service                  │
│                                                    │
│  Controller                                        │
│    → CustomerRegistrationService                   │
│        → Generate ORN (CUS0000XXXXXXXX)            │
│        → Generate eventId (UUID)                   │
│        → Save Customer + Addresses to PostgreSQL   │
│        → Publish event to Kafka (key = ORN)        │
│    → Return { orderRefNo: "CUS0000XXXXXXXX" }     │
└──────────────────────────────────────────────────┘
         │                          │
         ▼                          ▼
┌──────────────┐         ┌─────────────────────┐
│  PostgreSQL   │         │  Kafka (Redpanda)    │
│  port: 5433   │         │  port: 19092         │
│  - customers  │         │  topic:              │
│  - addresses  │         │  customer-registration│
└──────────────┘         └─────────────────────┘
                                    │
                          ┌─────────┼──────────┐
                          ▼         ▼          ▼
                     KYC Service  Notif.   Account Setup
                     (future)    (future)   (future)
```

---

## Tech Stack

| Component | Technology |
|---|---|
| Framework | Spring Boot 3.2.5 (Java 17) |
| Message Broker | Redpanda (Kafka-compatible) |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Build | Maven |
| Containerization | Docker Compose |

---

## Project Structure

```
src/main/java/com/example/customerservice/
├── CustomerServiceApplication.java        # Entry point
├── config/
│   └── KafkaConsumerConfig.java           # @EnableKafka (config-driven via YAML)
├── controller/
│   └── CustomerController.java            # POST /api/customers/register
├── listener/
│   └── CustomerRegistrationListener.java  # Kafka consumer (disabled — for downstream services)
├── model/
│   ├── dto/
│   │   ├── AddressDto.java                # Address in JSON payload
│   │   ├── CustomerRegistrationEvent.java # Kafka event (published to topic)
│   │   └── CustomerRegistrationRequest.java # REST request body
│   └── entity/
│       ├── Address.java                   # JPA entity (@ManyToOne → Customer)
│       └── Customer.java                  # JPA entity with ORN, unique email
├── repository/
│   └── CustomerRepository.java            # JpaRepository with custom queries
├── service/
│   └── CustomerRegistrationService.java   # Business logic, Kafka producer
└── util/
    └── OrderReferenceGenerator.java       # Generates CUS0000XXXXXXXX (Base36)

src/main/resources/
├── application.yml                        # Main config (PostgreSQL + Kafka)
├── application-h2.yml                     # H2 profile for quick testing
└── application.properties.reference       # YAML-to-properties mapping reference
```

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker Desktop

---

## Quick Start

### 1. Start infrastructure

```bash
docker-compose up -d
```

This starts:
- **Redpanda** (Kafka) on `localhost:19092`
- **Redpanda Console** at `http://localhost:8090`
- **PostgreSQL** on `localhost:5433` (db: `customerdb`, user: `customer`, pass: `customer123`)

### 2. Build & run

```bash
mvn clean compile
mvn spring-boot:run
```

App starts on `http://localhost:8080`.

### 3. Register a customer

```bash
curl -X POST http://localhost:8080/api/customers/register \
  -H "Content-Type: application/json" \
  -d '{
    "source": "WEB",
    "firstName": "Pratik",
    "lastName": "Joshi",
    "email": "pratik@example.com",
    "phoneNumber": "+91-9876543210",
    "dateOfBirth": "1995-06-15",
    "gender": "MALE",
    "nationality": "IN",
    "governmentId": "ABCDE1234F",
    "governmentIdType": "PAN",
    "addresses": [
      {
        "type": "HOME",
        "line1": "42 MG Road",
        "city": "Pune",
        "state": "Maharashtra",
        "postalCode": "411001",
        "country": "IN"
      }
    ],
    "preferredLanguage": "en",
    "marketingOptIn": true,
    "registeredVia": "WEB"
  }'
```

Response:
```json
{"orderRefNo": "CUS0000A3F8B2C1"}
```

### 4. Verify

| What | Where |
|---|---|
| Database | `docker exec -i postgres psql -U customer -d customerdb -c "SELECT order_ref_no, first_name, email FROM customers;"` |
| Kafka messages | Redpanda Console → http://localhost:8090 → Topics → customer-registration |
| App logs | Terminal running `mvn spring-boot:run` |

---

## Configuration

All Kafka and DB config is in `application.yml`. Key properties:

| Property | Value | Purpose |
|---|---|---|
| `spring.kafka.bootstrap-servers` | `localhost:19092` | Redpanda broker |
| `spring.kafka.consumer.group-id` | `customer-registration-group` | Consumer group |
| `spring.kafka.producer.value-serializer` | `JsonSerializer` | Serialize events as JSON |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/customerdb` | PostgreSQL connection |
| `app.kafka.topic.customer-registration` | `customer-registration` | Topic name |

### Profiles

```bash
# PostgreSQL (default)
mvn spring-boot:run

# H2 in-memory (no Docker DB needed)
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

---

## Order Reference Number (ORN)

Format: `CUS0000XXXXXXXX` (15 chars, fixed length)

- `CUS0000` — prefix identifying customer domain
- `XXXXXXXX` — 8 Base36 characters (0-9, A-Z)
- **2.8 trillion** unique combinations
- Generated server-side via `OrderReferenceGenerator`
- Used as the Kafka message key for partition affinity
- Stored as unique indexed column in PostgreSQL

---

## Data Flow

```
POST /api/customers/register (JSON body)
    → Controller receives CustomerRegistrationRequest
    → Service:
        1. Duplicate check (existsByEmail)
        2. Generate ORN (CUS0000XXXXXXXX)
        3. Generate eventId (UUID)
        4. Map request → Customer entity → save to PostgreSQL
        5. Map request → CustomerRegistrationEvent → publish to Kafka
        6. Return ORN
    → Response: 201 { "orderRefNo": "CUS0000XXXXXXXX" }
```

---

## What's Next

- [ ] Journey + Activity tracking (track multi-step workflows by ORN)
- [ ] Dashboard frontend (visualize customer journeys)
- [ ] Dead Letter Topic for failed messages
- [ ] Input validation on REST request
- [ ] Downstream subscriber services (KYC, Account Setup, Notifications)
