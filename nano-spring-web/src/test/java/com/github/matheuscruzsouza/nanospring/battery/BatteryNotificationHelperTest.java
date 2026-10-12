package com.github.matheuscruzsouza.nanospring.battery;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BatteryNotificationHelperTest {

    @Test
    public void testSendLowBatteryNotificationNullSafe() {
        BatteryNotificationHelper.sendLowBatteryNotification(null, null);
        BatteryNotificationHelper.cancelLowBatteryNotification(null);
    }

    @Test
    public void testSendLowBatteryNotificationWithMock() {
        Context context = Mockito.mock(Context.class);
        NotificationManager manager = Mockito.mock(NotificationManager.class);
        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(manager);

        BatteryStatus status = new BatteryStatus(
                10, 100, 10, false, false, false, 30.0,
                1, true, System.currentTimeMillis()
        );

        BatteryNotificationHelper.sendLowBatteryNotification(context, status);
        verify(manager).notify(eq(BatteryNotificationHelper.DEFAULT_BATTERY_NOTIFICATION_ID), nullable(Notification.class));
    }

    @Test
    public void testCancelNotification() {
        Context context = Mockito.mock(Context.class);
        NotificationManager manager = Mockito.mock(NotificationManager.class);
        when(context.getSystemService(Context.NOTIFICATION_SERVICE)).thenReturn(manager);

        BatteryNotificationHelper.cancelLowBatteryNotification(context);
        verify(manager).cancel(eq(BatteryNotificationHelper.DEFAULT_BATTERY_NOTIFICATION_ID));
    }
}
