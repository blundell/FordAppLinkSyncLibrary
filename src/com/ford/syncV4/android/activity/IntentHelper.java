package com.ford.syncV4.android.activity;

import java.util.Hashtable;
import java.util.Map;

public class IntentHelper {
	private static IntentHelper _instance;
	private Map<String, Object> _map;

	private IntentHelper() {
		_map = new Hashtable<String, Object>();
	}

	private static IntentHelper getInstance() {
		if (_instance == null) {
			_instance = new IntentHelper();
		}
		return _instance;
	}

	public static void addObjectForKey(Object obj, String key) {
		getInstance()._map.put(key, obj);
	}

	public static Object getObjectForKey(String key) {
		return getInstance()._map.get(key);
	}

	public static void removeObjectForKey(String key) {
		getInstance()._map.remove(key);
	}
}
