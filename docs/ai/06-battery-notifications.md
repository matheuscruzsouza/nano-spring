# Gerenciamento de Bateria e Notificações no Nano-Spring

O Nano-Spring fornece suporte nativo para monitorar a integridade e o nível da bateria em dispositivos Android, além de disparar notificações ao usuário caso a bateria esteja acabando.

## 1. Modelo de Status (`BatteryStatus`)
O objeto `BatteryStatus` contém as informações consolidadas da bateria:
- `getLevel()`: nível bruto da bateria.
- `getPercentage()`: porcentagem calculada (0 a 100%).
- `isCharging()`: indica se está carregando.
- `isPluggedAc()` / `isPluggedUsb()` / `isPlugged()`: status de conexão à energia.
- `getTemperatureCelsius()`: temperatura em graus Celsius.
- `isLow()`: indica se o nível está abaixo do limite crítico e o aparelho não está carregando.

---

## 2. Injeção de Dependência (`BatteryService`)
Você pode injetar o serviço de bateria em qualquer `@RestController` ou `@Service`:

```java
package com.meuapp.controller;

import com.github.matheuscruzsouza.nanospring.annotation.Autowired;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.battery.BatteryService;
import com.github.matheuscruzsouza.nanospring.battery.BatteryStatus;

@RestController("/api/device")
public class DeviceBatteryController {

    @Autowired
    private BatteryService batteryService;

    @GetMethod("/battery")
    public BatteryStatus getBattery() {
        return batteryService.getCurrentStatus();
    }

    @GetMethod("/battery/alert")
    public boolean triggerAlertIfLow() {
        return batteryService.notifyUserIfBatteryLow();
    }
}
```

---

## 3. Utilitários Estáticos (`BatteryHelper` e `BatteryNotificationHelper`)

### Consultar status sem injeção
```java
BatteryStatus status = BatteryHelper.getBatteryStatus(context);
boolean isLow = BatteryHelper.isBatteryLow(context, 15); // threshold de 15%
```

### Disparar ou cancelar notificação para o usuário
```java
// Notificação padrão de bateria baixa:
BatteryNotificationHelper.sendLowBatteryNotification(context, status);

// Cancelar notificação quando o dispositivo for conectado à tomada:
BatteryNotificationHelper.cancelLowBatteryNotification(context);

// Notificação customizada:
BatteryNotificationHelper.sendNotification(
    context,
    "canal_customizado",
    "Nome do Canal",
    1003,
    "Aviso Importante",
    "Servidor pausando tarefas pesadas devido à bateria baixa",
    android.R.drawable.stat_sys_warning
);
```

---

## 4. Monitoramento Automático no `NanoSpringService`
No Foreground Service, o monitoramento pode ser ativado via propriedade ou sobrescrita de método:

```properties
# application.properties
nano.battery.monitor.enabled=true
nano.battery.threshold=15
```

Ou customizando o serviço:
```java
public class MyServerService extends NanoSpringService {

    @Override
    protected String getBasePackage() {
        return "com.meuapp";
    }

    @Override
    protected boolean isBatteryMonitorEnabled() {
        return true;
    }

    @Override
    protected int getLowBatteryThreshold() {
        return 20; // Alerta aos 20%
    }

    @Override
    protected void onLowBattery(BatteryStatus status) {
        super.onLowBattery(status); // Dispara notificação nativa
        // Executar ações adicionais, como suspender workers ou pausar sincronizações
    }
}
```
