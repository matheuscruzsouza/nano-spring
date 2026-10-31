# Advanced Features & Guides

This guide contains detailed references for advanced capabilities in **Nano-Spring**.

---

## 1. Security & Authentication

### HTTP Basic Authentication

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

> [!WARNING]
> - **Illustrative credentials**: The values above (`admin` / `secret123`) are purely illustrative examples for local testing. Always set strong, non-default credentials in `application.properties`. If not defined, fallback defaults to `"admin"` / `"admin"`.
> - **Plaintext Exposure (No Encryption)**: HTTP Basic Auth **only Base64-encodes** credentials (`username:password`)—it does **not** encrypt them. On unencrypted HTTP connections across untrusted or public networks (such as public Wi-Fi), credentials can easily be intercepted. **Always enable HTTPS/TLS (`server.ssl.enabled=true`)** when transmitting Basic Auth over untrusted networks.

### HTTPS / TLS Configuration

To encrypt traffic and protect credentials in transit, configure SSL/TLS natively via `application.properties`:

```properties
# Enable TLS / HTTPS socket
server.ssl.enabled=true

# Keystore path (assets/ or absolute filesystem path)
server.ssl.key-store=certificates/keystore.p12

# Keystore password
server.ssl.key-store-password=your-secure-password

# Keystore format (PKCS12, BKS or JKS; default: PKCS12)
server.ssl.key-store-type=PKCS12

# Optional allowed protocols
server.ssl.protocols=TLSv1.2,TLSv1.3
```

### Administrative Endpoints Protection

- **Built-in Actuator Endpoints (`/actuator/*`)**:
  - Protected by default using HTTP Basic Authentication (`nano.actuator.secure=true`).
  - `/actuator/logfile` requires explicit enablement (`nano.actuator.logfile.enabled=true`), otherwise returning `403 Forbidden`.
- **Application Administrative Endpoints**:
  - Unlike standard Spring Security, Nano-Spring does not automatically protect `/admin/**` by path matching.
  - Custom administrative controllers or methods must be **explicitly protected** using `@Authenticated` (on the method or `@RestController` class) or mapped using a custom `HandlerInterceptor`.


### Rate Limiting (Token Bucket Anti-DoS)

Nano-Spring provides an in-memory **Token Bucket** rate limiter (`TokenBucketRateLimiter`) to protect resource-constrained Android devices from high-volume DoS bursts.

#### How It Works
- **Algorithm**: In-Memory Token Bucket. Each incoming request consumes 1 token. Tokens refill continuously over time up to the configured burst capacity. When a bucket has fewer than 1 token available, requests are rejected immediately. Stale client buckets (idle for over 60 seconds based on last request activity) are automatically pruned when the cache exceeds 500 entries to prevent memory leaks on Android.
- **Client Identification**: Clients are identified per remote IP address (`session.getRemoteIpAddress()`). Bucket keys are isolated by IP for global checks (`GLOBAL:<ip>`) and by endpoint + IP for route checks (`ROUTE:<Class>#<method>:<ip>`).
- **HTTP 429 Response**: When limits are exceeded, the server immediately returns `429 Too Many Requests` (`application/json`) with a `Retry-After` response header:
  ```json
  {
    "status": 429,
    "error": "Too Many Requests",
    "message": "Rate limit exceeded for route"
  }
  ```
  *(For global limits, the message is `"Global rate limit exceeded"` and `Retry-After` is `1` second)*.

#### Global Configuration
Enable and configure global rate limiting across all incoming requests in `application.properties`:
```properties
# Enable global rate limiting (default: false)
nano.ratelimit.enabled=true

# Sustained requests per second per IP (refill rate, default: 50)
nano.ratelimit.requests-per-second=50

# Maximum burst capacity of tokens per IP (default: 100)
nano.ratelimit.burst-capacity=100
```

#### Route-Level Protection (`@RateLimit`)
Annotate controller classes or individual methods using `@RateLimit` (from `com.github.matheuscruzsouza.nanospring.security.RateLimit`):

```java
import com.github.matheuscruzsouza.nanospring.annotation.*;
import com.github.matheuscruzsouza.nanospring.security.RateLimit;

@RestController("/api/payments")
public class PaymentController {

    // Allow a maximum of 5 requests every 60 seconds per client IP
    @RateLimit(requests = 5, durationSeconds = 60)
    @PostMethod("/process")
    public ResponseEntity<String> processPayment(@RequestBody PaymentRequest req) {
        return ResponseEntity.ok("Payment processed successfully");
    }
}
```
*Note: Default values when not specified are `requests = 10` and `durationSeconds = 1`.*


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

Render static files from `assets/static/` (automatically served with `Cache-Control: public, max-age=3600`) or dynamic views from `assets/templates/` with `ModelAndView`:

```java
@GetMethod("/dashboard")
public ModelAndView dashboard() {
    ModelAndView mv = new ModelAndView("dashboard");
    mv.addObject("title", "Local POS Server");
    return mv;
}
```
