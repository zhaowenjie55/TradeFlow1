# Search Module

## Purpose
This module provides the current backend search API for Amazon keyword search.
It keeps the integration path explicit:

`Frontend -> Spring Boot -> Python FastAPI crawler -> SerpApi`

The Java backend does not call SerpApi directly. The Python service is the data acquisition layer and returns normalized JSON that Java converts into typed response DTOs.

## Current API
Endpoint:

```http
POST /api/search/amazon
```

Request body:

```json
{
  "keyword": "portable blender",
  "page": 1
}
```

Response body:

```json
{
  "success": true,
  "keyword": "portable blender",
  "page": 1,
  "count": 10,
  "items": [
    {
      "platform": "amazon",
      "externalItemId": "B0XXXXXXX",
      "title": "Portable Blender",
      "price": "$29.99",
      "imageUrl": "https://...",
      "productUrl": "https://...",
      "rating": 4.5,
      "reviewCount": 123
    }
  ]
}
```

## Request/Response Flow
1. `SearchController` accepts `POST /api/search/amazon`.
2. `SearchService` calls `PythonCrawlerClient`.
3. `PythonCrawlerClient` sends `POST` to the FastAPI crawler endpoint.
4. Python calls SerpApi with `engine=amazon` and `k=<keyword>`.
5. Python extracts `organic_results`, normalizes the fields, and returns JSON.
6. `SearchService` maps the normalized JSON into `SearchResponse` and `ProductItem`.

## Involved Classes
- `domain/search/controller/SearchController`
  Exposes the backend API endpoint.
- `domain/search/service/SearchService`
  Orchestrates the search flow and maps JSON to typed DTOs.
- `integration/crawler/PythonCrawlerClient`
  Performs the HTTP call from Java to the Python crawler.
- `domain/search/dto/SearchRequest`
  Request payload with validation for `keyword` and `page`.
- `domain/search/dto/SearchResponse`
  Typed response returned by the backend.
- `domain/search/dto/ProductItem`
  Individual product row in the backend response.

## Mapping Rules
The mapping logic lives in `SearchService`.

- Text fields return `null` when missing or blank.
- `rating` is converted to `Double`.
- `reviewCount` is converted to `Integer`.
- If Python omits `page`, Java falls back to the request page.
- If Python omits `count`, Java falls back to `items.size()`.
- If Python omits `keyword`, Java falls back to the request keyword.
- If `items` is missing or not an array, Java returns an empty list.

This keeps the current module tolerant of partial downstream responses without changing the public API shape.

## Local Run
Start the Python crawler first:

```bash
cd /Users/coolmood/TradeFlow/crawler-service
export SERPAPI_KEY='YOUR_KEY'
uvicorn main:app --reload --port 8001
```

Start the Spring Boot backend:

```bash
cd /Users/coolmood/TradeFlow/backend
mvn spring-boot:run
```

## Local Test
Call the backend API:

```bash
curl -X POST "http://127.0.0.1:8080/api/search/amazon" \
  -H "Content-Type: application/json" \
  -d '{"keyword":"portable blender","page":1}'
```

## Configuration
The crawler endpoint is configured in:

`backend/src/main/resources/application.yml`

Relevant variables:

- `CRAWLER_SERVICE_ENABLED`
  Enables or disables the Java-to-Python crawler integration.
- `CRAWLER_SEARCH_ENDPOINT`
  Overrides the default crawler search endpoint.

Default value:

```text
http://127.0.0.1:8001/api/search
```

Example override:

```bash
export CRAWLER_SEARCH_ENDPOINT='http://127.0.0.1:9001/api/search'
```

## Current Limitations
- Amazon search only.
- Search only; no product detail endpoint yet.
- No caching.
- No persistence.
- No provider fallback.
- No resilience features beyond simple field-level fallbacks.

## Recommended Next Steps
- Add a formal `/api/detail` flow using the same controller-service-client pattern.
- Add integration tests that stub the Python crawler response.
- Add request/response logging around the crawler boundary if operational debugging becomes necessary.
- Add caching only after search semantics and invalidation rules are clear.
