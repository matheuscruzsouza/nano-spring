package com.github.matheuscruzsouza.nanospring.discovery;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class MdnsHostResponderTest {

    @Test
    public void testFqdnNormalization() {
        MdnsHostResponder r1 = new MdnsHostResponder(null, "androidserver");
        assertEquals("androidserver.local", r1.getFullLocalHostname());

        MdnsHostResponder r2 = new MdnsHostResponder(null, "my-app.local");
        assertEquals("my-app.local", r2.getFullLocalHostname());

        MdnsHostResponder r3 = new MdnsHostResponder(null, null);
        assertEquals("nano-spring.local", r3.getFullLocalHostname());
    }

    @Test
    public void testBuildARecordResponse() throws IOException {
        InetAddress ip = InetAddress.getByName("192.168.1.150");
        byte[] response = MdnsHostResponder.buildARecordResponse("androidserver.local", ip, 120);

        assertNotNull(response);
        assertTrue(response.length > 12);

        // Flags: 0x8400 (QR=1, AA=1)
        assertEquals((byte) 0x84, response[2]);
        assertEquals((byte) 0x00, response[3]);

        // QDCOUNT = 0
        assertEquals((byte) 0x00, response[4]);
        assertEquals((byte) 0x00, response[5]);

        // ANCOUNT = 1
        assertEquals((byte) 0x00, response[6]);
        assertEquals((byte) 0x01, response[7]);

        // Check that the last 4 bytes contain the IP address
        int len = response.length;
        assertEquals((byte) 192, response[len - 4]);
        assertEquals((byte) 168, response[len - 3]);
        assertEquals((byte) 1, response[len - 2]);
        assertEquals((byte) 150, response[len - 1]);
    }

    @Test
    public void testParseDnsNameStandardLabels() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] label1 = "androidserver".getBytes(StandardCharsets.US_ASCII);
        byte[] label2 = "local".getBytes(StandardCharsets.US_ASCII);

        baos.write(label1.length);
        baos.write(label1);
        baos.write(label2.length);
        baos.write(label2);
        baos.write(0x00);

        byte[] raw = baos.toByteArray();
        MdnsHostResponder.DnsNameResult result = MdnsHostResponder.parseDnsName(raw, 0, raw.length);

        assertNotNull(result);
        assertEquals("androidserver.local", result.name);
        assertEquals(raw.length, result.nextOffset);
    }
}
