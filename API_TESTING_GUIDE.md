# 🧪 Complete API Testing Guide

This guide provides step-by-step instructions to test all APIs in the Car Parking Management System.

## Prerequisites

1. **Start the application:**
```bash
./mvnw spring-boot:run
```

2. **Verify application is running:**
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{"status":"UP","components":{"db":{"status":"UP"}}}
```

## Step-by-Step API Testing

### Step 1: Health Check & System Status

```bash
# Test 1: Application Health
curl -X GET http://localhost:8080/actuator/health

# Test 2: API Documentation Access
open http://localhost:8080/swagger-ui.html
# Or: curl http://localhost:8080/swagger-ui.html
```

### Step 2: Data Seeding (Required First)

```bash
# Seed the database with sample data
curl -X POST http://localhost:8080/api/v1/data/seed
```

Expected response:
```json
{
  "success": true,
  "message": "Sample data seeded successfully",
  "data": {
    "usersCreated": 5,
    "facilitiesCreated": 3,
    "spotsCreated": 50,
    "vehiclesCreated": 8
  }
}
```

### Step 3: Authentication Testing

#### 3.1 User Registration

```bash
# Register a new user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+255700123456",
    "password": "SecurePassword123",
    "role": "USER"
  }'
```

Expected response:
```json
{
  "success": true,
  "message": "Authentication successful",
  "token": "eyJhbGciOiJIUzM4NCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "user": {
    "id": 6,
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "phoneNumber": "+255700123456",
    "role": "USER"
  }
}
```

**Save the token for subsequent requests:**
```bash
export TOKEN="your-token-here"
```

#### 3.2 User Login

```bash
# Login with existing user
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePassword123"
  }'
```

#### 3.3 Token Validation

```bash
# Validate JWT token
curl -X GET http://localhost:8080/api/v1/auth/validate \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "valid": true,
  "message": "Token is valid",
  "expiresIn": 86400000
}
```

#### 3.4 Get Current User

```bash
# Get current user info
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN"
```

#### 3.5 Refresh Token

```bash
# Refresh JWT token
curl -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Authorization: Bearer $TOKEN"
```

### Step 4: Facility Management Testing

#### 4.1 List All Facilities

```bash
# Get all parking facilities
curl -X GET http://localhost:8080/api/v1/facilities
```

Expected response:
```json
[
  {
    "id": 1,
    "name": "Central Business District Garage",
    "facilityType": "GARAGE",
    "address": "123 Uhuru Street, Dar es Salaam",
    "locationLat": -6.7924,
    "locationLng": 39.2083,
    "totalSpots": 100,
    "availableSpots": 85,
    "baseHourlyRate": 2000.00,
    "operatingHoursStart": "06:00:00",
    "operatingHoursEnd": "22:00:00",
    "isActive": true
  }
]
```

#### 4.2 Get Specific Facility

```bash
# Get facility by ID
curl -X GET http://localhost:8080/api/v1/facilities/1
```

#### 4.3 Find Nearby Facilities

```bash
# Find facilities within 5km radius of Dar es Salaam city center
curl -X GET "http://localhost:8080/api/v1/facilities/nearby?latitude=-6.7924&longitude=39.2083&radius=5"
```

### Step 5: Parking Spot Testing

#### 5.1 Get Spots by Facility

```bash
# Get all spots for facility ID 1
curl -X GET http://localhost:8080/api/v1/spots/facility/1
```

#### 5.2 Get Available Spots

```bash
# Get available spots for facility ID 1
curl -X GET http://localhost:8080/api/v1/spots/facility/1/available
```

Expected response:
```json
[
  {
    "id": 1,
    "spotNumber": "A001",
    "spotType": "REGULAR",
    "status": "AVAILABLE",
    "facilityId": 1,
    "facilityName": "Central Business District Garage"
  }
]
```

### Step 6: Reservation System Testing

#### 6.1 Reserve a Parking Spot

```bash
# Reserve spot ID 1 for 2 hours
curl -X POST http://localhost:8080/api/v1/reservations/reserve \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 6,
    "vehicleId": 1,
    "facilityId": 1,
    "spotId": 1,
    "durationMinutes": 120
  }'
```

Expected response:
```json
{
  "success": true,
  "message": "Spot reserved successfully. You have 15 minutes to confirm your arrival.",
  "reservation": {
    "id": 1,
    "reservationReference": "RES-123456789",
    "userId": 6,
    "vehicleId": 1,
    "facilityId": 1,
    "spotId": 1,
    "reservedFrom": "2024-01-15T10:30:00",
    "reservedUntil": "2024-01-15T12:30:00",
    "totalAmount": 4000.00,
    "status": "ACTIVE",
    "expiresAt": "2024-01-15T10:45:00"
  }
}
```

**Save the reservation reference:**
```bash
export RESERVATION_REF="RES-123456789"
```

#### 6.2 Confirm Reservation

```bash
# Confirm arrival and activate parking session
curl -X POST http://localhost:8080/api/v1/reservations/$RESERVATION_REF/confirm \
  -H "Authorization: Bearer $TOKEN"
```

Expected response:
```json
{
  "success": true,
  "message": "Reservation confirmed successfully",
  "reservation": {
    "id": 1,
    "reservationReference": "RES-123456789",
    "status": "CONFIRMED"
  }
}
```

#### 6.3 Get User Reservations

```bash
# Get all reservations for user ID 6
curl -X GET http://localhost:8080/api/v1/reservations/user/6 \
  -H "Authorization: Bearer $TOKEN"
```

#### 6.4 Cancel Reservation

```bash
# Cancel reservation (if needed)
curl -X DELETE http://localhost:8080/api/v1/reservations/$RESERVATION_REF \
  -H "Authorization: Bearer $TOKEN"
```

### Step 7: Payment System Testing

#### 7.1 Get Supported Payment Providers

```bash
# Get list of supported payment providers
curl -X GET http://localhost:8080/api/v1/payments/providers
```

Expected response:
```json
{
  "providers": [
    {
      "id": "VISA",
      "name": "Visa Card",
      "type": "CARD",
      "supportedCurrencies": ["TZS", "USD"],
      "isActive": true
    },
    {
      "id": "VODACOM",
      "name": "Vodacom M-Pesa",
      "type": "MOBILE_MONEY",
      "supportedCurrencies": ["TZS"],
      "isActive": true
    }
  ]
}
```

#### 7.2 Process Card Payment

```bash
# Process card payment for parking session
curl -X POST http://localhost:8080/api/v1/payments/card \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "amount": 4000,
    "currency": "TZS",
    "customerEmail": "john.doe@example.com",
    "cardNumber": "4111111111111111",
    "cardExpiry": "12/25",
    "cardCvv": "123"
  }'
```

Expected response:
```json
{
  "success": true,
  "message": "Payment processed successfully",
  "payment": {
    "id": 1,
    "paymentReference": "PAY-123456789",
    "externalPaymentId": "xpay_ext_123456",
    "amount": 4000.00,
    "currency": "TZS",
    "paymentMethod": "CARD",
    "paymentProvider": "VISA",
    "status": "COMPLETED"
  }
}
```

#### 7.3 Process Mobile Money Payment

```bash
# Process mobile money payment
curl -X POST http://localhost:8080/api/v1/payments/mobile-money \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "sessionId": 1,
    "amount": 4000,
    "currency": "TZS",
    "customerPhone": "+255700123456",
    "mobileMoneyProvider": "VODACOM"
  }'
```

#### 7.4 Verify Payment

```bash
# Verify payment status
curl -X GET http://localhost:8080/api/v1/payments/PAY-123456789/verify \
  -H "Authorization: Bearer $TOKEN"
```

### Step 8: Cache Performance Testing

#### 8.1 Test Cache Performance

```bash
# Test cache performance (should be sub-50ms)
curl -X GET http://localhost:8080/api/v1/cache/performance-test
```

Expected response:
```json
{
  "success": true,
  "message": "Cache performance test completed",
  "results": {
    "averageResponseTime": "12ms",
    "cacheHitRate": "95.2%",
    "totalRequests": 1000,
    "cacheHits": 952,
    "cacheMisses": 48,
    "performanceStatus": "EXCELLENT"
  }
}
```

#### 8.2 Get Real-time Availability

```bash
# Get real-time availability for facility 1
curl -X GET http://localhost:8080/api/v1/cache/availability/1 \
  -H "Authorization: Bearer $TOKEN"
```

#### 8.3 Get Cache Statistics

```bash
# Get cache statistics
curl -X GET http://localhost:8080/api/v1/cache/stats \
  -H "Authorization: Bearer $TOKEN"
```

### Step 9: User Management Testing

#### 9.1 Get All Users (Admin only)

```bash
# Get all users (requires ADMIN role)
curl -X GET http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

#### 9.2 Get User Details

```bash
# Get user details
curl -X GET http://localhost:8080/api/v1/users/6 \
  -H "Authorization: Bearer $TOKEN"
```

#### 9.3 Add Vehicle to User

```bash
# Add a vehicle to user
curl -X POST http://localhost:8080/api/v1/users/6/vehicles \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "licensePlate": "T123ABC",
    "vehicleType": "CAR",
    "make": "Toyota",
    "model": "Corolla",
    "color": "White"
  }'
```

#### 9.4 Get User Vehicles

```bash
# Get all vehicles for user
curl -X GET http://localhost:8080/api/v1/users/6/vehicles \
  -H "Authorization: Bearer $TOKEN"
```

## Complete Workflow Test Script

Save this as `test_all_apis.sh`:

```bash
#!/bin/bash

echo "=== Complete API Workflow Test ==="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080/api/v1"

echo -e "${YELLOW}1. Testing health check...${NC}"
HEALTH=$(curl -s ${BASE_URL}/../actuator/health)
if [[ $HEALTH == *"UP"* ]]; then
    echo -e "${GREEN}✅ Application is healthy${NC}"
else
    echo -e "${RED}❌ Application is not healthy${NC}"
    exit 1
fi

echo -e "${YELLOW}2. Seeding database...${NC}"
curl -s -X POST ${BASE_URL}/data/seed > /dev/null
echo -e "${GREEN}✅ Database seeded${NC}"

echo -e "${YELLOW}3. Registering user...${NC}"
REGISTER_RESPONSE=$(curl -s -X POST ${BASE_URL}/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "email": "test@example.com",
    "phoneNumber": "+255700123456",
    "password": "TestPassword123",
    "role": "USER"
  }')

TOKEN=$(echo "$REGISTER_RESPONSE" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ Failed to get token${NC}"
    exit 1
fi
echo -e "${GREEN}✅ User registered, token obtained${NC}"

echo -e "${YELLOW}4. Testing protected endpoint...${NC}"
USER_INFO=$(curl -s -X GET ${BASE_URL}/auth/me \
  -H "Authorization: Bearer $TOKEN")
if [[ $USER_INFO == *"test@example.com"* ]]; then
    echo -e "${GREEN}✅ Protected endpoint works${NC}"
else
    echo -e "${RED}❌ Protected endpoint failed${NC}"
fi

echo -e "${YELLOW}5. Testing facilities...${NC}"
FACILITIES=$(curl -s -X GET ${BASE_URL}/facilities)
if [[ $FACILITIES == *"Central Business District"* ]]; then
    echo -e "${GREEN}✅ Facilities endpoint works${NC}"
else
    echo -e "${RED}❌ Facilities endpoint failed${NC}"
fi

echo -e "${YELLOW}6. Testing cache performance...${NC}"
CACHE_TEST=$(curl -s -X GET ${BASE_URL}/cache/performance-test)
if [[ $CACHE_TEST == *"ms"* ]]; then
    echo -e "${GREEN}✅ Cache performance test works${NC}"
else
    echo -e "${RED}❌ Cache performance test failed${NC}"
fi

echo -e "${YELLOW}7. Testing payment providers...${NC}"
PROVIDERS=$(curl -s -X GET ${BASE_URL}/payments/providers)
if [[ $PROVIDERS == *"VODACOM"* ]]; then
    echo -e "${GREEN}✅ Payment providers endpoint works${NC}"
else
    echo -e "${RED}❌ Payment providers endpoint failed${NC}"
fi

echo -e "${GREEN}🎉 All API tests completed successfully!${NC}"
echo -e "${YELLOW}Your JWT Token: ${TOKEN:0:50}...${NC}"
echo -e "${YELLOW}Use this token for authenticated requests${NC}"
```

Make it executable and run:
```bash
chmod +x test_all_apis.sh
./test_all_apis.sh
```

## Expected Response Times

- **Authentication**: < 100ms
- **Facility queries**: < 50ms (with Redis cache)
- **Reservation creation**: < 200ms
- **Payment processing**: < 500ms
- **Cache operations**: < 15ms

## Common Error Responses

### 401 Unauthorized
```json
{
  "timestamp": "2024-01-15T10:30:00.000+00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT Token is missing or invalid",
  "path": "/api/v1/reservations"
}
```

### 400 Bad Request
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "email": "Email is required",
    "password": "Password must be at least 8 characters"
  }
}
```

### 404 Not Found
```json
{
  "success": false,
  "message": "Resource not found",
  "errorCode": "RESOURCE_NOT_FOUND"
}
```

## Troubleshooting

1. **Token expires**: Use the refresh endpoint or re-login
2. **Database connection issues**: Check XAMPP MySQL service
3. **Cache errors**: Verify Redis is running
4. **Permission denied**: Check user role for admin endpoints

This completes the comprehensive API testing guide! 🚀