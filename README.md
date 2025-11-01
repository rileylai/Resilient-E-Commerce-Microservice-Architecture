# COMP5348 Tutorial 2 - Group 3 Application

A distributed microservices application for online store management with order processing, payment, and delivery services.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Node.js and npm (for frontend)

## Architecture

The application consists of the following microservices:

- **Store**: Main store service handling products and orders
- **Bank**: Payment processing service
- **Warehouse**: Inventory management service
- **DeliveryCo**: Order delivery service
- **EmailService**: Email notification service
- **Frontend**: React-based user interface
- **RabbitMQ**: Message broker for inter-service communication

## Quick Start

### 1. Start RabbitMQ

Use Docker Compose to start RabbitMQ.

**Configuration:**
- AMQP Port: `5672`
- Management UI: `http://localhost:15672`
- Username: `admin`
- Password: `admin`

### 2. Start Backend Services

Start each service using Maven Spring Boot plugin.

**Service Ports:**
- Store Service: `8080`
- Bank Service: `8081`
- Warehouse Service: `8082`
- Delivery Service: `8083`
- Email Service: `8084`

### 3. Start Frontend

Install dependencies and start the React application.

**Configuration:**
- Frontend URL: `http://localhost:3000`

## Database

All services use H2 in-memory database. Schema and initial data are automatically created on startup from `schema.sql` and `data.sql` files in each service's resources directory.

## Additional Documentation

For detailed project report and implementation details, see `5348_tut2_group3_report.md`.
