# 🏥 Backend Feature Guide: Geolocation-based Hospital Search

This guide outlines the requirements and implementation details for the "Nearby Hospitals" feature in the Healthcare Chatbot service.

## 1. Feature Overview

The frontend will send the user's current latitude and longitude. The backend needs to return a list of hospitals within a certain radius (e.g., 2km), sorted by distance.

## 2. API Specification

**Endpoint:** `GET /api/healthcare/hospitals`

**Request Parameters:**
| Parameter | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `lat` | Double | Yes | User's current latitude (e.g., 37.5665) |
| `lng` | Double | Yes | User's current longitude (e.g., 126.9780) |
| `radius` | Integer | No | Search radius in meters (Default: 2000) |

**Response Format (JSON):**

```json
[
  {
    "id": "1",
    "name": "Happy Animal Hospital",
    "address": "123 Gangnam-daero, Seoul",
    "lat": 37.567,
    "lng": 126.979,
    "rating": 4.5,
    "distance": 350, // Distance in meters from user
    "status": "OPEN" // "OPEN" or "CLOSED"
  }
  // ... more hospitals
]
```

## 3. Database Implementation Strategies

### Option A: PostGIS (Recommended for PostgreSQL)

If you are using PostgreSQL, enabling the **PostGIS** extension is the most robust solution.

**Query Example:**

```sql
SELECT
    id, name, address, lat, lng, rating, status,
    ST_DistanceSphere(geom, ST_MakePoint(:userLng, :userLat)) as distance
FROM hospitals
WHERE ST_DWithin(geom, ST_MakePoint(:userLng, :userLat), :radius)
ORDER BY distance ASC
LIMIT 20;
```

### Option B: Haversine Formula (No Spatial DB)

If you cannot use spatial extensions, you can use the **Haversine formula** logic directly in code or SQL (though SQL is faster).

**Native SQL Query Example (MySQL/PostgreSQL):**

```sql
SELECT
    id, name, address, lat, lng, rating, status,
    (6371 * acos(cos(radians(:userLat)) * cos(radians(lat)) * cos(radians(lng) - radians(:userLng)) + sin(radians(:userLat)) * sin(radians(lat)))) AS distance
FROM hospitals
HAVING distance < (:radius / 1000) -- radius in km
ORDER BY distance ASC
LIMIT 20;
```

### Option C: Java logic (Using Geolocation Library)

If database capabilities are limited, fetch a rough bounding box from DB then filter in Java.

```java
public List<Hospital> findNearby(double lat, double lng, double radiusKm) {
    // 1. Calculate rough bounding box (minLat, maxLat, minLng, maxLng)
    // 2. Repository.findByLatBetweenAndLngBetween(...)
    // 3. Filter list precisely using Haversine formula in Java to sort by exact distance
}
```

## 4. Entity Update Recommendation

Ensure your `Hospital` entity works well with the chosen strategy.

```java
@Entity
public class Hospital {
    @Id
    private Long id;

    private String name;
    private Double latitude;
    private Double longitude;

    // Indexing latitude and longitude is crucial for performance!
    // @Index(name = "idx_lat_lng", columnList = "latitude, longitude")
}
```
