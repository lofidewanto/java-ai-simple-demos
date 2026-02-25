# 07 — Testing Strategy

## Purpose

This document describes the test pyramid, all test classes, tools and annotations used, mocking strategy, and the expected coverage for the Guest Book application.

---

## Test Pyramid

```
           ┌──────────────────────┐
           │   Integration Tests  │  1 test
           │  (@SpringBootTest)   │  Full Spring context
           └──────────────────────┘
          ┌────────────────────────────┐
          │    Controller Slice Tests  │  17 tests
          │  (@WebMvcTest — 2 classes) │  Web layer only
          └────────────────────────────┘
         ┌──────────────────────────────────┐
         │       Repository Slice Tests     │  6 tests
         │         (@DataJpaTest)           │  JPA layer only
         └──────────────────────────────────┘
        ┌────────────────────────────────────────┐
        │           Unit Tests                   │  8 tests
        │  (@ExtendWith(MockitoExtension.class))  │  No Spring context
        └────────────────────────────────────────┘
```

**Total: 32 tests across 5 test classes.**

---

## Test Tools

| Tool | Version | Purpose |
|---|---|---|
| JUnit 5 (Jupiter) | Boot-managed | Test runner, assertions (`assertEquals`, `assertTrue`, etc.) |
| Mockito | Boot-managed | Mocking dependencies in unit tests |
| MockMvc | Boot-managed | Simulating HTTP requests in controller slice tests |
| Spring Boot Test | Boot-managed | `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest` slices |
| H2 (in-memory) | Boot-managed | Embedded DB for repository and integration tests |
| Jackson `ObjectMapper` | Boot-managed | Serialising/deserialising JSON in API controller tests |

All are provided by `spring-boot-starter-test` — no additional test dependencies are required.

---

## Test Classes

### 1. `DemoSddSpringApplicationTests` — Integration Test

**File:** `src/test/java/com/example/demo_sdd_spring/DemoSddSpringApplicationTests.java`  
**Annotation:** `@SpringBootTest`  
**Tests:** 1

| Test | Description |
|---|---|
| `contextLoads()` | Starts the full Spring application context (including Flyway migrations, data init, all beans). Passes if no exception is thrown. |

**Purpose:** Smoke test — verifies the entire wiring is correct and the application can start.

---

### 2. `GuestBookServiceTest` — Unit Tests

**File:** `src/test/java/com/example/demo_sdd_spring/service/GuestBookServiceTest.java`  
**Annotation:** `@ExtendWith(MockitoExtension.class)`  
**Tests:** 8

**Setup:**
```java
@Mock
GuestBookEntryRepository guestBookEntryRepository;

@InjectMocks
GuestBookService guestBookService;
```

No Spring context is loaded. Mockito creates the mock and injects it via constructor.

| Test | Method Under Test | Scenario | Verification |
|---|---|---|---|
| `getAllEntries_ShouldReturnAllEntries` | `getAllEntries()` | Repository returns 2 entries | `findAllByOrderByCreatedAtDesc()` called once; list size = 2 |
| `getEntryById_WhenExists_ShouldReturnEntry` | `getEntryById(Long)` | Repository returns `Optional.of(entry)` | Optional is present; entry matches |
| `getEntryById_WhenNotExists_ShouldReturnEmpty` | `getEntryById(Long)` | Repository returns `Optional.empty()` | Optional is empty |
| `createEntry_ShouldSaveAndReturnEntry` | `createEntry(entry)` | `repository.save()` returns the entry | Returned entry is identical; `save()` called once |
| `updateEntry_WhenExists_ShouldUpdateAndReturn` | `updateEntry(id, entry)` | `findById` returns existing entry | Fields updated; `save()` called with updated entry |
| `updateEntry_WhenNotExists_ShouldThrowException` | `updateEntry(id, entry)` | `findById` returns empty Optional | `RuntimeException` thrown |
| `deleteEntry_WhenExists_ShouldDelete` | `deleteEntry(id)` | `existsById` returns `true` | `deleteById(id)` called once |
| `deleteEntry_WhenNotExists_ShouldThrowException` | `deleteEntry(id)` | `existsById` returns `false` | `RuntimeException` thrown |

---

### 3. `GuestBookEntryRepositoryTest` — Repository Slice Tests

**File:** `src/test/java/com/example/demo_sdd_spring/repository/GuestBookEntryRepositoryTest.java`  
**Annotation:** `@DataJpaTest`  
**Tests:** 6

`@DataJpaTest` configures:
- An embedded H2 database (auto-configured, separate from the main app's H2 instance).
- Flyway or `ddl-auto=create-drop` for schema (Spring Boot auto-configures for test slice).
- Only JPA-related beans — no web layer, no services.
- Each test runs in a transaction that is rolled back after the test.

| Test | Scenario | Verification |
|---|---|---|
| `saveEntry_ShouldPersistEntry` | Save a new entry | ID is assigned; `createdAt` and `updatedAt` are not null |
| `findById_WhenExists_ShouldReturnEntry` | Save then find by ID | Optional is present; fields match |
| `findById_WhenNotExists_ShouldReturnEmpty` | Find by non-existent ID | Optional is empty |
| `findAllByOrderByCreatedAtDesc_ShouldReturnEntriesInDescendingOrder` | Save 2 entries with a `Thread.sleep(100)` between them | List has 2 entries; first entry has later `createdAt` than second |
| `deleteById_ShouldRemoveEntry` | Save then delete | `findById` returns empty after deletion |
| `updateEntry_ShouldModifyExistingEntry` | Save, modify fields, save again | Retrieved entry reflects updated field values; `updatedAt` is refreshed |

> **Note on `Thread.sleep(100)`:** Used in the ordering test to guarantee two distinct `createdAt` timestamps, since `LocalDateTime.now()` resolution may not distinguish two rapid consecutive inserts on all JVMs.

---

### 4. `GuestBookApiControllerTest` — API Controller Slice Tests

**File:** `src/test/java/com/example/demo_sdd_spring/controller/api/GuestBookApiControllerTest.java`  
**Annotation:** `@WebMvcTest(GuestBookApiController.class)`  
**Tests:** 8

`@WebMvcTest` configures:
- Only the web layer (controllers, filters, `@ControllerAdvice`).
- `MockMvc` auto-wired for HTTP simulation.
- No real service or repository — `GuestBookService` is replaced by a `@MockBean`.

**Setup:**
```java
@Autowired MockMvc mockMvc;
@Autowired ObjectMapper objectMapper;
@MockBean  GuestBookService guestBookService;
```

| Test | HTTP | Path | Mock Setup | Expected Status | Verification |
|---|---|---|---|---|---|
| `getAllEntries_ShouldReturnListOfEntries` | GET | `/api/entries` | service returns 2-entry list | 200 | JSON array length = 2; first entry's name correct |
| `getEntryById_WhenExists_ShouldReturnEntry` | GET | `/api/entries/1` | service returns `Optional.of(entry)` | 200 | JSON `id`, `name`, `message` match |
| `getEntryById_WhenNotExists_ShouldReturn404` | GET | `/api/entries/999` | service returns `Optional.empty()` | 404 | Empty body |
| `createEntry_ShouldReturnCreatedEntry` | POST | `/api/entries` | service returns saved entry | 201 | JSON body matches; `Content-Type: application/json` |
| `updateEntry_WhenExists_ShouldReturnUpdatedEntry` | PUT | `/api/entries/1` | service returns updated entry | 200 | JSON body matches updated values |
| `updateEntry_WhenNotExists_ShouldReturn404` | PUT | `/api/entries/999` | service throws `RuntimeException` | 404 | Empty body |
| `deleteEntry_WhenExists_ShouldReturn204` | DELETE | `/api/entries/1` | service returns normally | 204 | No body |
| `deleteEntry_WhenNotExists_ShouldReturn404` | DELETE | `/api/entries/999` | service throws `RuntimeException` | 404 | Empty body |

---

### 5. `GuestBookWebControllerTest` — Web Controller Slice Tests

**File:** `src/test/java/com/example/demo_sdd_spring/controller/web/GuestBookWebControllerTest.java`  
**Annotation:** `@WebMvcTest(GuestBookWebController.class)`  
**Tests:** 9

Same slice setup as the API controller test, but targeting the Thymeleaf controller. MockMvc is used to simulate browser form submissions.

| Test | HTTP | Path | Mock Setup | Expected Status | Verification |
|---|---|---|---|---|---|
| `redirectToEntries_ShouldRedirectToEntriesPage` | GET | `/` | — | 3xx | Redirect location = `/entries` |
| `listEntries_ShouldShowEntriesList` | GET | `/entries` | service returns list | 200 | View name = `entries/list`; model contains `entries` attribute |
| `showAddForm_ShouldShowForm` | GET | `/entries/new` | — | 200 | View = `entries/form`; model `isEdit = false` |
| `createEntry_ShouldRedirectToList` | POST | `/entries` | service returns new entry | 3xx | Redirect to `/entries` |
| `showEditForm_WhenExists_ShouldShowForm` | GET | `/entries/1/edit` | service returns `Optional.of(entry)` | 200 | View = `entries/form`; model `isEdit = true` |
| `showEditForm_WhenNotExists_ShouldRedirectWithError` | GET | `/entries/999/edit` | service returns `Optional.empty()` | 3xx | Redirect to `/entries` |
| `updateEntry_WhenExists_ShouldRedirectToList` | POST | `/entries/1` | service returns updated entry | 3xx | Redirect to `/entries` |
| `deleteEntry_WhenExists_ShouldRedirectToList` | POST | `/entries/1/delete` | service returns normally | 3xx | Redirect to `/entries` |
| `deleteEntry_WhenNotExists_ShouldRedirectWithError` | POST | `/entries/999/delete` | service throws `RuntimeException` | 3xx | Redirect to `/entries` |

---

## Mocking Strategy Summary

| Test Class | Spring Context | Service Mock | Repository Mock |
|---|---|---|---|
| `DemoSddSpringApplicationTests` | Full (`@SpringBootTest`) | Real bean | Real bean |
| `GuestBookServiceTest` | None (pure unit) | `@InjectMocks` (real) | `@Mock` (Mockito) |
| `GuestBookEntryRepositoryTest` | JPA slice (`@DataJpaTest`) | Not loaded | Real bean (H2) |
| `GuestBookApiControllerTest` | Web slice (`@WebMvcTest`) | `@MockBean` (Mockito) | Not loaded |
| `GuestBookWebControllerTest` | Web slice (`@WebMvcTest`) | `@MockBean` (Mockito) | Not loaded |

---

## Running Tests

### Run all tests
```bash
./mvnw test
```

### Run a specific test class
```bash
./mvnw test -Dtest=GuestBookServiceTest
./mvnw test -Dtest=GuestBookApiControllerTest
./mvnw test -Dtest=GuestBookEntryRepositoryTest
```

### Run with verbose output
```bash
./mvnw test -Dsurefire.useFile=false
```

---

## Coverage Goals

| Layer | Approach | Target |
|---|---|---|
| Service | Pure unit tests with mocked repository | All public methods, all branches (found / not found) |
| Repository | JPA slice tests with real H2 | All CRUD operations + custom query method |
| API Controller | Web slice with MockMvc | All 5 endpoints × success + not-found paths |
| Web Controller | Web slice with MockMvc | All 7 routes × primary paths |
| Application startup | Full integration test | Context loads without error |

> **Extension:** Add `jacoco-maven-plugin` to `pom.xml` to generate HTML coverage reports with `./mvnw verify`.
