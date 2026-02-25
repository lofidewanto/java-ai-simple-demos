# 03 — REST API Specification

## Purpose

This document specifies the complete REST API contract for the Guest Book application. It covers all endpoints, HTTP methods, request/response shapes, status codes, and error cases.

---

## Base URL

```
http://localhost:8080/api/entries
```

---

## Controller

**Class:** `com.example.demo_sdd_spring.controller.api.GuestBookApiController`  
**Annotations:** `@RestController`, `@RequestMapping("/api/entries")`  
**OpenAPI tag:** `Guest Book` — `"Guest Book Entry Management API"`  
**Dependency:** Constructor-injected `GuestBookService`

---

## OpenAPI / Swagger UI

The API is self-documented via Springdoc OpenAPI 3:

| Resource | URL |
|---|---|
| Interactive Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI 3 JSON spec | `http://localhost:8080/api-docs` |

Configuration in `application.properties`:
```properties
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true
springdoc.swagger-ui.operationsSorter=method
springdoc.swagger-ui.tagsSorter=alpha
```

---

## Endpoints

### GET /api/entries

**Description:** Retrieve all guest book entries, ordered by creation date descending (newest first).

**Request**
- Body: none
- Parameters: none

**Response — 200 OK**
```json
[
  {
    "id": 3,
    "name": "Carol Williams",
    "email": "carol@example.com",
    "message": "Excellent work on this Spring Boot application. Very clean code!",
    "createdAt": "2026-02-14T10:32:00",
    "updatedAt": "2026-02-14T10:32:00"
  },
  {
    "id": 2,
    "name": "Bob Smith",
    "email": "bob@example.com",
    "message": "Hello everyone! Happy to be here and leave my mark.",
    "createdAt": "2026-02-14T10:31:00",
    "updatedAt": "2026-02-14T10:31:00"
  }
]
```

Returns an empty array `[]` when no entries exist.

---

### GET /api/entries/{id}

**Description:** Retrieve a single guest book entry by its ID.

**Path Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Long` | yes | The numeric ID of the entry |

**Response — 200 OK**
```json
{
  "id": 1,
  "name": "Alice Johnson",
  "email": "alice@example.com",
  "message": "What a wonderful guest book! Great job on the implementation.",
  "createdAt": "2026-02-14T10:30:00",
  "updatedAt": "2026-02-14T10:30:00"
}
```

**Response — 404 Not Found**

Empty body. Returned when no entry exists with the given `id`.

---

### POST /api/entries

**Description:** Create a new guest book entry.

**Request Headers**
```
Content-Type: application/json
```

**Request Body**

| Field | Type | Required | Constraints |
|---|---|---|---|
| `name` | `String` | yes | Non-null |
| `email` | `String` | no | May be omitted or `null` |
| `message` | `String` | yes | Non-null |

```json
{
  "name": "Dave Miller",
  "email": "dave@example.com",
  "message": "Just discovered this app. Looks amazing!"
}
```

**Response — 201 Created**

Returns the persisted entry (with `id`, `createdAt`, `updatedAt` populated by the server):

```json
{
  "id": 4,
  "name": "Dave Miller",
  "email": "dave@example.com",
  "message": "Just discovered this app. Looks amazing!",
  "createdAt": "2026-02-14T11:00:00",
  "updatedAt": "2026-02-14T11:00:00"
}
```

**Response — 400 Bad Request**

Returned when the request body cannot be parsed (malformed JSON).

> **Implementation note:** Bean Validation (`@Valid`) is not applied in the current implementation. The `nullable = false` constraint is enforced at the database level; a missing `name` or `message` will result in a 500 from the DB constraint if supplied as `null`.

---

### PUT /api/entries/{id}

**Description:** Fully replace an existing guest book entry. All writable fields (`name`, `email`, `message`) are overwritten with the supplied values.

**Path Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Long` | yes | The numeric ID of the entry to update |

**Request Headers**
```
Content-Type: application/json
```

**Request Body**

Same shape as POST:

```json
{
  "name": "Dave Miller",
  "email": "dave.updated@example.com",
  "message": "Updating my previous entry with a better message."
}
```

**Response — 200 OK**

Returns the updated entry:

```json
{
  "id": 4,
  "name": "Dave Miller",
  "email": "dave.updated@example.com",
  "message": "Updating my previous entry with a better message.",
  "createdAt": "2026-02-14T11:00:00",
  "updatedAt": "2026-02-14T11:05:00"
}
```

**Response — 404 Not Found**

Empty body. Returned when no entry exists with the given `id`.

> **Implementation note:** The service throws `RuntimeException("Entry not found with id: " + id)` when the entry does not exist. The controller catches this and returns `ResponseEntity.notFound().build()`.

---

### DELETE /api/entries/{id}

**Description:** Delete a guest book entry by its ID.

**Path Parameters**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `id` | `Long` | yes | The numeric ID of the entry to delete |

**Response — 204 No Content**

Empty body. Entry was successfully deleted.

**Response — 404 Not Found**

Empty body. Returned when no entry exists with the given `id`.

---

## Error Handling Summary

| Scenario | HTTP Status | Body |
|---|---|---|
| Entry not found (GET by ID) | 404 | empty |
| Entry not found (PUT) | 404 | empty |
| Entry not found (DELETE) | 404 | empty |
| Successful creation | 201 | entry JSON |
| Malformed JSON body | 400 | Spring default error body |
| DB constraint violation (null required field) | 500 | Spring default error body |

Error mapping in the controller for update/delete:

```java
try {
    // service call
} catch (RuntimeException e) {
    return ResponseEntity.notFound().build();
}
```

---

## curl Examples

### List all entries
```bash
curl -X GET http://localhost:8080/api/entries
```

### Get entry by ID
```bash
curl -X GET http://localhost:8080/api/entries/1
```

### Create entry
```bash
curl -X POST http://localhost:8080/api/entries \
  -H "Content-Type: application/json" \
  -d '{"name":"Dave Miller","email":"dave@example.com","message":"Just discovered this app!"}'
```

### Update entry
```bash
curl -X PUT http://localhost:8080/api/entries/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"Alice Johnson","email":"alice@example.com","message":"Updated message."}'
```

### Delete entry
```bash
curl -X DELETE http://localhost:8080/api/entries/1
```

---

## Controller Implementation Skeleton

```java
@RestController
@RequestMapping("/api/entries")
@Tag(name = "Guest Book", description = "Guest Book Entry Management API")
public class GuestBookApiController {

    private final GuestBookService guestBookService;

    public GuestBookApiController(GuestBookService guestBookService) {
        this.guestBookService = guestBookService;
    }

    @GetMapping
    public ResponseEntity<List<GuestBookEntry>> getAllEntries() { ... }

    @GetMapping("/{id}")
    public ResponseEntity<GuestBookEntry> getEntryById(@PathVariable Long id) { ... }

    @PostMapping
    public ResponseEntity<GuestBookEntry> createEntry(@RequestBody GuestBookEntry entry) { ... }

    @PutMapping("/{id}")
    public ResponseEntity<GuestBookEntry> updateEntry(
            @PathVariable Long id, @RequestBody GuestBookEntry entry) { ... }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) { ... }
}
```
