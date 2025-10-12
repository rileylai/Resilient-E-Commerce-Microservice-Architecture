# Warehouse Service

## Overview

The Warehouse Service is a critical component of the online store system, responsible for managing inventory across multiple warehouse locations. It provides stock availability checking, inventory management, and stock reservation functionality to support the order fulfillment workflow.

## Core Functionalities

- **Stock Management**: Maintain stock levels for products across multiple warehouses
- **Stock Availability Check**: Determine which warehouse(s) can fulfill an order
- **Stock Reservation**: Reserve stock when orders are placed
- **Stock Release**: Release reserved stock when orders are cancelled
- **Stock Deduction**: Deduct stock levels after successful delivery pickup
- **Multi-warehouse Support**: Handle orders from single or multiple warehouses

## Technology Stack

- **Framework**: Spring Boot 3.5.6
- **Java Version**: 17
- **ORM**: MyBatis-Plus 3.5.14
- **Database**: MySQL
- **Message Queue**: RabbitMQ (Spring AMQP)
- **Authentication**: JWT (java-jwt 4.4.0)
- **Validation**: Spring Boot Starter Validation
- **Utilities**: Lombok

## Architecture

The Warehouse Service follows a layered architecture:

```
├── controller/       # REST API endpoints
├── service/          # Business logic layer
├── mapper/           # MyBatis-Plus data access layer
├── entity/           # Database entities
├── dto/              # Data Transfer Objects
└── config/           # Configuration classes
```

## Database Schema

### Warehouse Table
Stores information about warehouse locations.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-increment) |
| name | VARCHAR(100) | Warehouse name |
| address | VARCHAR(255) | Warehouse address |
| status | VARCHAR(20) | Warehouse status (ACTIVE/INACTIVE) |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

### Product Table
Stores product information.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-increment) |
| name | VARCHAR(200) | Product name |
| description | TEXT | Product description |
| price | DECIMAL(10,2) | Product price |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

### Inventory Table
Stores stock levels for products in each warehouse.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-increment) |
| warehouse_id | BIGINT | Foreign key to Warehouse |
| product_id | BIGINT | Foreign key to Product |
| available_quantity | INT | Available stock quantity |
| reserved_quantity | INT | Reserved stock quantity |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

### StockReservation Table
Tracks stock reservations for orders.

| Column | Type | Description |
|--------|------|-------------|
| id | BIGINT | Primary key (auto-increment) |
| order_id | VARCHAR(100) | Order identifier |
| warehouse_id | BIGINT | Foreign key to Warehouse |
| product_id | BIGINT | Foreign key to Product |
| quantity | INT | Reserved quantity |
| status | VARCHAR(20) | Reservation status (RESERVED/CONFIRMED/RELEASED) |
| created_at | TIMESTAMP | Creation timestamp |
| updated_at | TIMESTAMP | Last update timestamp |

## API Endpoints

### 1. Check Stock Availability

**Endpoint**: `POST /api/warehouse/check-availability`

**Description**: Check if the requested product quantity is available and determine which warehouse(s) can fulfill the order.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "productId": 1,
  "quantity": 10
}
```

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Stock is available",
  "data": {
    "available": true,
    "fulfillmentStrategy": "SINGLE_WAREHOUSE",
    "warehouses": [
      {
        "warehouseId": 1,
        "warehouseName": "Central Warehouse",
        "availableQuantity": 15,
        "allocatedQuantity": 10
      }
    ]
  }
}
```

**Success Response - Multiple Warehouses** (200 OK):
```json
{
  "code": 200,
  "message": "Stock is available from multiple warehouses",
  "data": {
    "available": true,
    "fulfillmentStrategy": "MULTIPLE_WAREHOUSES",
    "warehouses": [
      {
        "warehouseId": 1,
        "warehouseName": "Central Warehouse",
        "availableQuantity": 5,
        "allocatedQuantity": 5
      },
      {
        "warehouseId": 2,
        "warehouseName": "East Warehouse",
        "availableQuantity": 8,
        "allocatedQuantity": 5
      }
    ]
  }
}
```

**Error Response - Insufficient Stock** (200 OK):
```json
{
  "code": 601,
  "message": "Insufficient stock available",
  "data": {
    "available": false,
    "requestedQuantity": 100,
    "totalAvailableQuantity": 23
  }
}
```

**Error Response** (400 Bad Request):
```json
{
  "code": 400,
  "message": "Invalid request parameters",
  "data": null
}
```

### 2. Reserve Stock

**Endpoint**: `POST /api/warehouse/reserve`

**Description**: Reserve stock for an order. This locks the inventory to prevent overselling.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "orderId": "ORD-20251012-001",
  "productId": 1,
  "quantity": 10,
  "warehouses": [
    {
      "warehouseId": 1,
      "quantity": 10
    }
  ]
}
```

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Stock reserved successfully",
  "data": {
    "orderId": "ORD-20251012-001",
    "reservationId": "RES-20251012-001",
    "reservations": [
      {
        "warehouseId": 1,
        "warehouseName": "Central Warehouse",
        "productId": 1,
        "quantity": 10,
        "status": "RESERVED"
      }
    ],
    "timestamp": "2025-10-12T10:30:00Z"
  }
}
```

**Error Response** (409 Conflict):
```json
{
  "code": 602,
  "message": "Stock reservation failed - insufficient stock",
  "data": {
    "orderId": "ORD-20251012-001",
    "reason": "Stock level changed during reservation"
  }
}
```

### 3. Confirm Stock Reservation

**Endpoint**: `POST /api/warehouse/confirm`

**Description**: Confirm the stock reservation after successful payment. This deducts the reserved stock from available inventory.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "orderId": "ORD-20251012-001",
  "reservationId": "RES-20251012-001"
}
```

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Stock reservation confirmed",
  "data": {
    "orderId": "ORD-20251012-001",
    "reservationId": "RES-20251012-001",
    "status": "CONFIRMED",
    "warehouses": [
      {
        "warehouseId": 1,
        "warehouseName": "Central Warehouse",
        "productId": 1,
        "quantity": 10
      }
    ],
    "timestamp": "2025-10-12T10:31:00Z"
  }
}
```

**Error Response** (404 Not Found):
```json
{
  "code": 603,
  "message": "Reservation not found",
  "data": {
    "orderId": "ORD-20251012-001",
    "reservationId": "RES-20251012-001"
  }
}
```

### 4. Release Stock

**Endpoint**: `POST /api/warehouse/release`

**Description**: Release reserved stock when an order is cancelled or payment fails. This returns the reserved stock to available inventory.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "orderId": "ORD-20251012-001",
  "reservationId": "RES-20251012-001",
  "reason": "ORDER_CANCELLED"
}
```

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Stock released successfully",
  "data": {
    "orderId": "ORD-20251012-001",
    "reservationId": "RES-20251012-001",
    "status": "RELEASED",
    "warehouses": [
      {
        "warehouseId": 1,
        "productId": 1,
        "quantity": 10,
        "releasedAt": "2025-10-12T10:35:00Z"
      }
    ]
  }
}
```

### 5. Get Warehouse Stock

**Endpoint**: `GET /api/warehouse/{warehouseId}/stock`

**Description**: Retrieve all stock information for a specific warehouse.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Path Parameters**:
- `warehouseId` (Long): The warehouse identifier

**Query Parameters**:
- `productId` (Long, optional): Filter by specific product
- `page` (int, optional, default: 0): Page number
- `size` (int, optional, default: 20): Page size

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Stock information retrieved",
  "data": {
    "warehouseId": 1,
    "warehouseName": "Central Warehouse",
    "stocks": [
      {
        "productId": 1,
        "productName": "Laptop",
        "availableQuantity": 15,
        "reservedQuantity": 5,
        "totalQuantity": 20
      },
      {
        "productId": 2,
        "productName": "Mouse",
        "availableQuantity": 50,
        "reservedQuantity": 10,
        "totalQuantity": 60
      }
    ],
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 2,
      "totalPages": 1
    }
  }
}
```

### 6. Update Stock Level

**Endpoint**: `PUT /api/warehouse/stock`

**Description**: Manually update stock levels (for inventory replenishment or adjustments).

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "warehouseId": 1,
  "productId": 1,
  "quantity": 100,
  "operation": "ADD"
}
```

**Operations**:
- `ADD`: Add to existing stock
- `SET`: Set absolute stock level
- `SUBTRACT`: Subtract from existing stock

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Stock level updated",
  "data": {
    "warehouseId": 1,
    "productId": 1,
    "previousAvailableQuantity": 15,
    "newAvailableQuantity": 115,
    "operation": "ADD",
    "timestamp": "2025-10-12T11:00:00Z"
  }
}
```

### 7. Get All Warehouses

**Endpoint**: `GET /api/warehouse/list`

**Description**: Retrieve a list of all warehouses.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**:
- `status` (String, optional): Filter by status (ACTIVE/INACTIVE)

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Warehouses retrieved",
  "data": [
    {
      "id": 1,
      "name": "Central Warehouse",
      "address": "123 Main St, Sydney NSW 2000",
      "status": "ACTIVE"
    },
    {
      "id": 2,
      "name": "East Warehouse",
      "address": "456 East Rd, Sydney NSW 2010",
      "status": "ACTIVE"
    }
  ]
}
```

### 8. Health Check

**Endpoint**: `GET /api/warehouse/health`

**Description**: Check the health status of the warehouse service.

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Service is healthy",
  "data": {
    "status": "UP",
    "timestamp": "2025-10-12T12:00:00Z",
    "database": "CONNECTED",
    "messageQueue": "CONNECTED"
  }
}
```

## Message Queue Integration

The Warehouse Service integrates with RabbitMQ to handle asynchronous events.

### Published Events

#### Stock Reserved Event
**Exchange**: `warehouse.exchange`
**Routing Key**: `warehouse.stock.reserved`

**Payload**:
```json
{
  "eventId": "EVT-20251012-001",
  "eventType": "STOCK_RESERVED",
  "timestamp": "2025-10-12T10:30:00Z",
  "orderId": "ORD-20251012-001",
  "reservationId": "RES-20251012-001",
  "productId": 1,
  "quantity": 10,
  "warehouses": [
    {
      "warehouseId": 1,
      "quantity": 10
    }
  ]
}
```

#### Stock Confirmed Event
**Exchange**: `warehouse.exchange`
**Routing Key**: `warehouse.stock.confirmed`

**Payload**:
```json
{
  "eventId": "EVT-20251012-002",
  "eventType": "STOCK_CONFIRMED",
  "timestamp": "2025-10-12T10:31:00Z",
  "orderId": "ORD-20251012-001",
  "reservationId": "RES-20251012-001",
  "warehouses": [
    {
      "warehouseId": 1,
      "productId": 1,
      "quantity": 10
    }
  ]
}
```

#### Stock Released Event
**Exchange**: `warehouse.exchange`
**Routing Key**: `warehouse.stock.released`

**Payload**:
```json
{
  "eventId": "EVT-20251012-003",
  "eventType": "STOCK_RELEASED",
  "timestamp": "2025-10-12T10:35:00Z",
  "orderId": "ORD-20251012-001",
  "reservationId": "RES-20251012-001",
  "reason": "ORDER_CANCELLED"
}
```

### Consumed Events

#### Delivery Pickup Confirmed Event
**Queue**: `warehouse.delivery.pickup`
**Routing Key**: `delivery.pickup.confirmed`

**Payload**:
```json
{
  "eventId": "EVT-DEL-001",
  "eventType": "DELIVERY_PICKUP_CONFIRMED",
  "timestamp": "2025-10-12T10:40:00Z",
  "orderId": "ORD-20251012-001",
  "warehouseId": 1,
  "trackingNumber": "TRK-20251012-001"
}
```

**Action**: Updates reservation status to prevent duplicate pickups.

#### Order Cancelled Event
**Queue**: `warehouse.order.cancelled`
**Routing Key**: `order.cancelled`

**Payload**:
```json
{
  "eventId": "EVT-ORD-001",
  "eventType": "ORDER_CANCELLED",
  "timestamp": "2025-10-12T10:35:00Z",
  "orderId": "ORD-20251012-001",
  "reason": "CUSTOMER_REQUESTED"
}
```

**Action**: Automatically releases reserved stock for the cancelled order.

## Setup and Installation

### Prerequisites
- Java 17 or higher
- MySQL 8.0 or higher
- RabbitMQ 3.x
- Maven 3.6+

### Configuration

Update `application.yml` or `application.properties` with your environment settings:

```yaml
spring:
  application:
    name: warehouse-service

  datasource:
    url: jdbc:mysql://localhost:3306/warehouse_db?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver

  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest

server:
  port: 8082

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.tut2.group3.warehouse.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: your-secret-key-here
  expiration: 86400000
```

### Database Initialization

Run the SQL scripts to create the database schema:

```bash
mysql -u your_username -p warehouse_db < database/schema.sql
mysql -u your_username -p warehouse_db < database/data.sql
```

### Build and Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR file
java -jar target/warehouse-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:8082`

## Testing

### Run Unit Tests
```bash
mvn test
```

### Run Integration Tests
```bash
mvn verify
```

### Sample API Requests

#### Check Stock Availability
```bash
curl -X POST http://localhost:8082/api/warehouse/check-availability \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1,
    "quantity": 10
  }'
```

#### Reserve Stock
```bash
curl -X POST http://localhost:8082/api/warehouse/reserve \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "ORD-20251012-001",
    "productId": 1,
    "quantity": 10,
    "warehouses": [
      {
        "warehouseId": 1,
        "quantity": 10
      }
    ]
  }'
```

## Error Handling

All API endpoints follow a consistent error response format using the `Result<T>` wrapper:

```json
{
  "code": 400,
  "message": "Error description",
  "data": null
}
```

### Response Codes

**Success Codes:**
- `200`: Success

**Common Error Codes:**
- `400`: Bad request - Invalid request parameters
- `401`: Unauthorized - Missing or invalid JWT token
- `404`: Not found - Resource not found
- `409`: Conflict - Business logic conflict
- `500`: Internal server error

**Warehouse-Specific Error Codes:**
- `601`: Insufficient stock available
- `602`: Stock reservation failed
- `603`: Reservation not found
- `604`: Warehouse not found
- `605`: Product not found
- `606`: Stock update failed
- `607`: Invalid quantity

## Logging

The service logs critical information including:
- Stock availability checks with timestamps
- Stock reservations and confirmations
- Stock releases and reasons
- Message queue events sent and received
- Database transaction operations
- Error occurrences with stack traces

Logs are written to:
- Console output (development)
- `logs/warehouse.log` (production)

## Fault Tolerance and Availability

### Stock Reservation Mechanism
- Uses optimistic locking to prevent overselling
- Reservation timeout: 15 minutes (configurable)
- Automatic release of expired reservations

### Message Queue Reliability
- Message acknowledgment enabled
- Retry mechanism for failed messages
- Dead letter queue for unprocessable messages

### Database Transactions
- ACID properties maintained
- Rollback on failure scenarios
- Connection pooling for performance

## Integration with Other Services

### Store Service
- Provides stock availability information
- Handles stock reservations for orders
- Confirms or releases stock based on payment status

### Delivery Service
- Receives pickup confirmation events
- Updates stock status after pickup
- Handles multiple warehouse pickups

## Future Enhancements

- Real-time stock level monitoring dashboard
- Predictive analytics for stock replenishment
- Warehouse performance metrics
- Automated reordering based on stock levels
- Support for inter-warehouse transfers

## Contact and Support

For questions or issues related to the Warehouse Service, please contact the development team or create an issue in the project repository.

---

**Version**: 1.0.0
**Last Updated**: October 12, 2025
**Maintained by**: Tutorial 02 Group 03
