# US-10 Order Processing — E2E Test Results

**Date:** 2026-03-08  
**Branch:** main  
**Build:** `./mvnw test` — 32/32 tests pass, BUILD SUCCESS

---

## Automated Unit & Integration Tests

| Test Class | Tests | Result |
|---|---|---|
| `DemoSddNoworkflowSpringApplicationTests` | 1 | PASS |
| `OrderServiceTest` | 11 | PASS |
| `OrderRepositoryTest` | 5 | PASS |
| `OrderApiControllerTest` | 8 | PASS |
| `OrderWebControllerTest` | 7 | PASS |
| **Total** | **32** | **PASS** |

```
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## E2E Browser Tests (Manual via Chrome)

### Test 1 — Application loads and displays seeded orders

**Steps:**
1. Start the application (`./mvnw spring-boot:run`)
2. Navigate to `http://localhost:8080/orders`

**Expected:** Order list shows 3 seeded orders (Alice/SHIPPED, Bob/CUSTOMER_NOTIFIED, Carol/SUBMITTED)  
**Result:** PASS

---

### Test 2 — Create a new order (happy path setup)

**Steps:**
1. Click "New Order"
2. Fill in: Customer Name = "Dave Testuser", Product = "Laptop Stand", Quantity = 2
3. Click "Create Order"

**Expected:** Order #4 created with status SUBMITTED, redirected to detail page  
**Result:** PASS

---

### Test 3 — Happy path: SUBMITTED → CHECKING_INVENTORY → PAYMENT_COLLECTED → SHIPPED

**Steps (Order #4):**
1. On detail page, click "Check Inventory" → status becomes CHECKING_INVENTORY
2. Click "Collect Payment" → status becomes PAYMENT_COLLECTED
3. Click "Ship Order" → status becomes SHIPPED

**Expected:** Each transition succeeds; audit trail entries added; progress stepper reflects current state; final status = SHIPPED  
**Result:** PASS

---

### Test 4 — Create a new order (alternative path setup)

**Steps:**
1. Click "New Order"
2. Fill in: Customer Name = "Eve Unavailable", Product = "Rare Item", Quantity = 1
3. Click "Create Order"

**Expected:** Order #5 created with status SUBMITTED  
**Result:** PASS

---

### Test 5 — Alternative path: SUBMITTED → CHECKING_INVENTORY → CUSTOMER_NOTIFIED

**Steps (Order #5):**
1. On detail page, click "Check Inventory" → status becomes CHECKING_INVENTORY
2. Click "Notify Customer (Unavailable)" → status becomes CUSTOMER_NOTIFIED

**Expected:** Transition to CUSTOMER_NOTIFIED succeeds; "Ship" button no longer shown; progress stepper shows skipped step  
**Result:** PASS

---

### Test 6 — REST API via Swagger UI

**Steps:**
1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Expand `GET /api/orders`
3. Click "Try it out" → "Execute"

**Expected:** HTTP 200, JSON array of 5 orders including full `auditTrail` entries  
**Result:** PASS

---

## Known Issues / Notes

- `@MockBean` annotation (Spring Boot 2.x) was replaced with `@MockitoBean` (Spring Boot 3.x / Spring Framework 6.2+) in `OrderApiControllerTest` and `OrderWebControllerTest`.
- Thymeleaf nested `${}` syntax in `detail.html` line 157 was fixed: consolidated into a single `${}` wrapping the full ternary expression for the progress stepper `th:class` attribute.
