package com.github.matheuscruzsouza.nanospring.discovery;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;

public class NsdServiceManager {
    private static final String TAG = "NsdServiceManager";
    public static final String DEFAULT_SERVICE_TYPE = "_http._tcp.";
    public static final String DEFAULT_SERVICE_NAME = "nano-spring";

    private NsdManager nsdManager;
    private NsdManager.RegistrationListener registrationListener;
    private boolean registered = false;
    private String registeredServiceName;
    private MdnsHostResponder hostResponder;
    private Context lastContext;
    private String lastServiceName;
    private String lastServiceType;
    private int lastPort;
    private boolean lastEnableHostResolution;

    public synchronized void register(Context context, String serviceName, String serviceType, int port) {
        register(context, serviceName, serviceType, port, true);
    }

    public synchronized void register(Context context, String serviceName, String serviceType, int port, boolean enableHostResolution) {
        this.lastContext = context;
        this.lastServiceName = serviceName;
        this.lastServiceType = serviceType;
        this.lastPort = port;
        this.lastEnableHostResolution = enableHostResolution;
        if (context == null) {
            Log.w(TAG, "Context is null, cannot register NSD service.");
            return;
        }

        if (registered) {
            unregister();
        }

        try {
            this.nsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
            if (this.nsdManager == null) {
                Log.w(TAG, "NsdManager service not available on this device.");
                return;
            }

            final String finalServiceName = (serviceName != null && !serviceName.trim().isEmpty())
                    ? serviceName.trim()
                    : DEFAULT_SERVICE_NAME;
            final String finalServiceType = (serviceType != null && !serviceType.trim().isEmpty())
                    ? serviceType.trim()
                    : DEFAULT_SERVICE_TYPE;

            NsdServiceInfo serviceInfo = new NsdServiceInfo();
            serviceInfo.setServiceName(finalServiceName);
            serviceInfo.setServiceType(finalServiceType);
            serviceInfo.setPort(port);

            this.registrationListener = new NsdManager.RegistrationListener() {
                @Override
                public void onServiceRegistered(NsdServiceInfo nsdServiceInfo) {
                    registered = true;
                    registeredServiceName = nsdServiceInfo.getServiceName();
                    Log.i(TAG, "Service registered on mDNS/DNS-SD: " + registeredServiceName + " (Port: " + port + ")");
                }

                @Override
                public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    registered = false;
                    Log.e(TAG, "mDNS/DNS-SD registration failed for " + finalServiceName + ". Error code: " + errorCode);
                }

                @Override
                public void onServiceUnregistered(NsdServiceInfo arg0) {
                    registered = false;
                    Log.i(TAG, "Service unregistered from mDNS/DNS-SD.");
                }

                @Override
                public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    Log.e(TAG, "mDNS/DNS-SD unregistration failed. Error code: " + errorCode);
                }
            };

            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, this.registrationListener);

            if (enableHostResolution) {
                try {
                    hostResponder = new MdnsHostResponder(context, finalServiceName);
                    hostResponder.start();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to start mDNS host responder: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering NSD service", e);
        }
    }

    public synchronized void unregister() {
        if (hostResponder != null) {
            try {
                hostResponder.stop();
            } catch (Exception e) {
                Log.w(TAG, "Failed to stop host responder: " + e.getMessage());
            } finally {
                hostResponder = null;
            }
        }

        if (nsdManager != null && registrationListener != null) {
            try {
                nsdManager.unregisterService(registrationListener);
            } catch (Exception e) {
                Log.w(TAG, "Failed to unregister NSD service: " + e.getMessage());
            } finally {
                registrationListener = null;
                registered = false;
                registeredServiceName = null;
            }
        }
    }

    public synchronized boolean isRegistered() {
        return registered;
    }

    public synchronized String getRegisteredServiceName() {
        return registeredServiceName;
    }

    public synchronized MdnsHostResponder getHostResponder() {
        return hostResponder;
    }

    public synchronized void rebind() {
        if (lastContext != null && lastPort > 0) {
            Log.i(TAG, "Rebinding mDNS service registration on port " + lastPort);
            register(lastContext, lastServiceName, lastServiceType, lastPort, lastEnableHostResolution);
        }
    }
}
