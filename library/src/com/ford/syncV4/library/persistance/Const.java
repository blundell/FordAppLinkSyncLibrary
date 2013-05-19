package com.ford.syncV4.library.persistance;

/**
 * Stores application-wide constants.
 */
public class Const {
    // Shared preference name for properties
    static final String PREFS_NAME = "AppLinkTesterPrefs";

    // Properties
    static final String PREFS_KEY_ISMEDIAAPP = "isMediaApp";

    // Default values
    static final boolean PREFS_DEFAULT_ISMEDIAAPP = true;

    // Transport properties
    public static final class Transport {
        // Properties
        static final String PREFS_KEY_TRANSPORT_TYPE = "TransportType";
        static final String PREFS_KEY_TRANSPORT_PORT = "TCPPort";
        static final String PREFS_KEY_TRANSPORT_IP = "IPAddress";
        static final String PREFS_KEY_TRANSPORT_RECONNECT = "AutoReconnect";

        public static final String TCP = "WiFi";
        public static final String BLUETOOTH = "Bluetooth";
        public static final int KEY_TCP = 1;
        public static final int KEY_BLUETOOTH = 2;

        static final int PREFS_DEFAULT_TRANSPORT_TYPE = KEY_TCP;
        static final String PREFS_DEFAULT_TRANSPORT_IP = "10.0.2.2";
        static final int PREFS_DEFAULT_TRANSPORT_PORT = 50007;
        static final boolean PREFS_DEFAULT_TRANSPORT_RECONNECT_DEFAULT = true;
    }
}
