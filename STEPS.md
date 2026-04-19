# Customer Registration Kafka Service — Step-by-Step Guide

Work through these steps in order. Each step tells you which file to open, what to add, and how to verify before moving on.

---

## Step 0: Start Kafka (Redpanda)

Before any code, get Kafka running locally.

```bash
cd customer-service
docker-compose up -d
```

**Verify:** Open http://localhost:8090 — you should see the Redpanda Console with an empty cluster.

> If you don't have Docker, install Docker Desktop first: https://www.docker.com/products/docker-desktop

---

## Step 1: AddressDto — The Address Payload

**File:** `src/main/java/com/example/customerservice/model/dto/AddressDto.java`

**What to do:**

1. Add these private fields:
   - `String type` — "HOME", "WORK", or "BILLING"
   - `String line1`
   - `String line2`
   - `String city`
   - `String state`
   - `String postalCode`
   - `String country`

2. Add a **no-arg constructor** (Jackson needs this to deserialize JSON)

3. Add **getters and setters** for every field

**Why:** This class represents one address inside the Kafka JSON message. Jackson will map JSON fields to these Java fields by name.

**Verify:** The file compiles with no errors.

---zx
## Step 2: CustomerRegistrationEvent — The Main Kafka Payload

**File:** `src/main/java/com/example/customerservice/model/dto/CustomerRegistrationEvent.java`

**What to do:**

1. Add these private fields:

   ```
   // Metadata
   String eventId
   String source

   // Basic info
   String firstName
   String lastName
   String email
   String phoneNumber

   // Extended info
   LocalDate dateOfBirth
   String gender
   String nationality
   String governmentId
   String governmentIdType

   // Addresses
   List<AddressDto> addresses

   // Preferences
   String preferredLanguage
   boolean marketingOptIn

   // Registration context
   String registeredVia
   long registeredAtEpoch
   ```

2. Add a **no-arg constructor**

3. Add **getters and setters** for every field

**Why:** This is the exact shape of JSON that arrives on the Kafka topic. The field names must match the JSON keys.

**Verify:** The file compiles. The imports for `LocalDate` and `List` are already there.

---

## Step 3: Address Entity — JPA Database Model

**File:** `src/main/java/com/example/customerservice/model/entity/Address.java`

**What to do:**

1. Add `@Entity` and `@Table(name = "addresses")` annotations to the class

2. Add these fields with annotations:
   ```java
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   private String type;
   private String line1;
   private String line2;
   private String city;
   private String state;
   private String postalCode;
   private String country;
   ```

3. Add the relationship back to Customer:
   ```java
   @ManyToOne
   @JoinColumn(name = "customer_id")
   private Customer customer;
   ```

4. Add **getters and setters** for all fields (including `customer`)

**Why:** `@ManyToOne` tells JPA that many addresses belong to one customer. The `customer_id` column in the `addresses` table is the foreign key.

**Verify:** File compiles. Don't worry if `Customer` shows a warning — you'll implement it next.

---

## Step 4: Customer Entity — JPA Database Model

**File:** `src/main/java/com/example/customerservice/model/entity/Customer.java`

**What to do:**

1. Add `@Entity` and `@Table(name = "customers")` annotations to the class

2. Add these fields:
   ```java
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long id;

   @Column(unique = true)
   private String eventId;

   private String firstName;
   private String lastName;

   @Column(unique = true)
   private String email;

   private String phoneNumber;
   private LocalDate dateOfBirth;
   private String gender;
   private String nationality;
   private String governmentId;
   private String governmentIdType;
   private String preferredLanguage;
   private boolean marketingOptIn;
   private String registeredVia;
   ```

3. Add the one-to-many relationship:
   ```java
   @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
   private List<Address> addresses;
   ```

4. Add **getters and setters** for everything

**Why:**
- `@Column(unique = true)` on `eventId` prevents processing the same Kafka message twice (idempotency)
- `@Column(unique = true)` on `email` prevents duplicate customers
- `cascade = CascadeType.ALL` means when you save a Customer, its Addresses are saved automatically
- `mappedBy = "customer"` points to the `customer` field in the Address entity

**Verify:** Both entity files compile with no errors.

---

## Step 5: CustomerRepository — Database Access

**File:** `src/main/java/com/example/customerservice/repository/CustomerRepository.java`

**What to do:**

1. Change the interface declaration to extend `JpaRepository`:
   ```java
   public interface CustomerRepository extends JpaRepository<Customer, Long> {
   ```

2. Add this method signature inside:
   ```java
   boolean existsByEmail(String email);
   ```

**Why:** Spring Data JPA auto-generates the SQL query from the method name. `existsByEmail` becomes `SELECT EXISTS(SELECT 1 FROM customers WHERE email = ?)`. You don't write any implementation — Spring does it for you.

**Verify:** File compiles. That's it — this is the simplest step.

---

## Step 6: KafkaConsumerConfig — Wire Up the Consumer

**File:** `src/main/java/com/example/customerservice/config/KafkaConsumerConfig.java`

**What to do:**

1. Add `@EnableKafka` and `@Configuration` annotations to the class

2. Inject the bootstrap server address:
   ```java
   @Value("${spring.kafka.bootstrap-servers}")
   private String bootstrapServers;
   ```

3. Create the consumer factory bean:
   ```java
   @Bean
   public ConsumerFactory<String, CustomerRegistrationEvent> consumerFactory() {
       // 1. Create a JsonDeserializer for our event class
       JsonDeserializer<CustomerRegistrationEvent> deserializer =
               new JsonDeserializer<>(CustomerRegistrationEvent.class);
       deserializer.addTrustedPackages("*");

       // 2. Set consumer properties
       Map<String, Object> props = new HashMap<>();
       props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
       props.put(ConsumerConfig.GROUP_ID_CONFIG, "customer-registration-group");
       props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

       // 3. Return factory with String key deserializer and JSON value deserializer
       return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
   }
   ```

4. Create the listener container factory bean:
   ```java
   @Bean
   public ConcurrentKafkaListenerContainerFactory<String, CustomerRegistrationEvent>
           kafkaListenerContainerFactory() {
       ConcurrentKafkaListenerContainerFactory<String, CustomerRegistrationEvent> factory =
               new ConcurrentKafkaListenerContainerFactory<>();
       factory.setConsumerFactory(consumerFactory());
       factory.setConcurrency(3);
       return factory;
   }
   ```

**You'll need these imports:**
```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import java.util.HashMap;
import java.util.Map;
```

**Why:**
- `ConsumerFactory` tells Spring how to create Kafka consumers (what to connect to, how to deserialize)
- `ConcurrentKafkaListenerContainerFactory` manages the listener threads — concurrency of 3 means 3 threads pulling from 3 partitions in parallel
- `"earliest"` means on first startup, read from the beginning of the topic (don't skip old messages)

**Verify:** File compiles with all the imports resolved.

---

## Step 7: CustomerRegistrationService — Business Logic

**File:** `src/main/java/com/example/customerservice/service/CustomerRegistrationService.java`

**What to do:**

1. Add `@Service` annotation to the class

2. Inject `CustomerRepository` via constructor:
   ```java
   private final CustomerRepository customerRepository;

   public CustomerRegistrationService(CustomerRepository customerRepository) {
       this.customerRepository = customerRepository;
   }
   ```

3. Implement the main method:
   ```java
   @Transactional
   public void processRegistration(CustomerRegistrationEvent event) {
       // 1. Duplicate check
       if (customerRepository.existsByEmail(event.getEmail())) {
           // log warning and return, or throw exception — your choice
           return;
       }

       // 2. Map DTO to entity
       Customer customer = mapToEntity(event);

       // 3. Save
       customerRepository.save(customer);
   }
   ```

4. Implement the mapping helper:
   ```java
   private Customer mapToEntity(CustomerRegistrationEvent event) {
       Customer c = new Customer();
       // Map each field from event → c
       // e.g. c.setFirstName(event.getFirstName());

       // Map addresses: for each AddressDto, create an Address entity
       // IMPORTANT: set the back-reference → address.setCustomer(c)

       return c;
   }
   ```

**You'll need to add:** `import org.springframework.transaction.annotation.Transactional;`

**Why:**
- `@Transactional` ensures the save + address inserts happen atomically (all or nothing)
- The duplicate check prevents re-processing if the same message is consumed twice
- `address.setCustomer(c)` is critical — without it, JPA doesn't know which customer the address belongs to

**Verify:** File compiles. The `mapToEntity` method should set every field you defined in Step 4.

---

## Step 8: CustomerRegistrationListener — The Kafka Entry Point

**File:** `src/main/java/com/example/customerservice/listener/CustomerRegistrationListener.java`

**What to do:**

1. Add `@Component` annotation to the class

2. Inject the service via constructor:
   ```java
   private final CustomerRegistrationService registrationService;

   public CustomerRegistrationListener(CustomerRegistrationService registrationService) {
       this.registrationService = registrationService;
   }
   ```

3. Add the listener method:
   ```java
   @KafkaListener(
       topics = "${app.kafka.topic.customer-registration}",
       groupId = "customer-registration-group",
       containerFactory = "kafkaListenerContainerFactory"
   )
   public void onCustomerRegistration(
           @Payload CustomerRegistrationEvent event,
           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
           @Header(KafkaHeaders.OFFSET) long offset) {

       // 1. Log what you received
       // 2. Call registrationService.processRegistration(event)
   }
   ```

**Why:** This is where everything comes together:
- Spring Kafka polls the `customer-registration` topic
- When a message arrives, it deserializes the JSON into `CustomerRegistrationEvent`
- Your method receives the event + metadata (which partition, which offset)
- You hand it off to the service layer for processing

**Verify:** File compiles.

---

## Step 9: Build & Run

```bash
# Make sure Redpanda is running
docker-compose up -d

# Build the project
./mvnw clean compile

# If build succeeds, run it
./mvnw spring-boot:run
```

You should see in the logs:
- Spring Boot starting
- Kafka consumer connecting to `localhost:9092`
- Consumer group `customer-registration-group` joining

---

## Step 10: Test It

### Create the topic (if auto-create didn't do it)

```bash
docker exec -it redpanda rpk topic create customer-registration --partitions 3
```

### Send a test message

```bash
docker exec -it redpanda rpk topic produce customer-registration
```

Then paste this JSON and press Enter:

```json
{"eventId":"evt-001","source":"WEB","firstName":"Pratik","lastName":"Joshi","email":"pratik@example.com","phoneNumber":"+91-9876543210","dateOfBirth":"1995-06-15","gender":"MALE","nationality":"IN","governmentId":"ABCDE1234F","governmentIdType":"PAN","addresses":[{"type":"HOME","line1":"42 MG Road","city":"Pune","state":"Maharashtra","postalCode":"411001","country":"IN"}],"preferredLanguage":"en","marketingOptIn":true,"registeredVia":"WEB","registeredAtEpoch":1776300000}
```

Press `Ctrl+C` to exit the producer.

### Check the results

1. **Redpanda Console** — http://localhost:8090 → Topics → `customer-registration` → you'll see your message
2. **H2 Console** — http://localhost:8080/h2-console → connect with JDBC URL `jcustomerdbdbc:h2:mem:`, user `sa`, no password → query `SELECT * FROM customers`
3. **App logs** — you should see your log message from the listener

---

## Quick Reference: Data Flow

```
You (rpk produce) → Kafka Topic → Listener → Service → Repository → H2 Database
                         ↑                                              ↓
                   Redpanda Console                              H2 Console
                   (see messages)                              (see saved data)
```

---

## Bonus Challenges (after everything works)

1. **Add error handling** — What happens if `processRegistration` throws? Add try/catch in the listener and log the error.
2. **Add a REST endpoint** — Create a `GET /customers` endpoint that returns all customers from the DB.
3. **Add a producer** — Create a `POST /register` REST endpoint that publishes to the Kafka topic (instead of using `rpk`).
4. **Dead Letter Topic** — Configure Spring Kafka's `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` so failed messages go to a `customer-registration-dlt` topic.
