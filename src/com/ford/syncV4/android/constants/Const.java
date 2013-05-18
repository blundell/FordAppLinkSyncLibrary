package com.ford.syncV4.android.constants;

/** Stores application-wide constants. */
public class Const {
	// Shared preference name for properties
	public static final String PREFS_NAME = "AppLinkTesterPrefs";

	// Properties
	public static final String PREFS_KEY_APPNAME = "appName";
	public static final String PREFS_KEY_ISMEDIAAPP = "isMediaApp";

	// Default values
	public static final String PREFS_DEFAULT_APPNAME = "AppLinkTester";
	public static final boolean PREFS_DEFAULT_ISMEDIAAPP = true;

	// Transport properties
	public static final class Transport {
		// Properties
		public static final String PREFS_KEY_TRANSPORT_TYPE = "TransportType";
		public static final String PREFS_KEY_TRANSPORT_PORT = "TCPPort";
		public static final String PREFS_KEY_TRANSPORT_IP = "IPAddress";
		public static final String PREFS_KEY_TRANSPORT_RECONNECT = "AutoReconnect";

		public static final String TCP = "WiFi";
		public static final String BLUETOOTH = "Bluetooth";
		public static final int KEY_TCP = 1;
		public static final int KEY_BLUETOOTH = 2;

		public static final int PREFS_DEFAULT_TRANSPORT_TYPE = KEY_BLUETOOTH;
		public static final int PREFS_DEFAULT_TRANSPORT_PORT = 50007;
		public static final String PREFS_DEFAULT_TRANSPORT_IP = "10.0.2.2";
		public static final boolean PREFS_DEFAULT_TRANSPORT_RECONNECT_DEFAULT = true;
	}
}
