# 06 — Web UI / MVC Flow

## Purpose

This document specifies the Thymeleaf-based web UI layer: all routes, controller-to-template mappings, model attributes, form flows, and the redirect-after-POST pattern used throughout.

---

## Controller

**Class:** `com.example.demo_sdd_spring.controller.web.GuestBookWebController`  
**Annotation:** `@Controller` (no base `@RequestMapping` — routes defined per method)  
**Dependency:** Constructor-injected `GuestBookService`

---

## Templates

Located in `src/main/resources/templates/`:

```
templates/
└── entries/
    ├── list.html    # Displays all guest book entries; includes add/delete/edit links
    └── form.html    # Shared form for both create and edit operations
```

Static assets in `src/main/resources/static/css/style.css`.  
Bootstrap 5 loaded from CDN (no local asset bundling).

---

## Route Map

| Method | Path | Handler | Template / Redirect | Description |
|---|---|---|---|---|
| `GET` | `/` | `redirectToEntries()` | `redirect:/entries` | Root URL redirect |
| `GET` | `/entries` | `listEntries(Model)` | `entries/list` | Show all entries |
| `GET` | `/entries/new` | `showAddForm(Model)` | `entries/form` | Show blank add form |
| `POST` | `/entries` | `createEntry(...)` | `redirect:/entries` | Submit new entry |
| `GET` | `/entries/{id}/edit` | `showEditForm(...)` | `entries/form` | Show pre-filled edit form |
| `POST` | `/entries/{id}` | `updateEntry(...)` | `redirect:/entries` | Submit edited entry |
| `POST` | `/entries/{id}/delete` | `deleteEntry(...)` | `redirect:/entries` | Delete entry |

> **Note on DELETE/PUT via HTML forms:** HTML forms only support `GET` and `POST`. Update is posted to `/entries/{id}` and delete to `/entries/{id}/delete` — no `_method` override (Spring's `HiddenHttpMethodFilter`) is used.

---

## Handler Details

### `GET /` — Redirect to Entries

```java
@GetMapping("/")
public String redirectToEntries() {
    return "redirect:/entries";
}
```

No model, no template — pure redirect. Ensures the root URL always sends users to the list.

---

### `GET /entries` — List Entries

```java
@GetMapping("/entries")
public String listEntries(Model model) {
    model.addAttribute("entries", guestBookService.getAllEntries());
    return "entries/list";
}
```

**Model attributes:**

| Attribute | Type | Description |
|---|---|---|
| `entries` | `List<GuestBookEntry>` | All entries, newest first |

**Template `entries/list.html` responsibilities:**
- Iterate over `entries` with `th:each`.
- Display `name`, `email`, `message`, `createdAt` per entry.
- Show flash messages (`successMessage`, `errorMessage`) from `RedirectAttributes` if present.
- Provide links: "Add New Entry" → `/entries/new`, "Edit" → `/entries/{id}/edit`, "Delete" form → POST `/entries/{id}/delete`.

---

### `GET /entries/new` — Show Add Form

```java
@GetMapping("/entries/new")
public String showAddForm(Model model) {
    model.addAttribute("entry", new GuestBookEntry());
    model.addAttribute("isEdit", false);
    return "entries/form";
}
```

**Model attributes:**

| Attribute | Type | Value | Description |
|---|---|---|---|
| `entry` | `GuestBookEntry` | empty (no-arg constructor) | Thymeleaf binds form fields to this object |
| `isEdit` | `boolean` | `false` | Controls form heading and submit button label |

---

### `POST /entries` — Create Entry

```java
@PostMapping("/entries")
public String createEntry(@ModelAttribute GuestBookEntry entry, RedirectAttributes redirectAttributes) {
    guestBookService.createEntry(entry);
    redirectAttributes.addFlashAttribute("successMessage", "Entry created successfully!");
    return "redirect:/entries";
}
```

**Form binding:** `@ModelAttribute GuestBookEntry entry` — Spring MVC maps form fields `name`, `email`, `message` from the POST body onto a new `GuestBookEntry` instance.

**Redirect-after-POST:** On success, redirects to `/entries` with a flash message. This prevents duplicate form submissions on browser refresh.

**Flash attributes** are stored in the session for exactly one subsequent request and then discarded.

---

### `GET /entries/{id}/edit` — Show Edit Form

```java
@GetMapping("/entries/{id}/edit")
public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    Optional<GuestBookEntry> entry = guestBookService.getEntryById(id);
    if (entry.isPresent()) {
        model.addAttribute("entry", entry.get());
        model.addAttribute("isEdit", true);
        return "entries/form";
    } else {
        redirectAttributes.addFlashAttribute("errorMessage", "Entry not found!");
        return "redirect:/entries";
    }
}
```

**Model attributes (success path):**

| Attribute | Type | Value | Description |
|---|---|---|---|
| `entry` | `GuestBookEntry` | existing entry | Pre-populates form fields via `th:field` |
| `isEdit` | `boolean` | `true` | Controls form heading, submit label, and form action URL |

**Error path:** If entry does not exist, redirect to `/entries` with an `errorMessage` flash attribute.

---

### `POST /entries/{id}` — Update Entry

```java
@PostMapping("/entries/{id}")
public String updateEntry(@PathVariable Long id, @ModelAttribute GuestBookEntry entry,
                          RedirectAttributes redirectAttributes) {
    try {
        guestBookService.updateEntry(id, entry);
        redirectAttributes.addFlashAttribute("successMessage", "Entry updated successfully!");
    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Entry not found!");
    }
    return "redirect:/entries";
}
```

Always redirects to `/entries` regardless of success or failure, with the appropriate flash message.

---

### `POST /entries/{id}/delete` — Delete Entry

```java
@PostMapping("/entries/{id}/delete")
public String deleteEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
    try {
        guestBookService.deleteEntry(id);
        redirectAttributes.addFlashAttribute("successMessage", "Entry deleted successfully!");
    } catch (RuntimeException e) {
        redirectAttributes.addFlashAttribute("errorMessage", "Entry not found!");
    }
    return "redirect:/entries";
}
```

---

## Thymeleaf Template Design

### `entries/list.html`

Key Thymeleaf expressions used:

```html
<!-- Flash message display -->
<div th:if="${successMessage}" class="alert alert-success" th:text="${successMessage}"></div>
<div th:if="${errorMessage}"   class="alert alert-danger"  th:text="${errorMessage}"></div>

<!-- Entry iteration -->
<tr th:each="entry : ${entries}">
    <td th:text="${entry.name}"></td>
    <td th:text="${entry.email}"></td>
    <td th:text="${entry.message}"></td>
    <td th:text="${#temporals.format(entry.createdAt, 'yyyy-MM-dd HH:mm')}"></td>
    <td>
        <a th:href="@{/entries/{id}/edit(id=${entry.id})}">Edit</a>
        <form th:action="@{/entries/{id}/delete(id=${entry.id})}" method="post">
            <button type="submit">Delete</button>
        </form>
    </td>
</tr>
```

### `entries/form.html`

Shared for both add and edit. Key patterns:

```html
<!-- Dynamic form action: /entries (add) or /entries/{id} (edit) -->
<form th:action="${isEdit} ? @{/entries/{id}(id=${entry.id})} : @{/entries}" method="post">

<!-- Dynamic heading -->
<h2 th:text="${isEdit} ? 'Edit Entry' : 'Add New Entry'"></h2>

<!-- Field binding -->
<input type="text" th:field="*{name}" />
<input type="email" th:field="*{email}" />
<textarea th:field="*{message}"></textarea>

<!-- Dynamic submit label -->
<button type="submit" th:text="${isEdit} ? 'Update Entry' : 'Add Entry'"></button>
```

`th:field="*{name}"` generates both `id="name"` and `name="name"` attributes and pre-populates the value from the bound `entry` object.

---

## Redirect-After-POST Pattern

All write operations (create, update, delete) follow the **PRG (Post/Redirect/Get)** pattern:

```
Browser         Controller
  │                │
  │  POST /entries │
  │ ──────────────▶│
  │                │  service.createEntry(...)
  │                │
  │  302 redirect  │
  │ ◀──────────────│  Location: /entries
  │                │
  │  GET /entries  │
  │ ──────────────▶│
  │                │  service.getAllEntries()
  │  200 HTML      │
  │ ◀──────────────│
```

**Benefits:**
- Prevents duplicate form submission if the user refreshes the results page.
- Flash attributes (`RedirectAttributes.addFlashAttribute`) survive exactly one redirect and are then discarded from the session.

---

## Flash Message Flow

```
POST /entries
  │
  │  redirectAttributes.addFlashAttribute("successMessage", "...")
  │
  ▼
redirect:/entries  (302)
  │
  ▼
GET /entries
  │  model automatically contains "successMessage" from the session flash scope
  │
  ▼
list.html renders <div class="alert alert-success">...</div>
  │
  ▼
Flash attribute is consumed and removed from session
```

---

## CSS and Frontend

- **Bootstrap 5** — loaded via CDN link in the base template layout. No local copy.
- **`static/css/style.css`** — custom overrides and additional styles for the guest book UI.
- No JavaScript framework — the UI is fully server-side rendered with standard HTML form submissions.
