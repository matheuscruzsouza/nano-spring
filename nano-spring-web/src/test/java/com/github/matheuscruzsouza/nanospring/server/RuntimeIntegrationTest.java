package com.github.matheuscruzsouza.nanospring.server;

import android.content.Context;

import com.github.matheuscruzsouza.nanospring.annotation.Authenticated;
import com.github.matheuscruzsouza.nanospring.annotation.GetMethod;
import com.github.matheuscruzsouza.nanospring.annotation.RestController;
import com.github.matheuscruzsouza.nanospring.annotation.Service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RuntimeIntegrationTest {

    private Server server;
    private Context mockContext;

    @Service
    public static class HelloService {
        public String getGreeting() {
            return "Hello DI!";
        }
    }

    @RestController("/api")
    public static class TestController {
        private final HelloService helloService;

        public TestController(HelloService helloService) {
            this.helloService = helloService;
        }

        @GetMethod("/hello")
        public String hello() {
            return helloService.getGreeting();
        }
        
        @Authenticated
        @GetMethod("/secure")
        public String secure() {
            return "Secret Data";
        }
    }

    @Before
    public void setup() throws Exception {
        Environment.reset();
        Environment.setProperty("nano.actuator.secure", "true");
        Environment.setProperty("nano.actuator.enabled", "true");
        Environment.setProperty("nano.security.username", "testuser");
        Environment.setProperty("nano.security.password", "testpass");

        mockContext = Mockito.mock(Context.class);
        server = new Server(mockContext, 8089, "com.github.matheuscruzsouza.nanospring.server");
        
        // Manual registration for test since classpath scan might not work in unit test without dex
        server.registerSingleton(HelloService.class, new HelloService());
        server.createInstance(TestController.class);

        server.start(5000, false);
    }

    @After
    public void teardown() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testPublicEndpointAndDependencyInjection() throws Exception {
        URL url = new URL("http://localhost:8089/api/hello");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(200, conn.getResponseCode());
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = in.readLine();
        assertEquals("Hello DI!", response);
    }

    @Test
    public void testAuthenticatedEndpointRequiresAuth() throws Exception {
        URL url = new URL("http://localhost:8089/api/secure");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(401, conn.getResponseCode());
    }

    @Test
    public void testAuthenticatedEndpointWithValidAuth() throws Exception {
        URL url = new URL("http://localhost:8089/api/secure");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        String auth = "testuser:testpass";
        String encodedAuth = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        } else {
            // No unit test context, we'll manually encode or just use java Base64 for the test JVM
            encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
        }
        
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);

        assertEquals(200, conn.getResponseCode());
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String response = in.readLine();
        assertEquals("Secret Data", response);
    }

    @Test
    public void testActuatorIsSecuredByDefault() throws Exception {
        URL url = new URL("http://localhost:8089/actuator/health");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(401, conn.getResponseCode());
    }
}
