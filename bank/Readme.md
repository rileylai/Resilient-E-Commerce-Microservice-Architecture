# Bank Service README

## 1. Project Overview
This bank service is part of our small e-commerce platform. It manages payment tasks for customer orders. The service handles debit and refund logic, talks to the database, protects calls with JWT, and exchanges messages with RabbitMQ. This document gives you a quick start guide.

## 2. Main Features
- **Debit flow**: When the store asks for a debit, we check the user account, confirm the currency, and make sure enough balance exists. If the debit passes, we subtract money and mark the transaction as `SUCCEEDED`. If not, we mark it as `FAILED`.
- **Refund flow**: When the store asks for a refund, we find the original debit, check if it succeeded, and ensure the refund amount is not larger than the original amount. We then add money back to the account and mark the refund as `SUCCEEDED`, or mark it as `FAILED` if something goes wrong.
- **Idempotency**: Each message from RabbitMQ carries an `idempotencyKey`. We ignore duplicate messages so we do not charge or refund twice. This makes the system safe during retries.

## 3. Tech Stack
- **Java 17**
- **Spring Boot 3**
- **MyBatis-Plus** for database mapper
- **MySQL** as data store
- **RabbitMQ** for asynchronous events
- **JWT (JSON Web Token)** for API security

## 4. Folder Structure and Key Files
```
bank/
├── pom.xml                           # Maven build file
├── docker-compose.yml                # Optional local services (MySQL, RabbitMQ)
├── src/main/java/com/tut2/group3/bank
│   ├── BankApplication.java          # Spring Boot main class
│   ├── config/                       # Security + RabbitMQ configuration
│   ├── consumer/                     # RabbitMQ listeners
│   ├── dto/                          # Data transfer objects
│   ├── entity/                       # Database entities
│   ├── producer/                     # RabbitMQ event publisher
│   ├── repository/                   # MyBatis-Plus mappers
│   ├── service/                      # Service interfaces and logic
│   └── controller/                   # REST endpoints
└── src/main/resources
    └── application.properties        # Application configuration
```

## 5. How to Set Up Locally
1. Install **Java 17** and **Maven 3.9+**.
2. Install **MySQL 8+** and **RabbitMQ 3.12+** (Docker is fine).
3. Create a database named `5348_bank_service_db`.
4. Load the schema from your SQL scripts (ask the team for the latest file).
5. Copy the sample configuration below into `src/main/resources/application.properties` (adjust passwords to match your setup).
6. Optional: use the provided `docker-compose.yml` to start MySQL and RabbitMQ quickly.

### Sample Configuration
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/5348_bank_service_db?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_mysql_password

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

bank.rabbitmq.queue-name=bank.events.queue
bank.rabbitmq.request-queue=bank.transaction.requests
```

## 6. How to Run the Project
1. Start MySQL and RabbitMQ.
2. In the `bank` folder, run:
   ```bash
   mvn spring-boot:run
   ```
3. The service runs on `http://localhost:8080`.
4. Check the logs to confirm successful startup.

## 7. How to Test the API (with Postman)
1. Open Postman and create a new collection.
2. Add an **Authorization** header with `Type: Bearer Token`.
3. Paste your JWT token into the token field (see section 9 for how to get it).
4. Create a **POST** request to `http://localhost:8080/api/bank/debit`.
5. Use the sample JSON body shown in section 8.
6. Send the request and review the response. A 200 code means success.
7. Repeat the same steps for the refund endpoint.

## 8. API Endpoints
| Method | Path                | Description                       | Body Example |
|--------|---------------------|-----------------------------------|--------------|
| POST   | `/api/bank/debit`   | Debit a user account              | ```json<br>{<br>  "orderId": "order001",<br>  "userId": "1",<br>  "amount": 50.00,<br>  "currency": "AUD"<br>}``` |
| POST   | `/api/bank/refund`  | Refund a user account             | ```json<br>{<br>  "orderId": "order001,<br>  "amount": 50.00,<br>  "currency": "AUD"<br>}``` |

**Note:** Every request must have the header `Authorization: Bearer <your-token>`.

## 9. How JWT Token Works
- Our API uses JWT to make sure only trusted services call these endpoints.
- A normal flow:
  1. Log in to the auth service (ask team for the correct URL) with username and password.
  2. The auth service returns a JWT string (for example `eyJhbGciOiJIUzI1NiIs...`).
  3. In Postman or curl, set the header `Authorization: Bearer <JWT>`.
  4. The Bank service reads the token using our custom JWT filter (`JWTFilter`).
- Without the token, the service returns HTTP 401 (Unauthorized).

## 10. RabbitMQ Integration
- **Purpose**: RabbitMQ lets the store send payment instructions without waiting. It also gets results asynchronously.
- **Incoming queue**: `bank.transaction.requests`. The store pushes `DebitRequest` or `RefundRequest` events here. Our `TransactionRequestConsumer` listens to this queue.
- **Outgoing queue**: `bank.events.queue`. After the bank processes a debit or refund, it publishes a `TransactionResultEventDTO`.
- **Idempotency**: Each incoming message includes an `idempotencyKey`. If RabbitMQ retries the same message, our handler checks if we already processed a successful transaction for the same order. If yes, we reuse the stored result and avoid a double debit or refund. This keeps money safe.

## 11. Common Problems & Solutions
- **Problem:** `Cannot connect to MySQL`.  
  **Fix:** Check MySQL is running, confirm username/password in `application.properties`, and ensure the port is 3306.
- **Problem:** `RabbitMQ connection refused`.  
  **Fix:** Make sure RabbitMQ is running, the port is 5672, and the credentials are correct.
- **Problem:** `HTTP 401 Unauthorized`.  
  **Fix:** Provide a valid JWT in the `Authorization` header, or request a new token from the auth service.
- **Problem:** `Duplicate debit seen in logs`.  
  **Fix:** Confirm the incoming messages have unique `idempotencyKey`. If the store retries, the bank will skip duplicates but still log them.

---

You are ready to explore the Bank Service. Keep experiments small, watch the logs, and ask the team if you feel unsure. Welcome aboard!
