# 🚀 Large-Scale Data Testing Guide

This guide demonstrates how to generate and test with large-scale datasets in the Car Parking Management System.

## 📊 Large-Scale Data Generation

### Overview
The system can generate comprehensive test datasets with:
- **1000+** parking spots across multiple facilities
- **10,000+** parking sessions with realistic historical data
- **500+** users with vehicles
- **25+** parking facilities (garages and street zones)
- **Full payment history** with realistic transaction patterns

## 🛠️ Prerequisites

1. **Start the application:**
```bash
./mvnw spring-boot:run
```

2. **Verify MySQL has sufficient resources:**
```sql
-- Check MySQL configuration
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
SHOW VARIABLES LIKE 'max_connections';
```

## 📋 Step-by-Step Large-Scale Testing

### Step 1: Check Current Data Status

```bash
# Get current database statistics
curl -X GET http://localhost:8080/api/v1/data/stats
```

Expected response:
```json
{
  "users": 0,
  "vehicles": 0,
  "facilities": 0,
  "parkingSpots": 0,
  "parkingSessions": 0,
  "payments": 0,
  "totalCapacity": 0,
  "totalAvailable": 0,
  "occupancyRate": "0.0%"
}
```

### Step 2: Generate Large-Scale Dataset

⚠️ **Warning**: This process may take 2-5 minutes depending on your system resources.

```bash
# Generate large-scale test data
curl -X POST http://localhost:8080/api/v1/data/seed/large \
  -H "Content-Type: application/json"
```

Expected response:
```json
{
  "success": true,
  "message": "Large-scale dataset seeded successfully",
  "executionTimeMs": 185439,
  "data": {
    "users": 500,
    "vehicles": 1200,
    "facilities": 25,
    "parkingSpots": 1087,
    "parkingSessions": 10000,
    "payments": 8532,
    "totalCapacity": 1087,
    "totalAvailable": 756,
    "occupancyRate": "30.4%"
  }
}
```

### Step 3: Verify Large Dataset

```bash
# Check final statistics
curl -X GET http://localhost:8080/api/v1/data/stats
```

## 🧪 Performance Testing with Large Dataset

### Test 1: Facility Query Performance

```bash
# Test facility listing with large dataset
time curl -X GET http://localhost:8080/api/v1/facilities
```

### Test 2: Spot Availability Performance

```bash
# Test available spots query
time curl -X GET http://localhost:8080/api/v1/spots/facility/1/available
```

### Test 3: Cache Performance with Large Dataset

```bash
# Test cache performance with 1000+ spots
curl -X GET http://localhost:8080/api/v1/cache/performance-test
```

Expected response should still show sub-50ms performance:
```json
{
  "success": true,
  "message": "Cache performance test completed",
  "results": {
    "averageResponseTime": "15ms",
    "cacheHitRate": "94.8%",
    "totalRequests": 1000,
    "cacheHits": 948,
    "cacheMisses": 52,
    "performanceStatus": "EXCELLENT"
  }
}
```

### Test 4: Authentication Performance

```bash
# Register user in large dataset environment
time curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Load",
    "lastName": "Test",
    "email": "loadtest@example.com",
    "phoneNumber": "+255700999999",
    "password": "TestPassword123",
    "role": "USER"
  }'
```

## 📈 Performance Benchmarks

### Expected Performance with Large Dataset

| Operation | Target | With 1K+ Spots | With 10K+ Sessions |
|-----------|--------|-----------------|-------------------|
| **Facility List** | <100ms | ~45ms | ~45ms |
| **Spot Availability** | <50ms | ~12ms | ~12ms |
| **User Registration** | <200ms | ~85ms | ~85ms |
| **Cache Operations** | <15ms | ~8ms | ~8ms |
| **Database Queries** | <100ms | ~35ms | ~40ms |

## 🎯 Load Testing Script

Save this as `load_test.sh`:

```bash
#!/bin/bash

echo "🚀 Starting Load Test with Large Dataset"
echo "========================================"

BASE_URL="http://localhost:8080/api/v1"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Test 1: Generate Large Dataset
echo -e "${YELLOW}1. Generating large dataset...${NC}"
start_time=$(date +%s)
LARGE_SEED_RESPONSE=$(curl -s -X POST ${BASE_URL}/data/seed/large)
end_time=$(date +%s)
generation_time=$((end_time - start_time))

if [[ $LARGE_SEED_RESPONSE == *"success\":true"* ]]; then
    echo -e "${GREEN}✅ Large dataset generated in ${generation_time}s${NC}"
else
    echo -e "${RED}❌ Failed to generate large dataset${NC}"
    exit 1
fi

# Test 2: Performance Testing
echo -e "${YELLOW}2. Testing performance with large dataset...${NC}"

# Cache performance
CACHE_RESPONSE=$(curl -s -X GET ${BASE_URL}/cache/performance-test)
if [[ $CACHE_RESPONSE == *"ms"* ]]; then
    echo -e "${GREEN}✅ Cache performance test passed${NC}"
else
    echo -e "${RED}❌ Cache performance test failed${NC}"
fi

# Facility listing performance
start_time=$(date +%s%3N)
FACILITIES=$(curl -s -X GET ${BASE_URL}/facilities)
end_time=$(date +%s%3N)
facility_time=$((end_time - start_time))

if [ $facility_time -lt 100 ]; then
    echo -e "${GREEN}✅ Facility listing: ${facility_time}ms (< 100ms target)${NC}"
else
    echo -e "${RED}❌ Facility listing: ${facility_time}ms (> 100ms target)${NC}"
fi

# Test 3: Concurrent User Registration
echo -e "${YELLOW}3. Testing concurrent user registrations...${NC}"

for i in {1..10}; do
    curl -s -X POST ${BASE_URL}/auth/register \
      -H "Content-Type: application/json" \
      -d "{
        \"firstName\": \"User\",
        \"lastName\": \"${i}\",
        \"email\": \"loadtest${i}@example.com\",
        \"phoneNumber\": \"+25570000${i}\",
        \"password\": \"TestPassword123\",
        \"role\": \"USER\"
      }" > /dev/null &
done

wait
echo -e "${GREEN}✅ Concurrent registrations completed${NC}"

# Test 4: Database Statistics
echo -e "${YELLOW}4. Final database statistics:${NC}"
STATS=$(curl -s -X GET ${BASE_URL}/data/stats)
echo "$STATS" | python3 -m json.tool

echo -e "${GREEN}🎉 Load testing completed!${NC}"
```

Run the load test:
```bash
chmod +x load_test.sh
./load_test.sh
```

## 📊 Dataset Composition

### Generated Data Breakdown

**Users (500 total):**
- 5 Admin users
- 20 Parking attendants  
- 475 Regular users

**Vehicles (1200 total):**
- Realistic Tanzanian license plates (T123ABC format)
- Mix of car types: 80% cars, 15% vans, 5% motorcycles
- 20+ different makes and models
- 95% active vehicles

**Facilities (25+ total):**
- Mix of garages (70%) and street zones (30%)
- Realistic Dar es Salaam locations
- Varied pricing: 1000-5000 TZS per hour
- Different operating hours

**Parking Spots (1000+ total):**
- Realistic spot numbering (L0-001, L1-015, S001, etc.)
- Spot type distribution:
  - 5% Disabled spots
  - 15% Electric charging spots
  - 35% Compact spots
  - 45% Regular spots
- Status distribution:
  - 70% Available
  - 15% Occupied  
  - 10% Reserved
  - 5% Under maintenance

**Sessions (10,000 total):**
- 75% Completed sessions
- 15% Active sessions
- 5% Expired sessions
- 5% Cancelled sessions
- Realistic duration: 1-8 hours
- Historical data spanning 90 days

**Payments (8,500+ total):**
- 90% Completed payments
- 5% Pending payments
- 5% Failed payments
- Mix of card and mobile money payments
- X-PAYMENT-PROVIDER integration

## 🔄 Data Management Operations

### Clear All Data (Use with Caution)

```bash
# WARNING: This will delete ALL data
curl -X POST http://localhost:8080/api/v1/data/clear
```

### Regenerate Dataset

```bash
# Clear and regenerate
curl -X POST http://localhost:8080/api/v1/data/clear
curl -X POST http://localhost:8080/api/v1/data/seed/large
```

## 🎯 Use Cases for Large Dataset

1. **Performance Testing**: Verify sub-50ms response times
2. **Load Testing**: Test with realistic data volumes
3. **UI Testing**: Test pagination and filtering
4. **Analytics Testing**: Test reporting with substantial data
5. **Caching Verification**: Ensure cache effectiveness
6. **Database Optimization**: Identify slow queries
7. **Memory Usage**: Monitor application memory with large datasets

## 📝 Monitoring During Large-Scale Testing

### Database Performance
```sql
-- Monitor active connections
SHOW PROCESSLIST;

-- Check table sizes
SELECT 
    table_name,
    table_rows,
    ROUND(((data_length + index_length) / 1024 / 1024), 2) AS size_mb
FROM information_schema.TABLES 
WHERE table_schema = 'parking_management'
ORDER BY size_mb DESC;
```

### Application Metrics
```bash
# Memory usage
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# HTTP request metrics
curl http://localhost:8080/actuator/metrics/http.server.requests

# Database connection pool
curl http://localhost:8080/actuator/metrics/hikaricp.connections.active
```

This large-scale testing capability ensures your Car Parking Management System can handle enterprise-level data volumes while maintaining optimal performance! 🚀