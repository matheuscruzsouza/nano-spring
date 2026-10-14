# Advanced Features & Guides

This guide contains detailed references for advanced capabilities in **Nano-Spring**.

---

## 1. Security & Authentication

Endpoints can require HTTP Basic Authentication using `@Authenticated`:

```java
@Authenticated
@GetMethod("/admin/status")
public Map<String, Object> adminStatus() {
    return Map.of("status", "UP");
}
```

Credentials are configured in `application.properties`:
```properties
nano.security.username=admin
nano.security.password=secret123
```

---

## 2. File Uploads & Downloads

Inject uploaded files with `@UploadedFile`:

```java
@PostMethod("/upload")
public ResponseEntity<String> handleUpload(@UploadedFile("file") File file) {
    long size = file.length();
    return ResponseEntity.ok("Received file size: " + size);
}
```

---

## 3. Server-Sent Events (SSE)

Stream real-time updates over HTTP:

```java
@GetMethod("/events")
public SseEmitter streamEvents() {
    SseEmitter emitter = new SseEmitter();
    // emit data asynchronously
    return emitter;
}
```

---

## 4. SQLite Migrations & Persistence

Organize migrations in `assets/db/migrations/` (e.g. `V1__init.sql`):
```sql
CREATE TABLE products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    price REAL NOT NULL
);
```

Inject repositories using `@Repository`:
```java
@Repository
public class ProductRepository {
    // Database operations using Android SQLiteDatabase
}
```

---

## 5. Local Service Discovery (mDNS / Zeroconf)

Publish your device on the local network (`.local` domain):
```properties
nano.nsd.enabled=true
nano.nsd.name=pos-server
nano.nsd.type=_http._tcp
```

---

## 6. HTML & Mustache Templates

Render static files from `assets/static/` or dynamic views from `assets/templates/` with `ModelAndView`:

```java
@GetMethod("/dashboard")
public ModelAndView dashboard() {
    ModelAndView mv = new ModelAndView("dashboard");
    mv.addObject("title", "Local POS Server");
    return mv;
}
```
