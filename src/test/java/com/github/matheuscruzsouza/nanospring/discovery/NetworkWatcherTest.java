package com.github.matheuscruzsouza.nanospring.discovery;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class NetworkWatcherTest {

    @Test
    public void testNetworkWatcherInitialState() {
        NetworkWatcher watcher = new NetworkWatcher(null);
        assertFalse(watcher.isRunning());
        assertNull(watcher.getLastKnownIp());
    }

    @Test
    public void testNetworkWatcherStartWithNullContext() {
        NetworkWatcher watcher = new NetworkWatcher(null);
        final AtomicBoolean changed = new AtomicBoolean(false);

        watcher.start(new NetworkWatcher.NetworkChangeListener() {
            @Override
            public void onNetworkChanged(String newIp) {
                changed.set(true);
            }

            @Override
            public void onNetworkLost() {
            }
        });

        assertFalse(watcher.isRunning());
        assertFalse(changed.get());
        watcher.stop();
        assertFalse(watcher.isRunning());
    }
}
