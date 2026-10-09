# Nano-Spring 🌱

**Turn old Android devices into local infrastructure.**

Nano-Spring is a lightweight, Spring-inspired runtime for building local services on Android devices — including hardware that is old, resource-constrained, or simply inexpensive enough to be repurposed as a dedicated local server.

It brings familiar Java/Spring-style concepts such as dependency injection, controllers, services, repositories, configuration, validation and application lifecycle to Android, while integrating directly with the platform's networking, storage and device capabilities.

> **Spring-inspired. Android-native. Local-first.**

---

## Why Nano-Spring?

An old Android phone may no longer be useful as a modern smartphone, but it still has many of the components required to provide useful local infrastructure:

* CPU
* RAM
* persistent storage
* Wi-Fi
* Bluetooth
* USB
* battery
* sensors
* Android's networking and hardware APIs

For many applications, that is enough.

A home, small business, workshop, store or IoT installation may not need a cloud server or a dedicated computer.

It may simply need a small local server.

Nano-Spring exists to make that kind of application easier to build.

### Examples

An Android device can become:

* a local inventory server;
* a small POS backend;
* an order management system;
* a home automation hub;
* an IoT gateway;
* a local dashboard;
* a printer or hardware gateway;
* a device-control API;
* an offline-first application backend;
* a local data collection service;
* or a local HTTP service for another application.

The idea is simple:

> **Reuse existing hardware before replacing it or introducing unnecessary infrastructure.**

---

# Local-first by design

Nano-Spring is designed around applications that can operate primarily on the local network.

Cloud infrastructure can be added when it is useful, but the core application does not need to depend on a remote server.

```text
                         Optional Cloud
                              │
                              │
                              ▼
┌──────────────┐       ┌───────────────┐
│ Local Client │──────▶│  Nano-Spring  │
│ Web / Mobile │       │    Runtime    │
└──────────────┘       └───────┬───────┘
                               │
                ┌──────────────┼──────────────┐
                │              │              │
                ▼              ▼              ▼
             SQLite        Android APIs     Network
                │              │              │
                └──────────────┼──────────────┘
                               ▼
                       Android Device
```

This architecture is particularly useful when:

* Internet access is unreliable;
* the application has a small number of users;
* data should remain on-site;
* local network latency matters;
* cloud infrastructure would add unnecessary cost or complexity;
* or an existing Android device is already available.

---

# Why Android devices?

An Android device already provides much of the infrastructure required by a small local service.

| Resource     | Possible use                                      |
| ------------ | ------------------------------------------------- |
| CPU          | Application and request processing                |
| RAM          | Runtime, application state and caching            |
| Storage      | SQLite databases and application data             |
| Wi-Fi        | Local network connectivity                        |
| Bluetooth    | Local peripherals and devices                     |
| USB          | Hardware and peripheral integration               |
| Battery      | Short-term power continuity                       |
| Sensors      | IoT and environmental applications                |
| Android APIs | Networking, connectivity and hardware integration |

Nano-Spring uses Android as the underlying platform instead of requiring a separate server operating system or dedicated computer.

---

# Why "Spring-inspired"?

The Spring ecosystem is widely known among Java developers.

Many developers are already familiar with concepts such as:

* Dependency Injection
* Controllers
* Services
* Repositories
* Configuration
* Validation
* Application lifecycle
* Interceptors
* Exception handling

Nano-Spring uses these concepts as a **familiar development model**.

It is not intended to reproduce Spring Boot or provide Spring compatibility.

Instead, it brings a similar style of application development to a much smaller Android-oriented runtime.

The goal is:

> **Make local Android infrastructure familiar to developers who already understand the Spring programming model.**

For example, a developer familiar with:

```java
@RestController
@Service
@Repository
@Autowired
```

can recognize the architectural concepts immediately, even though Nano-Spring has its own runtime and Android-specific APIs.

---

# What can you build?

## Small business

* Inventory systems
* Point-of-sale backends
* Order management
* Local reports
* Product catalogs
* Customer management
* Printer gateways
* Hardware integrations

## Home

* Home automation
* Local dashboards
* Sensor aggregation
* Device control
* Local APIs
* Household services

## IoT

* Device gateways
* Sensor collection
* Local command APIs
* Edge processing
* Local data storage
* Protocol bridges

## Offline-first applications

* Local data collection
* Applications that continue operating without Internet access
* Local synchronization gateways
* Applications that synchronize with cloud services only when connectivity is available

---

# Features

Nano-Spring provides a collection of infrastructure components for local Android services.

## Application runtime

* Dependency Injection
* Component scanning
* `@Service`
* `@Repository`
* `@Autowired`
* Application configuration
* Profiles
* Environment properties
* Application lifecycle

## HTTP and REST

* Embedded HTTP server based on NanoHTTPD
* RESTful routing
* `@RestController`
* `@GetMethod`
* `@PostMethod`
* `@PutMethod`
* `@DeleteMethod`
* `@PathVariable`
* `@RequestBody`
* `@RequestHeader`
* `ResponseEntity`
* JSON serialization with Gson
* Multipart file uploads
* File downloads and streaming
* Static files
* Server-Sent Events
* CORS
* TLS/HTTPS
* Rate limiting
* Request interceptors
* Global exception handling

## Persistence

* Native Android SQLite integration
* SQLite WAL mode
* Database migrations
* Versioned SQL migration files
* Automatic schema history
* Repository integration

## Validation

Declarative validation using annotations such as:

* `@Valid`
* `@NotNull`
* `@NotBlank`
* `@Size`
* `@Min`
* `@Max`
* `@Email`
* `@Pattern`

## Android integration

* `NanoSpringService`
* Android lifecycle integration
* Wake locks
* Wi-Fi multicast support
* Network monitoring
* Connectivity recovery
* Native Android networking APIs

## Local networking

* mDNS / DNS-SD
* `.local` host names
* Android `NsdManager`
* Automatic service registration
* Network-change detection and re-announcement

## Observability

* Actuator-style health endpoint
* Application information endpoint
* Remote log endpoint
* Rotating file logs
* Configurable log retention
* Runtime diagnostics

## Developer experience

* Environment profiles
* `application.properties`
* `@Value`
* Asynchronous controllers using `CompletableFuture`
* Configurable asynchronous timeouts
* OpenAPI 3.0 generation
* Built-in Swagger UI
* Native AI Assistant support (Javadocs and AGENTS.md)

---

# Reference hardware

Nano-Spring is intentionally developed with constrained Android hardware in mind.

The current reference development device is:

> **Motorola Moto G4 Play**

This is not because Nano-Spring is limited to that device.

It is because an older device provides a useful constraint for development.

The project therefore emphasizes:

* predictable memory usage;
* bounded concurrency;
* controlled thread creation;
* efficient persistence;
* limited storage growth;
* network recovery;
* long-running operation;
* and graceful failure handling.

The objective is not simply to achieve the highest possible benchmark numbers.

The objective is to make small local services practical on hardware that is already available.

---

# From old phone to local server

The intended deployment model is simple:

```text
Old Android device
        │
        ▼
Install application
        │
        ▼
Start Nano-Spring
        │
        ▼
Device joins local network
        │
        ▼
HTTP server starts
        │
        ▼
mDNS advertises the service
        │
        ▼
Local clients connect
        │
        ▼
Application runs locally
```

A device can therefore provide an HTTP service without requiring:

* a VPS;
* a cloud database;
* a dedicated server;
* a Kubernetes cluster;
* or a permanent Internet connection.

---

# Local service discovery

Local infrastructure should not require users to memorize an IP address.

Nano-Spring includes mDNS / DNS-SD support through Android's `NsdManager`.

For example, a device can expose a service using:

```text
http://meu-servidor-android.local:8080/
```

instead of requiring the user to discover an address such as:

```text
http://192.168.1.137:8080/
```

Configuration:

```properties
nano.nsd.enabled=true
nano.nsd.name=meu-servidor-android
nano.nsd.type=_http._tcp.
nano.nsd.host-resolution=true
```

The service can also be enabled programmatically:

```java
server.enableNsd("meu-servidor", true);
```

and disabled with:

```java
server.disableNsd();
```

When the server stops, its mDNS registration is removed.

---

# Requirements

## Android

| Requirement             | Current value          |
| ----------------------- | ---------------------- |
| Minimum Android version | Android 7.1.1 / API 25 |
| `minSdk`                | 25                     |
| `targetSdk`             | 36                     |
| `compileSdk`            | 36                     |
| Java                    | 11                     |

Nano-Spring is therefore compatible with Android devices starting from API 25, subject to the behavior and restrictions of the specific Android version and device.

Older Android devices remain an important part of the project's testing strategy.

---

# Installation

Nano-Spring can currently be used either as a local Gradle module or as a package published to GitHub Packages.

## Option 1 — Local module

Clone the repository and include it as a module in your Android project.

Then:

```gradle
dependencies {
    implementation project(':nano-spring-core')      // Core DI and Environment
    implementation project(':nano-spring-web')       // HTTP Server, Routers, OpenAPI
    implementation project(':nano-spring-data')      // SQLite and Migrations (Optional)
    implementation project(':nano-spring-discovery') // mDNS Discovery (Optional)
}
```

This is the simplest option when developing Nano-Spring itself or experimenting with the framework.

---

## Option 2 — GitHub Packages

The current published artifact is:

```text
com.github.matheuscruzsouza:nano-spring:1.11.0
```

Nano-Spring is published through GitHub Packages.

GitHub Packages requires authentication for package downloads, so configure credentials in your Gradle environment.

For example, in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

The token requires package read access.

Then add the GitHub Packages repository to your application's `settings.gradle`:

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/matheuscruzsouza/nano-spring")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull
                        ?: System.getenv("GITHUB_ACTOR")
                        ?: ""

                password = providers.gradleProperty("gpr.key").orNull
                        ?: System.getenv("GITHUB_TOKEN")
                        ?: ""
            }
        }
    }
}
```

Then add the dependency:

```gradle
dependencies {
    implementation 'com.github.matheuscruzsouza:nano-spring-web:2.0.0'
    // Note: Em versões 2.x o framework é modularizado (core, web, data, discovery)
}
```

---

# Starting the server

Nano-Spring supports starting the HTTP server through `NanoSpringService` or directly through the `Server` class.

## Using `NanoSpringService`

For applications that need an Android service lifecycle:

```java
public class MyBackendService extends NanoSpringService {

    @Override
    protected String getBasePackage() {
        return "com.example.app";
    }

    @Override
    protected int getPort() {
        return 8080;
    }
}
```

The service provides Android-specific infrastructure such as notification handling and device/network locks.

The exact foreground-service configuration should be selected according to the Android version and the actual type of work performed by the application.

---

## Starting the server directly

For applications that manage their own lifecycle:

```java
Server server = new Server(
        context,
        8080,
        "com.example.app"
);
```

---

# Configuration

Nano-Spring uses `application.properties` from the application's assets.

Example:

```properties
server.port=9090

nano.nsd.enabled=true
nano.nsd.name=meu-servidor
nano.nsd.type=_http._tcp.
nano.nsd.host-resolution=true

nano.network.watcher.enabled=true

nano.datasource.wal.enabled=true
```

Application-specific values can be injected using `@Value`:

```java
@RestController("/api/config")
public class ConfigController {

    @Value("${api.key:default-key}")
    private String apiKey;

    @GetMethod("")
    public String getKey() {
        return apiKey;
    }
}
```

For sensitive production credentials, do not treat `application.properties` as a secure secret store.

---

# Building a REST API

A controller can be defined using familiar Spring-style annotations.

```java
@RestController("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMethod("/:id")
    public User getUser(
            @PathVariable("id") String id,
            @RequestHeader("Authorization") String token) {

        return userService.findById(Integer.parseInt(id));
    }

    @PostMethod("")
    public ResponseEntity<User> createUser(
            @RequestBody User newUser) {

        User saved = userService.save(newUser);

        return ResponseEntity
                .created("/api/users/" + saved.getId())
                .body(saved);
    }
}
```

---

# Dependency Injection

Components can be registered using familiar annotations:

```java
@Service
public class UserService {

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}
```

Repositories can use the Android SQLite database:

```java
@Repository
public class UserRepository {

    @Autowired
    private SQLiteDatabase db;

    public List<String> listUsers() {
        List<String> users = new ArrayList<>();

        try (Cursor cursor =
                     db.rawQuery("SELECT nome FROM users", null)) {

            while (cursor.moveToNext()) {
                users.add(cursor.getString(0));
            }
        }

        return users;
    }
}
```

---

# File uploads and downloads

Multipart uploads are supported through `@UploadedFile`.

```java
@RestController("/api/uploads")
public class UploadController {

    @PostMethod("")
    public String upload(
            @UploadedFile("file") File file) {

        if (file == null) {
            return "No file received";
        }

        return "Uploaded " + file.length() + " bytes";
    }
}
```

Controllers can also return `File` objects for file streaming.

```java
@GetMethod(
        value = "/download/:id",
        mimeType = "application/octet-stream"
)
public Object download(
        @PathVariable("id") String id) {

    File file = fileService.getFileOnDisk(id);

    if (file.exists()) {
        return file;
    }

    return "File not found";
}
```

---

# Server-Sent Events

Nano-Spring provides `SseEmitter` for streaming events to web clients.

```java
@RestController("/api/stream")
public class NotificationController {

    @GetMethod(
            value = "/alerts",
            mimeType = "text/event-stream"
    )
    public SseEmitter alerts() {

        SseEmitter emitter = new SseEmitter();

        // Produce events asynchronously.

        return emitter;
    }
}
```

The client can consume the stream using the browser's native `EventSource` API:

```javascript
const eventSource =
    new EventSource('/api/stream/alerts');

eventSource.addEventListener('alerta', event => {
    const data = JSON.parse(event.data);
    console.log(data);
});
```

`SseEmitter` also supports lifecycle callbacks and keep-alive behavior for long-running connections.

---

# SQLite and migrations

Nano-Spring integrates with Android's native SQLite implementation.

Database migrations can be placed under:

```text
assets/db/migration/
```

For example:

```text
V1__create_users_table.sql
V2__add_status_column.sql
V3__create_orders_table.sql
```

Example:

```sql
CREATE TABLE users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nome TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE
);
```

Nano-Spring maintains migration history and executes pending migrations within transactions.

SQLite WAL mode can be enabled with:

```properties
nano.datasource.wal.enabled=true
```

---

# Network recovery

Android devices can change their network configuration while running.

Wi-Fi can disconnect, reconnect, or receive a different IP address.

Nano-Spring provides `NetworkWatcher` for monitoring connectivity changes and re-announcing local services when appropriate.

```properties
nano.network.watcher.enabled=true
```

This is particularly relevant when an Android device is being used as a permanent local server.

---

# Concurrency and resource limits

Resource-constrained hardware should avoid unbounded thread creation.

Nano-Spring provides a bounded asynchronous execution model:

```properties
nano.server.threads.core=4
nano.server.threads.max=16
nano.server.threads.queue-capacity=100
nano.server.threads.keep-alive=60
```

The exact values should be selected according to the device and workload.

The purpose is to keep concurrency bounded instead of allowing traffic spikes to create an uncontrolled number of threads.

---

# CORS

CORS can be configured globally:

```properties
nano.cors.enabled=true
nano.cors.allowed-origins=*
nano.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
nano.cors.allowed-headers=Content-Type,Authorization,X-Requested-With,Accept
nano.cors.max-age=86400
```

For applications exposed beyond a trusted local environment, configure allowed origins explicitly rather than relying on permissive defaults.

---

# Security

Running a server on a local network does not automatically make it secure.

Nano-Spring provides several building blocks for securing applications.

## TLS

HTTPS can be enabled using a Java/Android keystore:

```properties
server.ssl.enabled=true
server.ssl.key-store=certificates/keystore.p12
server.ssl.key-store-password=YOUR_PASSWORD
server.ssl.key-store-type=PKCS12
```

## Rate limiting

Routes can define request limits:

```java
@RateLimit(
        requests = 5,
        durationSeconds = 60
)
@PostMethod("/api/checkout")
public ResponseEntity<?> checkout(
        @RequestBody PaymentDTO payment) {

    // ...
}
```

Requests exceeding the configured limit receive HTTP `429`.

## Interceptors

Custom `HandlerInterceptor` implementations can inspect and reject requests before they reach controllers.

```java
@Interceptor
@Order(1)
public class SecurityInterceptor
        implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            NanoHTTPD.IHTTPSession session,
            String path) {

        // Authentication / authorization logic

        return true;
    }
}
```

## Global exception handling

Applications can centralize exception handling:

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NumberFormatException.class)
    public ErrorResponse handleFormatError(
            NumberFormatException ex) {

        return new ErrorResponse(
                "BAD_REQUEST",
                "Invalid number format"
        );
    }
}
```

---

# Validation

DTOs can use declarative validation:

```java
public class UserDTO {

    @NotNull
    @NotBlank
    private String name;

    @Email
    private String email;

    @Min(18)
    @Max(120)
    private int age;
}
```

Then validate request bodies directly:

```java
@PostMethod("/api/users")
public ResponseEntity<?> create(
        @Valid @RequestBody UserDTO user) {

    return ResponseEntity.ok(
            userService.save(user)
    );
}
```

---

# HTML, templates and static files

Nano-Spring can also serve web interfaces directly from the Android device.

Templates can be placed under:

```text
assets/templates/
```

and rendered using `ModelAndView`.

```java
@RestController("/web")
public class WebController {

    @GetMethod("/profile/:id")
    public ModelAndView profile(
            @PathVariable("id") String id) {

        User user = userService.findById(id);

        return new ModelAndView("user_profile")
                .addObject("username", user.getName());
    }
}
```

Static resources can be placed under:

```text
app/src/main/assets/static/
```

and accessed directly:

```html
<link
    rel="stylesheet"
    href="/static/css/style.css">

<script
    src="/static/js/main.js">
</script>
```

---

# Profiles

Different configurations can be selected using profiles.

For example:

```properties
nano.profiles.active=dev
```

with:

```text
assets/application.properties
assets/application-dev.properties
```

The profile-specific properties override the base configuration.

This is useful when the same application needs different configurations for development, testing and deployment.

---

# Async controllers

Operations involving slow hardware or peripherals can be exposed asynchronously.

For example:

```java
@PostMethod("/api/pos/imprimir")
public CompletableFuture<ResponseEntity<Map<String, Object>>>
        imprimir(@RequestBody CupomDTO cupom) {

    return CompletableFuture.supplyAsync(() -> {

        impressoraHardware.imprimir(cupom);

        return ResponseEntity.ok(
                Map.of("impresso", true)
        );
    });
}
```

Timeouts can be configured:

```properties
nano.async.timeout-seconds=30
nano.server.read-timeout=10000
```

This is useful for integrations involving hardware such as printers, Bluetooth peripherals or other operations that may take longer than a normal HTTP request.

---

# Observability

Nano-Spring provides Actuator-style endpoints:

```text
/actuator/health
/actuator/info
/actuator/logfile
```

For example:

```bash
curl http://localhost:8080/actuator/health
```

The health information can include runtime, database, network and Android-related information.

---

# Rotating logs

Local servers should avoid unlimited log growth.

Nano-Spring provides rotating file logging:

```properties
nano.logging.enabled=true
nano.logging.level=INFO
nano.logging.max-size-mb=5
nano.logging.max-history=3
```

Logs can also be inspected remotely through:

```text
/actuator/logfile
```

For example:

```bash
curl \
  "http://device.local:8080/actuator/logfile?lines=100"
```

Management endpoints should be protected appropriately when the service is exposed to untrusted networks.

---

# OpenAPI and Swagger UI

Nano-Spring can generate OpenAPI 3.0 documentation and provide a local Swagger UI.

The service exposes:

```text
/swagger-ui
/swagger-ui.html
/v3/api-docs
```

For example:

```text
http://device.local:8080/swagger-ui
```

The interface is served locally, so API documentation does not require an external Internet connection.

Configuration:

```properties
nano.swagger.enabled=true
nano.swagger.title=My Local API
nano.swagger.version=1.0.0
nano.swagger.description=Local application API
```

---

# Developing with AI

Nano-Spring has native support for AI code assistants (such as Copilot, Cursor, Windsurf, etc.). 
Since Nano-Spring does NOT use the standard Spring Boot packages (`org.springframework.*`), it provides context natively to prevent AI hallucinations.

When importing the package, all primary annotations (`@RestController`, `@Service`, `@Repository`, etc.) include **Defensive AI Javadocs**. Modern IDEs will automatically read these Javadocs and instruct the AI to use the correct Nano-Spring packages.

To further improve AI generation in your project, copy the [AGENTS.md](AGENTS.md) file and the `docs/ai/` directory from this repository into your own project's root. The AI will then automatically know how to structure Nano-Spring specific controllers, services, and SQLite repositories.

---

# Reliability on constrained hardware

Nano-Spring is not primarily designed around maximum requests per second.

The more important question is:

> **Can a local service continue operating predictably on inexpensive, resource-constrained hardware?**

For that reason, important engineering concerns include:

* memory usage;
* CPU utilization;
* thread count;
* bounded queues;
* garbage collection;
* storage growth;
* database behavior;
* network recovery;
* application restart;
* long-running operation;
* battery and power behavior;
* Android background execution restrictions.

The Motorola Moto G4 Play is used as a reference development device precisely because it provides a constrained environment for testing these characteristics.

---

# Performance

Performance measurements should always describe their environment.

Useful measurements include:

* startup time;
* memory consumption;
* CPU utilization;
* request latency;
* concurrent connections;
* database performance;
* storage usage;
* thread count;
* garbage collection;
* network recovery;
* long-running stability.

A benchmark should report at least:

```text
Device: Motorola Moto G4 Play
Android: <version>
Build: <debug/release>
R8: <enabled/disabled>
Database: <configuration>
Workload: <description>
Duration: <duration>
```

This makes results easier to reproduce and compare.

---

# Android lifecycle

Android is not a conventional server operating system.

Applications running long-lived local services must account for:

* application lifecycle;
* background execution restrictions;
* foreground services;
* Doze;
* battery optimization;
* Wi-Fi state;
* connectivity changes;
* vendor-specific Android behavior;
* device restarts.

Nano-Spring provides Android-specific infrastructure, but applications are still responsible for following the lifecycle and background-execution rules of the Android version on which they run.

For deployments intended to operate continuously, the target Android version and device should therefore be tested explicitly.

---

# Architecture

At a high level:

```text
┌──────────────────────────────────────┐
│            Application               │
│                                      │
│ Controllers / Services / Repositories│
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│             Nano-Spring              │
│                                      │
│ DI │ HTTP │ Validation │ Persistence │
│ Lifecycle │ Config │ Observability   │
└──────────────────┬───────────────────┘
                   │
                   ▼
┌──────────────────────────────────────┐
│              Android                 │
│                                      │
│ Network │ Storage │ Services │ APIs  │
└──────────────────┬───────────────────┘
                   │
                   ▼
             Android Device
```

The framework provides the application runtime while Android remains responsible for the underlying operating-system and hardware capabilities.

---

# Design principles

## Local-first

Applications should be useful on the local network without requiring permanent cloud infrastructure.

## Small footprint

The runtime should avoid unnecessary infrastructure and dependencies.

## Predictability over peak performance

Bounded resources and predictable behavior are more important than maximizing benchmark numbers.

## Reuse before replacement

Existing Android hardware can provide useful infrastructure long after its original smartphone role becomes obsolete.

## Familiar development model

Developers should be able to use concepts they already understand from the Java/Spring ecosystem.

## Android-native

The framework should use Android's native capabilities rather than requiring a separate server environment.

## Recoverability

Long-running services should be designed to recover from transient network and infrastructure failures.

## Simple deployment

Turning an Android device into a local service should require as little infrastructure as possible.

---

# What Nano-Spring is not

Nano-Spring is not intended to be:

* a replacement for Spring Boot;
* a Spring-compatible runtime;
* a general-purpose cloud platform;
* a Kubernetes alternative;
* a distributed systems platform;
* a high-throughput Internet backend;
* or a replacement for enterprise server infrastructure.

Its target is different:

> **Small, local services running directly on Android devices.**

---

# How it works internally

Nano-Spring currently uses Java Reflection together with Android `DexFile` scanning during application startup.

The framework scans the application package for components such as:

```text
@RestController
@Service
@Repository
```

and builds the runtime structures required for dependency injection and HTTP routing.

The reflection work is concentrated during startup rather than being repeatedly performed for every request.

This approach keeps the runtime programming model simple while remaining suitable for Android applications.

---

# Project status

Nano-Spring is an evolving project.

The current development focus is on making the framework useful as a foundation for local infrastructure running on Android devices, especially older and resource-constrained hardware.

Current areas of focus include:

* runtime stability;
* Android compatibility;
* resource efficiency;
* local networking;
* persistence;
* security;
* observability;
* and long-running operation.

APIs and implementation details may change as the project evolves.

---

# Roadmap

## Reliability

* [ ] Long-running stability tests
* [ ] Memory/resource monitoring
* [ ] Network recovery improvements
* [ ] Database recovery
* [ ] Runtime watchdog
* [ ] Graceful restart
* [ ] Failure recovery improvements

## Android

* [ ] Expanded Android compatibility matrix
* [ ] Background execution improvements
* [ ] Power-management handling
* [ ] Device-specific compatibility testing

## Security

* [ ] Harden management endpoints
* [ ] Improve secret management
* [ ] Authentication and authorization improvements
* [ ] Security documentation
* [ ] Secure defaults

## Infrastructure

* [ ] Backup and restore
* [ ] Device provisioning
* [ ] Improved local service discovery
* [ ] Device management

## Developer experience

* [ ] More documentation
* [ ] More complete examples
* [ ] Better diagnostics
* [ ] Improved testing infrastructure
* [ ] Generated metadata where appropriate

---

# Contributing

Contributions are welcome.

Particularly useful contributions include:

* Android compatibility testing;
* testing on older devices;
* performance measurements;
* long-running reliability testing;
* security reviews;
* documentation;
* examples;
* bug reports;
* framework development.

When reporting a device-specific problem, include:

```text
Device:
Android version:
Nano-Spring version:
Build type:
Workload:
Expected behavior:
Actual behavior:
```

Testing Nano-Spring on older Android hardware is especially valuable because constrained devices are an important part of the project's target environment.

---

# License

See [LICENSE](LICENSE) for the project license.

---

## Repository

[GitHub](https://github.com/matheuscruzsouza/nano-spring)
