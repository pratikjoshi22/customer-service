<div align="center">

# Customer Registration Service

**A Kafka-powered microservice for customer onboarding**

Built with Spring Boot | Redpanda (Kafka) | PostgreSQL

[![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Kafka-Redpanda-E04E39?logo=apachekafka&logoColor=white)](https://redpanda.com/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## How It Works

```
                        POST /api/customers/register
                                    |
                                    v
                     +------------------------------+
                     |    Customer Service (8080)    |
                     |                              |
                     |  1. Validate (duplicate?)    |
                     |  2. Generate ORN             |
                     |  3. Save to PostgreSQL       |
                     |  4. Publish to Kafka         |
                     |  5. Return ORN               |
                     +------------------------------+
                          |                  |
                          v                  v
                  +-------------+    +-----------------+
                  | PostgreSQL  |    |    Redpanda      |
                  |  (5433)     |    |    (19092)       |
                  | - customers |    | topic:           |
                  | - addresses |    | customer-        |
                  +-------------+    | registration     |
                                     +-----------------+
                                          |
                            +-------------+-------------+
                            v             v             v
                        KYC Service   Notif Service  Account Setup
                        (future)      (future)       (future)
```

---

## Quick Start

> **Prerequisites:** Java 17+, Maven 3.8+, Docker Desktop

```bash
# 1. Clone
git clone https://github.com/pratikjoshi22/customer-service.git
cd customer-service

# 2. Start infrastructure (Redpanda + PostgreSQL)
docker-compose up -d

# 3. Build & run
mvn spring-boot:run
```

**That's it.** The service is live at `http://localhost:8080`.

---

## Try It

### Register a customer

```bash
curl -s -X POST http://localhost:8080/api/customers/register \
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
    "addresses": [{
      "type": "HOME",
      "line1": "42 MG Road",
      "city": "Pune",
      "state": "Maharashtra",
      "postalCode": "411001",
      "country": "IN"
    }],
    "preferredLanguage": "en",
    "marketingOptIn": true,
    "registeredVia": "WEB"
  }' | python3 -m json.tool
```

```json
{
    "orderRefNo": "CUS0000A3F8B2C1"
}
```

### Verify the data

```bash
# Check PostgreSQL
docker exec -i postgres psql -U customer -d customerdb \
  -c "SELECT order_ref_no, first_name, last_name, email FROM customers;"

# Browse Kafka messages
# Open http://localhost:8090 -> Topics -> customer-registration
```

---

## Project Structure

```
customer-service/
├── docker-compose.yml                         # Redpanda + PostgreSQL
├── pom.xml                                    # Dependencies
│
└── src/main/
    ├── java/com/example/customerservice/
    │   ├── config/
    │   │   └── KafkaConsumerConfig             # Kafka wiring
    │   ├── controller/
    │   │   └── CustomerController              # REST endpoint
    │   ├── listener/
    │   │   └── CustomerRegistrationListener    # Kafka consumer (for downstream)
    │   ├── model/
    │   │   ├── dto/
    │   │   │   ├── AddressDto                  # Address payload
    │   │   │   ├── CustomerRegistrationEvent   # Kafka event
    │   │   │   └── CustomerRegistrationRequest # REST request body
    │   │   └── entity/
    │   │       ├── Address                     # JPA entity
    │   │       └── Customer                    # JPA entity with ORN
    │   ├── repository/
    │   │   └── CustomerRepository              # Spring Data JPA
    │   ├── service/
    │   │   └── CustomerRegistrationService     # Business logic + Kafka producer
    │   └── util/
    │       └── OrderReferenceGenerator         # ORN generator (Base36)
    │
    └── resources/
        ├── application.yml                     # Main config
        ├── application-h2.yml                  # H2 profile
        └── application.properties.reference    # YAML ↔ properties mapping
```

---

## Order Reference Number (ORN)

Every customer gets a unique, server-generated identifier:

```
CUS0000A3F8B2C1
│      │
│      └── 8 Base36 chars (0-9, A-Z) = 2.8 trillion combinations
└── Fixed prefix (customer domain)
```

| Property | Detail |
|---|---|
| Format | `CUS0000XXXXXXXX` |
| Length | 15 characters (fixed) |
| Character set | `0-9`, `A-Z` (Base36) |
| Uniqueness | 2.8 trillion combinations |
| Used as | Kafka message key, DB unique index |
| Generated by | `OrderReferenceGenerator` (SecureRandom) |

---

## Infrastructure

All infrastructure runs in Docker. One command to start:

```bash
docker-compose up -d
```

| Service | Port | URL | Purpose |
|---|---|---|---|
| **Redpanda** | `19092` | — | Kafka-compatible message broker |
| **Redpanda Console** | `8090` | http://localhost:8090 | Browse topics, messages, consumer groups |
| **PostgreSQL** | `5433` | — | Customer data persistence |
| **Spring Boot App** | `8080` | http://localhost:8080 | REST API |

### Database connection

| Property | Value |
|---|---|
| Host | `localhost` |
| Port | `5433` |
| Database | `customerdb` |
| Username | `customer` |
| Password | `customer123` |
| JDBC URL | `jdbc:postgresql://localhost:5433/customerdb` |

---

## Configuration

Everything is config-driven via `application.yml`. No hardcoded values in Java.

### Switch databases

```bash
# PostgreSQL (default)
mvn spring-boot:run

# H2 in-memory (no Docker needed)
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

### Key properties

```yaml
spring.kafka.bootstrap-servers: localhost:19092
spring.kafka.producer.value-serializer: JsonSerializer
spring.datasource.url: jdbc:postgresql://localhost:5433/customerdb
app.kafka.topic.customer-registration: customer-registration
```

See `application.properties.reference` for the complete YAML ↔ properties mapping.

---

## API Reference

### `POST /api/customers/register`

Register a new customer.

**Request body:**

| Field | Type | Required | Example |
|---|---|---|---|
| `source` | String | Yes | `"WEB"`, `"MOBILE"`, `"BRANCH"` |
| `firstName` | String | Yes | `"Pratik"` |
| `lastName` | String | Yes | `"Joshi"` |
| `email` | String | Yes | `"pratik@example.com"` |
| `phoneNumber` | String | Yes | `"+91-9876543210"` |
| `dateOfBirth` | String (ISO) | Yes | `"1995-06-15"` |
| `gender` | String | Yes | `"MALE"` |
| `nationality` | String | Yes | `"IN"` |
| `governmentId` | String | Yes | `"ABCDE1234F"` |
| `governmentIdType` | String | Yes | `"PAN"`, `"AADHAAR"`, `"SSN"` |
| `addresses` | Array | Yes | See below |
| `preferredLanguage` | String | Yes | `"en"` |
| `marketingOptIn` | Boolean | Yes | `true` |
| `registeredVia` | String | Yes | `"WEB"` |

**Address object:**

| Field | Type | Example |
|---|---|---|
| `type` | String | `"HOME"`, `"WORK"`, `"BILLING"` |
| `line1` | String | `"42 MG Road"` |
| `line2` | String | `null` |
| `city` | String | `"Pune"` |
| `state` | String | `"Maharashtra"` |
| `postalCode` | String | `"411001"` |
| `country` | String | `"IN"` |

**Response:** `201 Created`

```json
{
  "orderRefNo": "CUS0000A3F8B2C1"
}
```

**Error:** `500` if email already exists.

---

## Roadmap

- [x] Kafka consumer + listener
- [x] PostgreSQL migration (from H2)
- [x] ORN generation (CUS0000XXXXXXXX)
- [x] REST endpoint + Kafka producer
- [x] Config-driven setup (YAML + profiles)
- [ ] Journey + Activity tracking
- [ ] Dashboard frontend
- [ ] Input validation
- [ ] Dead Letter Topic (DLT)
- [ ] Downstream subscriber services

---

<div align="center">

**Built with Cursor IDE**

</div>
