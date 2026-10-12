package com.github.matheuscruzsouza.nanospring.battery;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

public class BatteryHelperTest {

    @Test
    public void testGetBatteryStatusWithNullContext() {
        BatteryStatus status = BatteryHelper.getBatteryStatus(null);
        assertNotNull(status);
        assertEquals(-1, status.getLevel());
        assertEquals(-1, status.getPercentage());
        assertFalse(status.isCharging());
        assertFalse(status.isLow());
    }

    @Test
    public void testFromIntentNull() {
        BatteryStatus status = BatteryHelper.fromIntent(null, 15);
        assertNotNull(status);
        assertEquals(-1, status.getLevel());
        assertFalse(status.isLow());
    }

    @Test
    public void testFromIntentLowBatteryNotCharging() {
        Intent intent = Mockito.mock(Intent.class);
        when(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(10);
        when(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100);
        when(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)).thenReturn(BatteryManager.BATTERY_STATUS_DISCHARGING);
        when(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)).thenReturn(0);
        when(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)).thenReturn(300);
        when(intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN))
                .thenReturn(BatteryManager.BATTERY_HEALTH_GOOD);

        BatteryStatus status = BatteryHelper.fromIntent(intent, 15);
        assertEquals(10, status.getLevel());
        assertEquals(100, status.getScale());
        assertEquals(10, status.getPercentage());
        assertFalse(status.isCharging());
        assertFalse(status.isPlugged());
        assertEquals(30.0, status.getTemperatureCelsius(), 0.001);
        assertTrue(status.isLow());
    }

    @Test
    public void testFromIntentLowBatteryWhileCharging() {
        Intent intent = Mockito.mock(Intent.class);
        when(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(10);
        when(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100);
        when(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)).thenReturn(BatteryManager.BATTERY_STATUS_CHARGING);
        when(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)).thenReturn(BatteryManager.BATTERY_PLUGGED_AC);
        when(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)).thenReturn(320);

        BatteryStatus status = BatteryHelper.fromIntent(intent, 15);
        assertEquals(10, status.getPercentage());
        assertTrue(status.isCharging());
        assertTrue(status.isPluggedAc());
        assertFalse(status.isLow());
    }

    @Test
    public void testGetBatteryStatusWithMockContext() {
        Context context = Mockito.mock(Context.class);
        Intent intent = Mockito.mock(Intent.class);
        when(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(50);
        when(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100);
        when(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)).thenReturn(BatteryManager.BATTERY_STATUS_NOT_CHARGING);
        when(context.registerReceiver(isNull(), any(IntentFilter.class))).thenReturn(intent);

        BatteryStatus status = BatteryHelper.getBatteryStatus(context);
        assertEquals(50, status.getPercentage());
        assertFalse(status.isCharging());
        assertFalse(status.isLow());
        assertFalse(BatteryHelper.isBatteryLow(context, 15));
    }
}
