# Nano-Spring 🌱

**Turn Android devices into lightweight, local infrastructure.**

[![Release](https://img.shields.io/badge/version-2.3.0-green.svg)](https://github.com/matheuscruzsouza/nano-spring)
[![Platform](https://img.shields.io/badge/platform-Android%20API%2025+-blue.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)](LICENSE)

Nano-Spring is a lightweight, Spring-inspired runtime for building local services directly on Android devices — including inexpensive, legacy, or repurposed hardware.

> **Spring-inspired. Android-native. Local-first.**

---

## Why Nano-Spring?

An Android phone or tablet is an all-in-one computing unit: CPU, RAM, storage, Wi-Fi, battery backup, and sensors. 

Nano-Spring lets you repurpose devices into:
* Local POS & cash register backends
* Offline inventory & order management systems
* Home automation & IoT hubs
* Local hardware & printer gateways
* Offline-first mobile & web application backends

---

## Key Features

* 🧩 **Modular Architecture**: Use only what you need (`core`, `web`, `data`, `discovery`).
* 🌐 **RESTful Routing**: `@RestController`, `@GetMethod`, `@PostMethod`, `@PutMethod`, `@DeleteMethod`.
* ⚡ **Form & Body Resolution**: Transparent binding for JSON, `application/x-www-form-urlencoded`, and query parameters into POJOs/DTOs or Maps.
* 💉 **Dependency Injection**: Seamless `@Service`, `@Repository`, and `@Autowired` with constructor and field injection.
* 🛡️ **Validation & Security**: `@Valid` bean validation, `@Authenticated` basic auth, and rate limiting.
* 📦 **Storage & Discovery**: SQLite automatic migrations and Zeroconf/mDNS service announcement.
* 🔋 **Battery & Hardware Aware**: Built-in `BatteryService` and lifecycle alerts to safeguard constrained devices.

---

## Quickstart

### 1. Requirements

* **Android**: `minSdk 25` (Android 7.1.1+), `targetSdk 36`, `compileSdk 36`
* **Java**: 11+

### 2. Installation (v2.3.0)

Add the GitHub Packages repository to your `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/matheuscruzsouza/nano-spring")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR") ?: ""
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
    }
}
```

Add dependencies to your `app/build.gradle`:

```groovy
dependencies {
    implementation 'com.github.matheuscruzsouza:nano-spring-core:2.3.0'
    implementation 'com.github.matheuscruzsouza:nano-spring-web:2.3.0'
    
    // Optional modules:
    // implementation 'com.github.matheuscruzsouza:nano-spring-data:2.3.0'
    // implementation 'com.github.matheuscruzsouza:nano-spring-discovery:2.3.0'
}
```

### 3. Create a Controller

```java
package com.example.app.controller;

import com.github.matheuscruzsouza.nanospring.annotation.*;
import com.github.matheuscruzsouza.nanospring.http.ResponseEntity;
import java.util.Map;

@RestController("/api/tickets")
public class TicketController {

    // Supports both application/json and application/x-www-form-urlencoded
    @PostMethod("")
    public ResponseEntity<Map<String, String>> createTicket(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(payload);
    }

    @GetMethod("/:id")
    public ResponseEntity<String> getTicket(@PathVariable("id") String id) {
        return ResponseEntity.ok("Ticket ID: " + id);
    }
}
```

### 4. Start the Server

Using an Android Foreground Service:

```java
public class BackendService extends com.github.matheuscruzsouza.nanospring.service.NanoSpringService {
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

Or programmatically:

```java
Server server = new Server(context, 8080, "com.example.app");
server.start();
```

---

## Detailed Documentation

To explore in-depth topics, refer to our specialized documentation:

* 📐 [**Architecture & Design Principles**](docs/architecture.md) — Internal mechanics, reflection lifecycle, and local-first architecture.
* 🛠️ [**Features & Modules Guide**](docs/features-guide.md) — Security, file uploads, SSE, SQLite migrations, mDNS, and templates.
* 📱 [**Hardware, Reliability & Lifecycle**](docs/hardware-and-lifecycle.md) — Background services, WakeLocks, battery monitoring, and constrained hardware resilience.
* ⚖️ [**Spring Boot vs Nano-Spring**](docs/spring-boot-vs-nano-spring.html) — Key conceptual differences and migration guide.
* 🤖 [**AI-Assisted Development**](AGENTS.md) — Context and instructions for GitHub Copilot, Cursor, and LLM coding assistants.
* 🌐 [**Web Documentation Portal**](docs/index.html) — Interactive HTML documentation and OpenAPI guides.

---

## Contributing & License

Contributions are welcome! Please report issues or submit PRs on [GitHub](https://github.com/matheuscruzsouza/nano-spring).

Licensed under the [MIT License](LICENSE).
