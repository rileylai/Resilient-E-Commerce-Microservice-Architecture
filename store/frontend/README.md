# Store Frontend

A simple Node.js frontend for the Store API.

## Prerequisites

- Node.js installed (v14 or higher)
- Store backend running on http://localhost:8080

## Installation

1. Navigate to the frontend directory:
```bash
cd store/frontend
```

2. Install dependencies:
```bash
npm install
```

## Running the Frontend

Start the frontend server:
```bash
npm start
```

The frontend will be available at http://localhost:3000

## Important: Enable CORS on Backend

The Store backend needs to allow CORS requests from the frontend. Add the following CORS configuration to your Spring Boot application.

Create a new file `store/src/main/java/com/tut2/group3/store/config/CorsConfig.java`:

```java
package com.tut2.group3.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
}
```

Or if using Spring Security, update the SecurityConfig to add CORS configuration.

## Features

- **Login**: Authenticate and receive JWT token (automatically saved)
- **Register**: Create new user account
- **Search User**: Find user by username
- **Secure Test**: Test if JWT token is valid
- **Get All Products**: View all available products from warehouse
- **Place Order**: Create new orders with multiple items
- **Cancel Order**: Cancel existing orders
- **Get Order by ID**: View order details
- **Get Orders by User ID**: View all orders for a user

## Usage

1. First, register a new user or login with existing credentials
2. The JWT token will be automatically saved in localStorage
3. All subsequent API calls will use the saved token
4. Results are displayed below each form (success in green, errors in red)
5. Use "Clear Token (Logout)" button to remove the saved token

## API Endpoints

The frontend connects to the following backend endpoints:

- POST `/user/login` - Login
- POST `/user/register` - Register
- GET `/user/search` - Search user by username
- GET `/user/secure` - Test JWT authentication
- GET `/product/all` - Get all products from warehouse
- POST `/order/place` - Place new order
- POST `/order/cancel/{orderId}` - Cancel order
- GET `/order/{orderId}` - Get order by ID
- GET `/order/user/{userId}` - Get orders by user ID

