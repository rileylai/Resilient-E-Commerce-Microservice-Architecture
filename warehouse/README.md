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
| version | INT | Optimistic lock version number |
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

### 1. Validate Order

**Endpoint**: `POST /api/warehouse/validate-order`

**Description**: Validate whether all products in an order can be fulfilled by checking stock availability across all warehouses. This endpoint is useful for validating entire orders before proceeding with reservation.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

**Request Body**:
```json
{
  "orderId": "123",
  "items": [
    {
      "productId": 1001,
      "quantity": 2
    },
    {
      "productId": 1002,
      "quantity": 1
    }
  ]
}
```

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "All products are available",
  "data": {
    "orderId": "123",
    "valid": true,
    "validationCode": "SUCCESS",
    "message": "All products are available",
    "productResults": [
      {
        "productId": 1001,
        "productName": "Laptop",
        "requestedQuantity": 2,
        "availableQuantity": 15,
        "available": true,
        "reason": null
      },
      {
        "productId": 1002,
        "productName": "Mouse",
        "requestedQuantity": 1,
        "availableQuantity": 50,
        "available": true,
        "reason": null
      }
    ]
  }
}
```

**Error Response - Insufficient Stock** (400 Bad Request):
```json
{
  "code": 601,
  "message": "Insufficient stock for one or more products",
  "data": {
    "orderId": "123",
    "valid": false,
    "validationCode": "INSUFFICIENT_STOCK",
    "message": "Insufficient stock for one or more products",
    "productResults": [
      {
        "productId": 1001,
        "productName": "Laptop",
        "requestedQuantity": 100,
        "availableQuantity": 15,
        "available": false,
        "reason": "Insufficient stock"
      },
      {
        "productId": 1002,
        "productName": "Mouse",
        "requestedQuantity": 1,
        "availableQuantity": 50,
        "available": true,
        "reason": null
      }
    ]
  }
}
```

**Error Response - Product Not Found** (400 Bad Request):
```json
{
  "code": 601,
  "message": "One or more products not found",
  "data": {
    "orderId": "123",
    "valid": false,
    "validationCode": "PRODUCT_NOT_FOUND",
    "message": "One or more products not found",
    "productResults": [
      {
        "productId": 9999,
        "productName": "Unknown",
        "requestedQuantity": 1,
        "availableQuantity": 0,
        "available": false,
        "reason": "Product not found"
      }
    ]
  }
}
```

**Validation Codes**:
- `SUCCESS`: All products are available in requested quantities
- `INSUFFICIENT_STOCK`: One or more products have insufficient stock
- `PRODUCT_NOT_FOUND`: One or more products do not exist in the system

### 2. Check Stock Availability

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

### 3. Reserve Stock

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

### 4. Confirm Stock Reservation

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

### 5. Release Stock

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

### 6. Get Warehouse Stock

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

### 7. Update Stock Level

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

### 8. Get All Warehouses

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

### 9. Health Check

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

### 10. Get All Products

**Endpoint**: `GET /api/products`

**Description**: Retrieve a list of all products with their complete information.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Success Response** (200 OK):
```json
{
  "code": 200,
  "message": "Found 3 product(s)",
  "data": [
    {
      "id": 1,
      "name": "Laptop",
      "description": "High-performance laptop",
      "price": 1299.99,
      "createdAt": "2025-01-01T10:00:00",
      "updatedAt": "2025-01-01T10:00:00"
    },
    {
      "id": 2,
      "name": "Mouse",
      "description": "Wireless mouse",
      "price": 29.99,
      "createdAt": "2025-01-01T10:05:00",
      "updatedAt": "2025-01-01T10:05:00"
    },
    {
      "id": 3,
      "name": "Keyboard",
      "description": "Mechanical keyboard",
      "price": 89.99,
      "createdAt": "2025-01-01T10:10:00",
      "updatedAt": "2025-01-01T10:10:00"
    }
  ]
}
```

**Success Response - Empty** (200 OK):
```json
{
  "code": 200,
  "message": "No products available",
  "data": []
}
```

### 11. Get Product Price

**Endpoint**: `GET /api/products/price`

**Description**: Query product price by ID or name. Only one parameter should be provided.

**Request Headers**:
```
Authorization: Bearer <JWT_TOKEN>
```

**Query Parameters**:
- `id` (Long, optional): Product ID
- `name` (String, optional): Product name (exact match)

**Note**: Either `id` or `name` must be provided, but not both.

**Success Response - Query by ID** (200 OK):
```
GET /api/products/price?id=1
```
```json
{
  "code": 200,
  "message": "Product price retrieved",
  "data": {
    "id": 1,
    "name": "Laptop",
    "price": 1299.99
  }
}
```

**Success Response - Query by Name** (200 OK):
```
GET /api/products/price?name=Mouse
```
```json
{
  "code": 200,
  "message": "Product price retrieved",
  "data": {
    "id": 2,
    "name": "Mouse",
    "price": 29.99
  }
}
```

**Error Response - Missing Parameter** (400 Bad Request):
```json
{
  "code": 400,
  "message": "Either id or name parameter is required",
  "data": null
}
```

**Error Response - Both Parameters Provided** (400 Bad Request):
```json
{
  "code": 400,
  "message": "Please provide only one parameter: either id or name",
  "data": null
}
```

**Error Response - Product Not Found** (605):
```json
{
  "code": 605,
  "message": "Product not found with id: 999",
  "data": null
}
```

## Message Queue Integration

The Warehouse Service integrates with RabbitMQ to handle asynchronous events and inter-service communication.

### Exchange and Queue Configuration

#### Exchanges
- **warehouse.exchange** (Topic): For publishing warehouse events to other services
- **delivery.exchange** (Topic): For receiving delivery service events
- **order.exchange** (Topic): For receiving order service events
- **warehouse.exchange.dlx** (Direct): Dead letter exchange for failed messages

#### Queues
- **warehouse.delivery.pickup**: Listens to delivery pickup confirmations
- **warehouse.order.cancelled**: Listens to order cancellation events
- **warehouse.exchange.dlq**: Dead letter queue for unprocessable messages

#### Bindings
- `warehouse.delivery.pickup` ← `delivery.exchange` with routing key `delivery.pickup.confirmed`
- `warehouse.order.cancelled` ← `order.exchange` with routing key `order.cancelled`
- `warehouse.exchange.dlq` ← `warehouse.exchange.dlx` with routing key `#`

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
- **Optimistic Locking**: Uses `version` field in inventory table to prevent concurrent modification conflicts
- **Retry Logic**: Automatic retry up to 3 times with exponential backoff (50ms, 100ms, 150ms)
- **Reservation States**: RESERVED → CONFIRMED → (ready for pickup)
- **Automatic Cleanup**: Reserved stock is automatically released when orders are cancelled via MQ events

### Message Queue Reliability
- **Message Acknowledgment**: Auto acknowledgment mode configured
- **Retry Mechanism**: Failed messages retry 3 times with exponential backoff (1s → 2s → 4s)
- **Dead Letter Queue (DLQ)**: Unprocessable messages sent to `warehouse.exchange.dlq`
- **Prefetch Limit**: 10 messages per consumer to prevent overwhelming the service
- **Concurrent Consumers**: 3-10 concurrent consumers for parallel processing

### Database Transactions
- **ACID Properties**: All stock operations wrapped in `@Transactional` annotations
- **Rollback on Failure**: Automatic rollback on exceptions
- **Connection Pooling**: MyBatis-Plus default connection pool
- **Constraint Validation**: Database-level CHECK constraints prevent negative stock levels

### Concurrency Control
- **Optimistic Locking**: Prevents lost updates during concurrent stock modifications
- **Unique Constraints**: Prevents duplicate inventory records per warehouse-product combination
- **Version Increment**: Every stock update increments the version field

## Integration with Other Services

### Communication Patterns

The Warehouse Service uses a hybrid communication pattern:
- **Synchronous (HTTP REST)**: For immediate responses (stock checking, reservation, confirmation)
- **Asynchronous (RabbitMQ)**: For event notifications and eventual consistency

### Store Service Integration

**Store → Warehouse (HTTP)**:
1. `POST /api/warehouse/check-availability` - Check if stock is available
2. `POST /api/warehouse/reserve` - Reserve stock for an order
3. `POST /api/warehouse/confirm` - Confirm reservation after payment success
4. `POST /api/warehouse/release` - Release stock if payment fails (alternative to MQ)

**Warehouse → Store (RabbitMQ)**:
- Publishes `warehouse.stock.reserved` when stock is successfully reserved
- Publishes `warehouse.stock.confirmed` when reservation is confirmed
- Publishes `warehouse.stock.released` when stock is released

**Store → Warehouse (RabbitMQ)**:
- Store publishes `order.cancelled` events that warehouse listens to
- Warehouse automatically releases reserved stock upon receiving cancellation

### Delivery Service Integration

**DeliveryCo → Warehouse (RabbitMQ)**:
- DeliveryCo publishes `delivery.pickup.confirmed` when goods are picked up
- Warehouse logs the pickup event for tracking purposes

**Warehouse → DeliveryCo (via Store)**:
- Warehouse publishes `warehouse.stock.confirmed`
- Store receives this and requests delivery from DeliveryCo with warehouse locations

### Workflow Example

```
1. Customer places order
   └─> Store checks availability: POST /api/warehouse/check-availability

2. Store reserves stock: POST /api/warehouse/reserve
   └─> Warehouse publishes: warehouse.stock.reserved

3. Store processes payment (Bank service)

4. If payment succeeds:
   └─> Store confirms: POST /api/warehouse/confirm
       └─> Warehouse publishes: warehouse.stock.confirmed
       └─> Store requests delivery from DeliveryCo

5. DeliveryCo picks up goods
   └─> DeliveryCo publishes: delivery.pickup.confirmed
       └─> Warehouse logs pickup event

6. If order cancelled (before delivery):
   └─> Store publishes: order.cancelled
       └─> Warehouse releases stock automatically
       └─> Warehouse publishes: warehouse.stock.released
```
