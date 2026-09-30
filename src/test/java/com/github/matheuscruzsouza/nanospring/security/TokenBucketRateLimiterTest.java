package com.github.matheuscruzsouza.nanospring.security;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TokenBucketRateLimiterTest {

    @Test
    public void testRouteRateLimitExceeded() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(false, 50, 100);
        String ip = "192.168.1.100";
        String routeKey = "UserController#getUsers";

        // Limit 3 requests in 1 second
        assertTrue("Request 1 should be allowed", limiter.checkRoute(ip, routeKey, 3, 1));
        assertTrue("Request 2 should be allowed", limiter.checkRoute(ip, routeKey, 3, 1));
        assertTrue("Request 3 should be allowed", limiter.checkRoute(ip, routeKey, 3, 1));

        // 4th request must be rejected
        assertFalse("Request 4 should be rejected", limiter.checkRoute(ip, routeKey, 3, 1));
    }

    @Test
    public void testDifferentIpsHaveIndependentBuckets() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(false, 50, 100);
        String routeKey = "OrderController#create";

        assertTrue(limiter.checkRoute("10.0.0.1", routeKey, 1, 1));
        assertFalse(limiter.checkRoute("10.0.0.1", routeKey, 1, 1));

        // Different IP should still have its own quota
        assertTrue(limiter.checkRoute("10.0.0.2", routeKey, 1, 1));
    }

    @Test
    public void testGlobalRateLimiterEnabled() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(true, 2, 2);
        String ip = "127.0.0.1";

        assertTrue(limiter.checkGlobal(ip));
        assertTrue(limiter.checkGlobal(ip));
        assertFalse("3rd global request should exceed burst capacity of 2", limiter.checkGlobal(ip));
    }

    @Test
    public void testGlobalRateLimiterDisabledAllowsAll() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(false, 1, 1);
        String ip = "127.0.0.1";

        for (int i = 0; i < 10; i++) {
            assertTrue("Disabled global rate limiter should allow all requests", limiter.checkGlobal(ip));
        }
    }
}
