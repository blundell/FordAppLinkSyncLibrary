package com.ford.syncV4.library.persistance;

import android.content.Context;
import android.content.SharedPreferences;

public class ConnectionPreferences {

    private final SharedPreferences preferences;

    public ConnectionPreferences(Context context) {
        preferences = context.getSharedPreferences(Const.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveIsAnMediaApp(boolean trueForMediaApp) {
        preferences.edit().putBoolean(Const.PREFS_KEY_ISMEDIAAPP, trueForMediaApp).commit();
    }

    public boolean isAnMediaApp() {
        return preferences.getBoolean(Const.PREFS_KEY_ISMEDIAAPP, Const.PREFS_DEFAULT_ISMEDIAAPP);
    }

    public void saveTransportType(int transportType) {
        preferences.edit().putInt(Const.Transport.PREFS_KEY_TRANSPORT_TYPE, transportType).commit();
    }

    public int getTransportType() {
        return preferences.getInt(Const.Transport.PREFS_KEY_TRANSPORT_TYPE, Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);
    }

    public void saveTransportAddress(String ipAddress, int tcpPort) {
        preferences.edit()
                .putString(Const.Transport.PREFS_KEY_TRANSPORT_IP, ipAddress)
                .putInt(Const.Transport.PREFS_KEY_TRANSPORT_PORT, tcpPort).commit();
    }

    public String getIpAddress() {
        return preferences.getString(Const.Transport.PREFS_KEY_TRANSPORT_IP, Const.Transport.PREFS_DEFAULT_TRANSPORT_IP);
    }

    public int getTcpPort() {
        return preferences.getInt(Const.Transport.PREFS_KEY_TRANSPORT_PORT, Const.Transport.PREFS_DEFAULT_TRANSPORT_PORT);
    }

    public void saveShouldAutoReconnect(boolean autoReconnect) {
        preferences.edit().putBoolean(Const.Transport.PREFS_KEY_TRANSPORT_RECONNECT, autoReconnect).commit();
    }

    public boolean shouldAutoReconnect() {
        return preferences.getBoolean(Const.Transport.PREFS_KEY_TRANSPORT_RECONNECT, Const.Transport.PREFS_DEFAULT_TRANSPORT_RECONNECT_DEFAULT);
    }
}
