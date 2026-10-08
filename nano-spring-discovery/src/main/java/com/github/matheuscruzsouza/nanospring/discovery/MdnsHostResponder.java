package com.github.matheuscruzsouza.nanospring.discovery;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;

public class MdnsHostResponder {
    private static final String TAG = "MdnsHostResponder";
    public static final String MDNS_IPV4_GROUP = "224.0.0.251";
    public static final int MDNS_PORT = 5353;
    public static final int DEFAULT_TTL_SECONDS = 120;

    private final String hostname;
    private final String fullLocalHostname;
    private final Context context;

    private MulticastSocket socket;
    private WifiManager.MulticastLock multicastLock;
    private Thread listenerThread;
    private volatile boolean running = false;
    private InetAddress localIpAddress;

    public MdnsHostResponder(Context context, String hostname) {
        this.context = context;
        String cleanName = (hostname != null && !hostname.trim().isEmpty())
                ? hostname.trim().toLowerCase(Locale.ROOT)
                : "nano-spring";
        if (cleanName.endsWith(".local")) {
            this.hostname = cleanName.substring(0, cleanName.length() - 6);
            this.fullLocalHostname = cleanName;
        } else {
            this.hostname = cleanName;
            this.fullLocalHostname = cleanName + ".local";
        }
    }

    public synchronized void start() {
        if (running) return;

        acquireMulticastLock();

        localIpAddress = findLocalIpAddress();
        if (localIpAddress == null) {
            Log.w(TAG, "No suitable local IPv4 address found for mDNS host responder.");
            return;
        }

        try {
            socket = new MulticastSocket(MDNS_PORT);
            socket.setReuseAddress(true);
            socket.setTimeToLive(255);

            InetAddress group = InetAddress.getByName(MDNS_IPV4_GROUP);
            socket.joinGroup(group);

            running = true;

            // Send initial gratuitous announcement so local caches learn our host immediately
            sendAnnouncement(DEFAULT_TTL_SECONDS);

            listenerThread = new Thread(this::listenLoop, "NanoSpring-MdnsHostResponder");
            listenerThread.setDaemon(true);
            listenerThread.start();

            Log.i(TAG, "mDNS Host Responder started for " + fullLocalHostname + " -> " + localIpAddress.getHostAddress());
        } catch (Exception e) {
            Log.w(TAG, "Could not start mDNS Host Responder on port 5353: " + e.getMessage());
            stop();
        }
    }

    public synchronized void stop() {
        if (!running && socket == null) return;
        running = false;

        // Send goodbye packet (TTL = 0) to flush local caches
        try {
            if (socket != null && !socket.isClosed() && localIpAddress != null) {
                sendAnnouncement(0);
            }
        } catch (Exception ignored) {}

        if (socket != null) {
            try {
                InetAddress group = InetAddress.getByName(MDNS_IPV4_GROUP);
                socket.leaveGroup(group);
            } catch (Exception ignored) {}
            socket.close();
            socket = null;
        }

        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }

        releaseMulticastLock();
        Log.i(TAG, "mDNS Host Responder stopped.");
    }

    public boolean isRunning() {
        return running;
    }

    public String getFullLocalHostname() {
        return fullLocalHostname;
    }

    private void listenLoop() {
        byte[] buffer = new byte[1500];
        while (running && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                if (!running) break;

                handlePacket(packet);
            } catch (IOException e) {
                if (!running) break;
                Log.d(TAG, "Socket receive error: " + e.getMessage());
            } catch (Exception e) {
                Log.d(TAG, "Error processing mDNS packet: " + e.getMessage());
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        byte[] data = packet.getData();
        int length = packet.getLength();
        if (length < 12) return;

        // Check flags: queries have QR bit = 0 (top bit of byte 2 is 0)
        int flags = ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        boolean isQuery = (flags & 0x8000) == 0;
        if (!isQuery) return;

        int qdCount = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
        if (qdCount <= 0) return;

        int offset = 12; // Start of questions section
        for (int q = 0; q < qdCount; q++) {
            DnsNameResult nameResult = parseDnsName(data, offset, length);
            if (nameResult == null) break;

            offset = nameResult.nextOffset;
            if (offset + 4 > length) break;

            int qType = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
            int qClass = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
            offset += 4;

            // QTYPE 1 = A (IPv4), QTYPE 255 = ANY
            boolean isTypeAOrAny = (qType == 1 || qType == 255);
            boolean isClassIn = ((qClass & 0x7FFF) == 1); // Ignore unicast-response bit

            if (isTypeAOrAny && isClassIn && matchesHostname(nameResult.name)) {
                // Refresh local IP in case it changed
                InetAddress currentIp = findLocalIpAddress();
                if (currentIp != null) {
                    localIpAddress = currentIp;
                }
                sendAnswerPacket(localIpAddress, DEFAULT_TTL_SECONDS);
                break;
            }
        }
    }

    private boolean matchesHostname(String queryName) {
        if (queryName == null) return false;
        String lower = queryName.toLowerCase(Locale.ROOT);
        return lower.equals(fullLocalHostname) || lower.equals(hostname);
    }

    private void sendAnnouncement(int ttl) {
        if (localIpAddress == null) return;
        sendAnswerPacket(localIpAddress, ttl);
    }

    private void sendAnswerPacket(InetAddress ip, int ttl) {
        if (socket == null || socket.isClosed()) return;
        try {
            byte[] responseBytes = buildARecordResponse(fullLocalHostname, ip, ttl);
            DatagramPacket outPacket = new DatagramPacket(
                    responseBytes,
                    responseBytes.length,
                    InetAddress.getByName(MDNS_IPV4_GROUP),
                    MDNS_PORT
            );
            socket.send(outPacket);
        } catch (Exception e) {
            Log.d(TAG, "Error sending mDNS response: " + e.getMessage());
        }
    }

    public static byte[] buildARecordResponse(String fqdn, InetAddress ipAddress, int ttlSeconds) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(baos);

        // Header (12 bytes)
        out.writeShort(0x0000); // Transaction ID: 0 for mDNS responses
        out.writeShort(0x8400); // Flags: Standard response (0x8000) | Authoritative Answer (0x0400)
        out.writeShort(0);      // Questions: 0
        out.writeShort(1);      // Answer RRs: 1
        out.writeShort(0);      // Authority RRs: 0
        out.writeShort(0);      // Additional RRs: 0

        // Answer Section: Name (encoded labels)
        writeDnsLabels(out, fqdn);

        // TYPE 1 = A
        out.writeShort(0x0001);

        // CLASS: 0x8001 (IN class with Cache-Flush bit set for unique records)
        out.writeShort(0x8001);

        // TTL
        out.writeInt(ttlSeconds);

        // RDLENGTH: 4 bytes for IPv4
        byte[] ipBytes = ipAddress.getAddress();
        out.writeShort(ipBytes.length);

        // RDATA
        out.write(ipBytes);

        out.flush();
        return baos.toByteArray();
    }

    private static void writeDnsLabels(DataOutputStream out, String name) throws IOException {
        String clean = name.endsWith(".") ? name.substring(0, name.length() - 1) : name;
        String[] parts = clean.split("\\.");
        for (String part : parts) {
            if (!part.isEmpty()) {
                byte[] bytes = part.getBytes(StandardCharsets.US_ASCII);
                out.writeByte(bytes.length);
                out.write(bytes);
            }
        }
        out.writeByte(0x00); // Terminal null byte
    }

    public static class DnsNameResult {
        public final String name;
        public final int nextOffset;

        public DnsNameResult(String name, int nextOffset) {
            this.name = name;
            this.nextOffset = nextOffset;
        }
    }

    public static DnsNameResult parseDnsName(byte[] data, int startOffset, int maxLength) {
        StringBuilder sb = new StringBuilder();
        int offset = startOffset;
        boolean jumped = false;
        int nextOffsetAfterFirstJump = -1;
        int maxJumps = 10;
        int jumps = 0;

        while (offset < maxLength) {
            int len = data[offset] & 0xFF;
            if (len == 0) {
                offset++;
                break;
            }

            if ((len & 0xC0) == 0xC0) {
                // Compression pointer
                if (offset + 1 >= maxLength) return null;
                if (!jumped) {
                    nextOffsetAfterFirstJump = offset + 2;
                    jumped = true;
                }
                int pointerOffset = ((len & 0x3F) << 8) | (data[offset + 1] & 0xFF);
                offset = pointerOffset;
                jumps++;
                if (jumps > maxJumps) return null; // Prevent pointer loop
                continue;
            }

            // Normal label
            offset++;
            if (offset + len > maxLength) return null;
            if (sb.length() > 0) sb.append(".");
            sb.append(new String(data, offset, len, StandardCharsets.US_ASCII));
            offset += len;
        }

        int finalOffset = jumped ? nextOffsetAfterFirstJump : offset;
        return new DnsNameResult(sb.toString(), finalOffset);
    }

    public static InetAddress findLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface nif = interfaces.nextElement();
                    if (nif.isLoopback() || !nif.isUp()) continue;

                    Enumeration<InetAddress> addresses = nif.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (!addr.isLoopbackAddress() && addr instanceof Inet4Address && !addr.isLinkLocalAddress()) {
                            return addr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Error enumerating network interfaces: " + e.getMessage());
        }
        return null;
    }

    private void acquireMulticastLock() {
        if (context == null) return;
        try {
            WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("nano-spring-mdns");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();
                Log.d(TAG, "WifiManager.MulticastLock acquired.");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to acquire WifiManager.MulticastLock: " + e.getMessage());
        }
    }

    private void releaseMulticastLock() {
        if (multicastLock != null && multicastLock.isHeld()) {
            try {
                multicastLock.release();
                Log.d(TAG, "WifiManager.MulticastLock released.");
            } catch (Exception ignored) {}
            multicastLock = null;
        }
    }
}
