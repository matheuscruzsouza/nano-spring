package com.github.matheuscruzsouza.nanospring.battery;

import android.os.BatteryManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryStatusTest {

    @Test
    public void testBatteryStatusGetters() {
        long now = System.currentTimeMillis();
        BatteryStatus status = new BatteryStatus(
                80, 100, 80, true, true, false, 32.5,
                BatteryManager.BATTERY_HEALTH_GOOD, false, now
        );

        assertEquals(80, status.getLevel());
        assertEquals(100, status.getScale());
        assertEquals(80, status.getPercentage());
        assertTrue(status.isCharging());
        assertTrue(status.isPluggedAc());
        assertFalse(status.isPluggedUsb());
        assertTrue(status.isPlugged());
        assertEquals(32.5, status.getTemperatureCelsius(), 0.001);
        assertEquals(BatteryManager.BATTERY_HEALTH_GOOD, status.getHealth());
        assertFalse(status.isLow());
        assertEquals(now, status.getTimestamp());
        assertTrue(status.toString().contains("80%"));
    }

    @Test
    public void testLowBatteryStatus() {
        BatteryStatus lowStatus = new BatteryStatus(
                10, 100, 10, false, false, false, 28.0,
                BatteryManager.BATTERY_HEALTH_GOOD, true, System.currentTimeMillis()
        );

        assertFalse(lowStatus.isCharging());
        assertFalse(lowStatus.isPlugged());
        assertTrue(lowStatus.isLow());
    }
}
