# Hardware, Reliability & Android Lifecycle

This document describes hardware expectations, performance benchmarks, and Android lifecycle management when running persistent services with **Nano-Spring**.

---

## Constrained Hardware & Reliability

Nano-Spring is tested against constrained devices (such as the Motorola Moto G4 Play / Nougat 7.1.1 API 25) to guarantee:
* Memory footprint bounded below 40MB heap.
* Controlled thread pools to prevent aggressive context-switching.
* Clean garbage collection patterns.
* Stability across continuous days of execution.

---

## Android Lifecycle Management

Android is not a standard Linux server. Long-lived local services must actively manage:
* **Foreground Services**: Requires `FOREGROUND_SERVICE` and appropriate foreground service types on modern Android versions.
* **WakeLocks & WifiLocks**: Held prudently to prevent the OS from suspending the CPU or network interfaces during idle periods.
* **Doze Mode & Battery Optimizations**: Applications should instruct users to disable aggressive vendor battery optimizations.
* **Network Connectivity**: Built-in broadcast receivers detect IP changes (e.g. Wi-Fi reconnection) and re-announce local services.

---

## Battery Management

When running on battery or during power outages:
* `BatteryService` provides current battery metrics (percentage, temperature, charging status).
* `BatteryNotificationHelper` raises high-priority alerts to prevent sudden shutdown.
* Hooks in `NanoSpringService` (`onLowBattery`) allow applications to trigger graceful degradation or state persistence.
