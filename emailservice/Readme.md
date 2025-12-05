# EmailService Microservice

Enterprise-grade Spring Boot service that powers transactional notifications, renders templated content, and dispatches user-facing emails whenever marketplace events occur. It consumes RabbitMQ messages from Store, DeliveryCo, and Bank, applies idempotent guards, and relays status updates to customer inboxes without blocking core transaction flows.

---

## Overview
- **Purpose**: Centralized notification hub that owns email formatting, delivery orchestration, and observability, ensuring every critical order, payment, and delivery event reaches the customer.
- **Scope**: Operates asynchronously over RabbitMQ, listens to `store.exchange` and `delivery.exchange`, and delivers branded templates for confirmations, payment failures, refunds, and delivery checkpoints to enhance user trust.

---

## Tech Stack
- **Spring Boot 3** (AMQP listeners, scheduling, config management)
- **Java 17**
- **RabbitMQ 3.x** (topic exchanges, durable queues)
- **MyBatis-Plus** (optional audit/event logging in MySQL 8+)
- **Lombok** (concise DTOs/services)
- **Logback** (structured notification telemetry)
- **Docker** (container packaging and deployment)

---

## Key Responsibilities
1. **Consume notification queues** – Listen to `email.orderfail.queue`, `email.refund.queue`, and `delivery.email.queue` (a.k.a. `email.delivery.queue`) for delivery, refund, and failure events.
2. **Parse structured payloads** – Deserialize JSON DTOs (`DeliveryStatusUpdateDTO`, `RefundNotificationDTO`, `OrderFailureNotificationDTO`) into internal models for consistent template rendering.
3. **Send branded emails** – Build templated content per notification type and dispatch through the SMTP adapter, preserving tone and visual identity.
4. **Audit every send** – Log success/failure events with correlation IDs so operations teams can trace issues and comply with audit requirements.

---

## Architecture & Workflow
- **RabbitMQ listeners**: `EmailNotificationListener` binds to `store.exchange` and `delivery.exchange`, receiving events via durable queues with manual acknowledgment.
- **Event-driven pipeline**: Messages flow listener → email builder → SMTP sender; each stage is isolated so templates, transport, or queue contracts evolve independently.
- **Idempotent processing**: Deduplication keys (`orderId + notificationType + timestamp`) prevent duplicate emails when upstream retries occur; processed IDs are cached/persisted for replay safety.
- **Durable queues**: `email.*.queue` definitions are durable and survive broker restarts; publisher confirms ensure upstream services know when messages land.
- **Retry safeguards**: Transient SMTP failures trigger exponential backoff retries before routing to DLQs, guaranteeing eventual delivery or clear operator visibility.

---

## REST API Usage
Base URL: `http://localhost:8085`

### `GET /api/email/test`
- **Purpose**: Minimal health-check endpoint to verify that the service can render and send a sample email.
- **Typical use**: Platform admins validating SMTP credentials or monitoring probes ensuring the pipeline is alive.
- **Success**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "Email test dispatched",
    "timestamp": "2024-11-21T08:02:03Z"
  }
}
```
- **SMTP failure**
```json
{
  "code": 502,
  "message": "SMTP delivery failed",
  "data": null
}
```

---

## RabbitMQ Integration

- **Delivery queue**: `delivery.email.queue`
- **Order failure queue**: `email.orderfail.queue`
- **Refund queue**: `email.refund.queue`
- **Exchanges**: `delivery.exchange`, `store.exchange`

### Event payloads
**Delivery status (`DeliveryStatusUpdateDTO`)**
```json
{
  "eventType": "DELIVERY_STATUS",
  "orderId": "order001",
  "deliveryId": 7301,
  "newStatus": "IN_TRANSIT",
  "customerEmail": "user@example.com",
  "message": "Your package has departed the depot",
  "timestamp": "2024-11-21T08:02:03Z"
}
```
**Order failure (`OrderFailureNotificationDTO`)**
```json
{
  "eventType": "ORDER_FAILURE",
  "orderId": "order001",
  "customerEmail": "user@example.com",
  "reason": "Payment authorization failed",
  "timestamp": "2024-11-20T10:15:30Z"
}
```
**Refund (`RefundNotificationDTO`)**
```json
{
  "eventType": "REFUND_PROCESSED",
  "orderId": "order001",
  "customerEmail": "user@example.com",
  "amount": 50.00,
  "reason": "Partial refund",
  "timestamp": "2024-11-22T05:11:42Z"
}
```

### Processing lifecycle
1. **Listener** receives a message, validates schema/version, and checks the dedup cache.
2. **Email builder** selects the appropriate template, injects variables, and localizes content based on tenant settings.
3. **SMTP sender** dispatches via the configured provider, wrapping calls in retries + timeout guards.
4. **Audit logger** records the outcome (success, retry, DLQ) with correlation metadata for observability.
5. **Acknowledgment** occurs only after logging succeeds, ensuring messages are never lost silently.

### Example message flow
```text
Store / DeliveryCo / Bank -> [email.*.queue] -> EmailService -> User Inbox
```
EmailService decouples customer communications from payment, fulfillment, and store logic so upstream services stay focused on core transactions.

---

## Fault Tolerance & Reliability
- **Retry/backoff**: Automatic retries with exponential backoff for transient SMTP or template rendering issues; configurable attempt counts per notification type.
- **Dead-letter queues**: Exhausted messages land in `email.dlq` for manual replay, keeping hot queues unblocked.
- **Broker resilience**: Durable queues + publisher confirms + transactional acknowledgments ensure no mail request is lost during RabbitMQ downtime.
- **Structured logging**: Logback emits JSON logs with correlation IDs, enabling centralized monitoring, alerting, and compliance audits.

---

## Configuration
- **SMTP**: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_USE_TLS` drive outbound delivery; values live in env vars or `application.properties`.
- **Sender identity**: `EMAIL_FROM_ADDRESS`, `EMAIL_FROM_NAME` ensure consistent branding across all notifications.
- **Templates**: `EMAIL_TEMPLATE_PATH` or database-backed templates managed by MyBatis-Plus for auditability.
- **Queues & exchanges**: `spring.rabbitmq.*`, `delivery.exchange`, `store.exchange`, routing keys (`notification.email`, `email.orderfail`, `email.refund`).
- **Observability**: `logging.level.com.tut2.group3.emailservice`, `logging.pattern.console` customize verbosity for prod vs. staging.

---

## Project Structure
```
src/main/java/com/tut2/group3/emailservice
├── EmailServiceApplication.java  # Spring Boot bootstrap
├── config/                      # RabbitMQ + converter configuration
├── dto/                         # Notification payload contracts
├── listener/                    # RabbitMQ consumers
├── service/                     # Email builder + SMTP facade
└── resources/                   # Application config, templates
```

---

## Setup Instructions
1. **Clone** the repo and `cd emailservice`.
2. **Configure SMTP & RabbitMQ** settings in `src/main/resources/application.properties` or env vars.
3. **Provision queues/exchanges** (`delivery.exchange`, `store.exchange`, `delivery.email.queue`, `email.orderfail.queue`, `email.refund.queue`).
4. **Build**: `mvn clean install`.
5. **Run**: `mvn spring-boot:run` (listens on port `8085`).
6. **Smoke test**: `curl http://localhost:8085/api/email/test`.

---

## Notes
- EmailService only sends for delivery statuses PICKED_UP, IN_TRANSIT, DELIVERED, or LOST to avoid noisy updates.
- Refund notifications are idempotent per `orderId + amount + reason`; duplicates are dropped on ingestion.
- Structured logs and audit tables make it easy to prove delivery or diagnose SMTP provider issues.

