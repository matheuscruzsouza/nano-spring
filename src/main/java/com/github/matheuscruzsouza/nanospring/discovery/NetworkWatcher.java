package com.github.matheuscruzsouza.nanospring.discovery;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import java.net.InetAddress;

/**
 * Monitors network changes using Android ConnectivityManager and triggers
 * callbacks when the active IP address changes or network connectivity is lost/restored.
 */
public class NetworkWatcher {

    private static final String TAG = "NetworkWatcher";

    public interface NetworkChangeListener {
        void onNetworkChanged(String newIp);
        void onNetworkLost();
    }

    private final Context context;
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private String lastKnownIp;
    private boolean running = false;

    public NetworkWatcher(Context context) {
        this.context = context;
    }

    public synchronized void start(final NetworkChangeListener listener) {
        if (running || context == null) return;

        try {
            connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager == null) {
                Log.w(TAG, "ConnectivityManager is not available.");
                return;
            }

            InetAddress initial = MdnsHostResponder.findLocalIpAddress();
            lastKnownIp = initial != null ? initial.getHostAddress() : null;

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    checkIpChange("onAvailable", listener);
                }

                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    checkIpChange("onLinkPropertiesChanged", listener);
                }

                @Override
                public void onLost(Network network) {
                    Log.i(TAG, "Network connection lost.");
                    lastKnownIp = null;
                    if (listener != null) {
                        listener.onNetworkLost();
                    }
                }
            };

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                        .build();
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }

            running = true;
            Log.i(TAG, "NetworkWatcher started successfully (Current IP: " + lastKnownIp + ").");
        } catch (Exception e) {
            Log.w(TAG, "Failed to start NetworkWatcher: " + e.getMessage());
        }
    }

    private synchronized void checkIpChange(String trigger, NetworkChangeListener listener) {
        InetAddress current = MdnsHostResponder.findLocalIpAddress();
        String currentIp = current != null ? current.getHostAddress() : null;

        if (currentIp != null && !currentIp.equals(lastKnownIp)) {
            Log.i(TAG, "Network IP change detected via " + trigger + ": " + lastKnownIp + " -> " + currentIp);
            lastKnownIp = currentIp;
            if (listener != null) {
                listener.onNetworkChanged(currentIp);
            }
        }
    }

    public synchronized void stop() {
        if (!running) return;

        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister network callback: " + e.getMessage());
            } finally {
                networkCallback = null;
            }
        }

        running = false;
        Log.i(TAG, "NetworkWatcher stopped.");
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized String getLastKnownIp() {
        return lastKnownIp;
    }
}
