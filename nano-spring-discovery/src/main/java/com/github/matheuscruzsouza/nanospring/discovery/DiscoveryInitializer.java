package com.github.matheuscruzsouza.nanospring.discovery;

import android.content.Context;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.server.ApplicationInitializer;
import com.github.matheuscruzsouza.nanospring.server.Environment;

import java.util.Map;

public class DiscoveryInitializer implements ApplicationInitializer {

    private NsdServiceManager nsdServiceManager;
    private NetworkWatcher networkWatcher;

    @Override
    public void initialize(Context context, Map<Class<?>, Object> services, String basePackage) {
        nsdServiceManager = new NsdServiceManager();
        services.put(NsdServiceManager.class, nsdServiceManager);
    }

    @Override
    public void onStart(Context context, int port) {
        boolean nsdEnabled = Boolean.parseBoolean(Environment.getProperty("nano.nsd.enabled", "false"));
        if (nsdEnabled && context != null) {
            String nsdName = Environment.getProperty("nano.nsd.name", NsdServiceManager.DEFAULT_SERVICE_NAME);
            String nsdType = Environment.getProperty("nano.nsd.type", NsdServiceManager.DEFAULT_SERVICE_TYPE);
            boolean hostResolution = Boolean.parseBoolean(Environment.getProperty("nano.nsd.host-resolution", "true"));
            nsdServiceManager.register(context, nsdName, nsdType, port, hostResolution);
        }

        boolean networkWatcherEnabled = Boolean.parseBoolean(Environment.getProperty("nano.network.watcher.enabled", "true"));
        if (networkWatcherEnabled && context != null) {
            this.networkWatcher = new NetworkWatcher(context);
            this.networkWatcher.start(new NetworkWatcher.NetworkChangeListener() {
                @Override
                public void onNetworkChanged(String newIp) {
                    Log.i("SERVER", "IP de rede alterado para " + newIp + ". Re-anunciando serviços mDNS...");
                    if (nsdServiceManager != null && nsdServiceManager.isRegistered()) {
                        nsdServiceManager.rebind();
                    }
                }

                @Override
                public void onNetworkLost() {
                    Log.w("SERVER", "Conexão de rede perdida.");
                }
            });
        }
    }

    @Override
    public void onStop() {
        if (networkWatcher != null) {
            networkWatcher.stop();
            networkWatcher = null;
        }
        if (nsdServiceManager != null) {
            nsdServiceManager.unregister();
        }
    }
}
