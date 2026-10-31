# Architecture & Design Principles

This document details the architecture, design principles, and runtime considerations of **Nano-Spring**.

---

## High-Level Architecture

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

Nano-Spring provides the application runtime while Android remains responsible for underlying operating-system and hardware capabilities.

---

## Design Principles

### 1. Local-first
Applications should operate on the local network without requiring permanent cloud infrastructure. Cloud infrastructure can be added as an enhancement, not a bottleneck.

### 2. Small footprint
The runtime avoids heavy abstraction layers, keeping memory consumption predictably between 15 MB and 40 MB of RAM.

### 3. Predictability over peak performance
Bounded thread pools, queue limits, and controlled memory allocations take precedence over theoretical peak requests per second.

### 4. Reuse before replacement
Constrained, older Android hardware (such as decommissioned smartphones or dedicated POS terminals) is repurposed into functional, robust servers.

### 5. Familiar development model
Concepts follow familiar Spring patterns (`@RestController`, `@Service`, `@Repository`, `@Autowired`, `@Valid`), but implemented natively for Android under `com.github.matheuscruzsouza.nanospring.*`.

### 6. Android-native
The framework runs in-process inside Android, taking advantage of `Context`, SQLite, `BatteryManager`, `WifiManager`, and `NsdManager` directly.

### 7. Recoverability
Foreground services recover gracefully from network disconnects, battery warnings, and lifecycle pauses.

---

## How It Works Internally

1. **Startup Scanning**: During application boot, Nano-Spring inspects the application package using reflection and Android `DexFile` analysis to locate `@RestController`, `@Service`, and `@Repository` components.
2. **Dependency Graph**: Singletons and controllers are instantiated, resolving constructor and `@Autowired` field dependencies.
3. **Route Registration**: Methods annotated with `@GetMethod`, `@PostMethod`, `@PutMethod`, and `@DeleteMethod` are compiled into regex route matchers within `ServerIndexHandler`.
4. **Execution Cycle**: When an HTTP request arrives, `ServerIndexHandler` matches the route, converts parameters (`@PathVariable`, `@RequestParam`, `@RequestHeader`, `@RequestBody`), applies Bean Validation (`@Valid`), executes the method, and serializes the response.
5. **Transport & Media Streaming (HTTP/1.1)**: Nano-Spring runs on an embedded HTTP/1.1 engine (`NanoHTTPD`) over `FastSocketWrapper` (which disables reverse-DNS PTR lookups, enables `TCP_NODELAY`, and configures 256 KB send / 128 KB receive socket buffers). Media and binary downloads use 64 KB buffered `FileChannel` streams (`FileResourceRegion` / `ByteRangeHandler`) with `HTTP Range` (`206 Partial Content`) support and automatic GZIP bypass for binary/media MIME types.
