package com.github.matheuscruzsouza.nanospring.battery;

import android.app.Notification;
import android.app.NotificationManager;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatteryServiceTest {

    @Test
    public void testBatteryServiceDefaultThreshold() {
        BatteryService service = new BatteryService();
        assertEquals(BatteryHelper.DEFAULT_LOW_BATTERY_THRESHOLD, service.getLowBatteryThreshold());

        service.setLowBatteryThreshold(20);
        assertEquals(20, service.getLowBatteryThreshold());
    }

    @Test
    public void testBatteryServiceWithContext() {
        Context context = Mockito.mock(Context.class);
        Intent intent = Mockito.mock(Intent.class);
        when(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(8);
        when(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100);
        when(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)).thenReturn(BatteryManager.BATTERY_STATUS_DISCHARGING);
        when(context.registerReceiver(isNull(), any(IntentFilter.class))).thenReturn(intent);

        NotificationManager manager = Mockito.mock(NotificationManager.class);
        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(manager);

        BatteryService service = new BatteryService(context, 10);
        assertEquals(context, service.getContext());

        BatteryStatus status = service.getCurrentStatus();
        assertNotNull(status);
        assertEquals(8, status.getPercentage());
        assertTrue(service.isLowBattery());

        boolean notified = service.notifyUserIfBatteryLow();
        assertTrue(notified);
        verify(manager).notify(eq(BatteryNotificationHelper.DEFAULT_BATTERY_NOTIFICATION_ID), nullable(Notification.class));
    }

    @Test
    public void testBatteryServiceNotLow() {
        Context context = Mockito.mock(Context.class);
        Intent intent = Mockito.mock(Intent.class);
        when(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)).thenReturn(80);
        when(intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)).thenReturn(100);
        when(intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)).thenReturn(BatteryManager.BATTERY_STATUS_DISCHARGING);
        when(context.registerReceiver(isNull(), any(IntentFilter.class))).thenReturn(intent);

        BatteryService service = new BatteryService(context);
        assertFalse(service.isLowBattery());
        assertFalse(service.notifyUserIfBatteryLow());
    }
}
