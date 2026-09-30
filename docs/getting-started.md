# 🚀 Guia de Início e Instalação

Este guia detalha os requisitos, permissões e métodos de instalação do **Nano-Spring** no seu aplicativo Android.

---

## 📱 Requisitos do Sistema

* **Android Mínimo:** Android 7.1.1 (API 25 - Nougat)
* **Android Alvo / Compilação:** Android 14+ (API 34 / 36)
* **Java:** Java 11 ou superior
* **Consumo de Memória:** ~15 MB a 40 MB de RAM

---

## 📦 Métodos de Instalação

### Opção 1: Via GitHub Packages (Dependência Remota)

O GitHub Packages exige autenticação com token (Personal Access Token) para baixar pacotes, mesmo que o repositório seja público.

#### 1. Configure suas credenciais do GitHub
No arquivo `~/.gradle/gradle.properties` da sua máquina (ou no `gradle.properties` da raiz do projeto):
```properties
gpr.user=SEU_USUARIO_GITHUB
gpr.key=SEU_GITHUB_PERSONAL_ACCESS_TOKEN
```
*(O token precisa apenas da permissão `read:packages`).*

#### 2. Declare o repositório no `settings.gradle`:
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

#### 3. Adicione a dependência no `build.gradle` do seu app:
```groovy
dependencies {
    implementation 'com.github.matheuscruzsouza:nano-spring:1.6.0'
}
```

---

### Opção 2: Como Módulo Local (Subprojeto)

Se você clonou o código-fonte do `nano-spring` dentro do mesmo projeto Android:

1. No `settings.gradle`:
```groovy
include ':app', ':nano-spring'
```

2. No `app/build.gradle`:
```groovy
dependencies {
    implementation project(':nano-spring')
}
```

---

## 🛡️ Permissões do Android (`AndroidManifest.xml`)

No seu arquivo `app/src/main/AndroidManifest.xml`, certifique-se de declarar as permissões de rede:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissões de rede obrigatórias para abrir a porta do servidor -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <!-- Necessário apenas se for usar Descoberta mDNS / Zeroconf -->
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />

    <application
        android:usesCleartextTraffic="true"
        ...>
        
        <!-- Serviço do Servidor em Background -->
        <service
            android:name=".service.AppServerService"
            android:enabled="true"
            android:exported="false" />

    </application>
</manifest>
```

> 💡 **Nota sobre `android:usesCleartextTraffic="true"`:** A partir do Android 9 (API 28), o Android bloqueia requisições HTTP sem TLS por padrão em webviews locais e conexões de loopback. Habilitar esta flag permite testes em `http://localhost`.

---

## 🔋 Executando como Foreground Service

Para garantir que o Android não finalize o processo do servidor quando a tela for desligada ou o app for para segundo plano:

```java
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.github.matheuscruzsouza.nanospring.server.Server;

public class AppServerService extends Service {
    private static final String CHANNEL_ID = "NanoSpringServerChannel";
    private Server server;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Nano-Spring Server")
                .setContentText("Servidor HTTP ativo e aguardando conexões")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (server == null) {
            // Sobe o servidor na porta 8080 varrendo classes em "com.meuapp"
            server = new Server(this, 8080, "com.meuapp");
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (server != null) {
            server.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nano-Spring Server",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
```

[⬅ Voltar para o Início](index.md)
