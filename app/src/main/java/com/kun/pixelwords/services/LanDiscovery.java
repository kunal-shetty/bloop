package com.kun.pixelwords.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Finds rooms on the same Wi-Fi. A host announces itself with Android's network service discovery and
 * other phones list what they hear, so nobody types an address. Listeners run on the main thread.
 */
public final class LanDiscovery {

    public static final class Room {
        public final String name;
        public final InetAddress host;
        public final int port;

        Room(String name, InetAddress host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }
    }

    private static final String SERVICE_TYPE = "_bloop._tcp.";
    private static final String TAG = "LanDiscovery";

    private final NsdManager nsd;
    private final WifiManager.MulticastLock lock;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Map<String, Room> rooms = new LinkedHashMap<>();
    private final Deque<NsdServiceInfo> toResolve = new ArrayDeque<>();
    private boolean resolving;
    private Consumer<List<Room>> onRooms;
    private NsdManager.RegistrationListener registration;
    private NsdManager.DiscoveryListener discovery;

    public LanDiscovery(Context context) {
        nsd = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        lock = wifi.createMulticastLock("bloop-discovery");
        lock.setReferenceCounted(false);
    }

    /** Races are Wi-Fi only for now, so mobile data on its own doesn't count. */
    public static boolean onWifi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = cm.getActiveNetwork();
        NetworkCapabilities caps = network == null ? null : cm.getNetworkCapabilities(network);
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    public void announce(String roomName, int port) {
        NsdServiceInfo info = new NsdServiceInfo();
        info.setServiceName(roomName);
        info.setServiceType(SERVICE_TYPE);
        info.setPort(port);
        registration = new NsdManager.RegistrationListener() {
            @Override public void onServiceRegistered(NsdServiceInfo s) {}
            @Override public void onRegistrationFailed(NsdServiceInfo s, int code) {
                Log.w(TAG, "announce failed " + code);
            }
            @Override public void onServiceUnregistered(NsdServiceInfo s) {}
            @Override public void onUnregistrationFailed(NsdServiceInfo s, int code) {}
        };
        nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, registration);
    }

    /** Stops telling people about the room, once it's full of the right players. */
    public void stopAnnouncing() {
        if (registration == null) return;
        try {
            nsd.unregisterService(registration);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "already stopped announcing", e);
        }
        registration = null;
    }

    public void search(Consumer<List<Room>> listener) {
        onRooms = listener;
        lock.acquire();
        discovery = new NsdManager.DiscoveryListener() {
            @Override public void onStartDiscoveryFailed(String type, int code) {
                Log.w(TAG, "search failed " + code);
            }
            @Override public void onStopDiscoveryFailed(String type, int code) {}
            @Override public void onDiscoveryStarted(String type) {}
            @Override public void onDiscoveryStopped(String type) {}

            @Override public void onServiceFound(NsdServiceInfo s) {
                main.post(() -> {
                    toResolve.add(s);
                    resolveNext();
                });
            }

            @Override public void onServiceLost(NsdServiceInfo s) {
                main.post(() -> {
                    rooms.remove(s.getServiceName());
                    publish();
                });
            }
        };
        nsd.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discovery);
    }

    /** Android allows one lookup at a time, so found rooms wait their turn. */
    @SuppressWarnings("deprecation")
    private void resolveNext() {
        if (resolving || toResolve.isEmpty()) return;
        resolving = true;
        nsd.resolveService(toResolve.poll(), new NsdManager.ResolveListener() {
            @Override public void onResolveFailed(NsdServiceInfo s, int code) {
                Log.w(TAG, "lookup failed " + code);
                main.post(() -> {
                    resolving = false;
                    resolveNext();
                });
            }

            @Override public void onServiceResolved(NsdServiceInfo s) {
                main.post(() -> {
                    resolving = false;
                    rooms.put(s.getServiceName(), new Room(s.getServiceName(), s.getHost(), s.getPort()));
                    publish();
                    resolveNext();
                });
            }
        });
    }

    private void publish() {
        if (onRooms != null) onRooms.accept(new ArrayList<>(rooms.values()));
    }

    public void stop() {
        stopAnnouncing();
        onRooms = null;
        if (discovery != null) {
            try {
                nsd.stopServiceDiscovery(discovery);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "search already stopped", e);
            }
            discovery = null;
        }
        if (lock.isHeld()) lock.release();
    }
}
