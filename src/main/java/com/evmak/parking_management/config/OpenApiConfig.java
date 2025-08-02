package com.evmak.parking_management.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Car Parking Management System API",
        version = "1.0.0",
        description = """
            A comprehensive car parking management system for Tanzania supporting:
            
            ## Features
            - **Real-time parking availability** with Redis caching (sub-50ms response times)
            - **High-performance spot reservation** with concurrency handling
            - **X-PAYMENT-PROVIDER integration** for card and mobile money payments
            - **JWT authentication** and role-based access control
            - **Multi-facility support** (15 garages, 200 street zones, 8,000+ spots)
            - **Dynamic pricing** based on demand and time
            - **Violation management** and automated fine calculation
            - **Comprehensive analytics** and reporting
            
            ## Payment Methods
            - **Card Payments**: VISA, MASTERCARD
            - **Mobile Money**: Vodacom, Airtel Money, Tigo Pesa, Halotel (Tanzania)
            - **Currency**: Tanzanian Shilling (TZS)
            
            ## Performance
            - Handles **100,000+ daily parking sessions**
            - **Sub-50ms response times** for cached data
            - **1000+ concurrent reservations** supported
            - **High availability** with Redis caching and database optimization
            
            ## Authentication
            All protected endpoints require a valid JWT token in the Authorization header:
            `Authorization: Bearer <your-jwt-token>`
            
            ## Getting Started
            1. Register a new user account using `/api/v1/auth/register`
            2. Login to receive a JWT token using `/api/v1/auth/login`
            3. Include the token in the Authorization header for protected endpoints
            4. Explore parking facilities and make reservations
            """,
        contact = @Contact(
            name = "EVMAK Development Team",
            email = "support@evmak.com",
            url = "https://evmak.com"
        ),
        license = @License(
            name = "MIT License",
            url = "https://opensource.org/licenses/MIT"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Development Server"),
        @Server(url = "https://api.parking.evmak.com", description = "Production Server")
    }
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Authentication Token"
)
public class OpenApiConfig {
}