# 🔍 Descoberta de Serviço mDNS & Acesso Direto (`.local`)

O **Nano-Spring** possui um subsistema duplo de descoberta local Zeroconf / Bonjour:
1. **DNS-SD (`_http._tcp.`):** Anúncio de serviço na rede local via API nativa `NsdManager` do Android.
2. **Resolução de Host mDNS (A-Record):** Responde a requisições de resolução de nome do tipo A/IPv4 via `MdnsHostResponder` embutido.

Isso permite que qualquer computador, celular ou tablet na mesma rede Wi-Fi acesse seu servidor diretamente pelo navegador (Chrome, Safari, Firefox, Edge) usando o domínio `.local`:

```text
http://meu-servidor.local:8080/
```

Sem precisar descobrir o IP do Android, sem configurar roteador e sem instalar nada no cliente!

---

## 🛠️ Permissões no `AndroidManifest.xml`

Para que o mDNS e o responder de multicast funcionem no Android, declare:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_MULTICAST_STATE" />
    ...
</manifest>
```

---

## ⚙️ Configuração via `application.properties`

Coloque em `assets/application.properties`:

```properties
# Habilita o anúncio de serviço na rede local (padrão: false)
nano.nsd.enabled=true

# Nome do host e serviço (acessível como http://meu-servidor-android.local:8080)
nano.nsd.name=meu-servidor-android

# Tipo do serviço DNS-SD (padrão: _http._tcp.)
nano.nsd.type=_http._tcp.

# Habilita o responder mDNS para resolução de Host no navegador (padrão: true)
nano.nsd.host-resolution=true
```

---

## 💻 Ativação Programática

Você também pode ativar e desativar o mDNS a qualquer momento pelo código:

```java
// Habilita com o nome "totem-loja" e resolução direta para navegador (true)
server.enableNsd("totem-loja", true);

// Desativa o anúncio e o responder
server.disableNsd();
```

---

## 🔬 Como Funciona por Baixo dos Panos

```
[Navegador Web no PC] 
         | 
         | 1. Query mDNS: "Quem é totem-loja.local?" (Porta UDP 5353)
         v
[Android: MdnsHostResponder] 
         |
         | 2. Responde Registro A com o IP atual do Wi-Fi (ex: 192.168.1.150)
         v
[Navegador conecta em http://192.168.1.150:8080/]
```

1. O `NsdServiceManager` gerencia o ciclo de vida junto ao `android.net.nsd.NsdManager` do sistema operacional.
2. O `MdnsHostResponder` abre um `MulticastSocket` na porta mDNS padrão (`5353`), vincula-se ao grupo multicast `224.0.0.251` e adquire um `WifiManager.MulticastLock` (liberado automaticamente ao parar).
3. Ao receber uma consulta de registro A para `<nome>.local`, o responder formata uma resposta binária DNS válida contendo o endereço IPv4 atual da interface Wi-Fi do Android.
4. Ao encerrar o servidor (`server.stop()`), todos os recursos e sockets são limpos imediatamente.

[⬅ Voltar para o Índice](../index.md)
