package com.ford.syncV4.library.service;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class ProxyServiceAction {
    public static final String ACTION_BUTTONS_SUBSCRIBED = "com.ford.syncV4.library.ACTION_BUTTONS_SUBSCRIBED";
    public static final String ACTION_BUTTON_PRESSED = "com.ford.syncV4.library.ACTION_BUTTON_PRESSED";
    public static final String ACTION_PROXY_CLOSED = "com.ford.syncV4.library.ACTION_PROXY_CLOSED";
    public static final String ACTION_CREATE_INTERACTION_CHOICE_RESPONSE = "com.ford.syncV4.library.ACTION_CREATE_INTERACTION_CHOICE_RESPONSE";
    public static final String ACTION_TEST_CUSTOM_COMMAND = "com.ford.syncV4.library.ACTION_TEST_CUSTOM_COMMAND";
    public static final String ACTION_REQ_LOCK_SCREEN = "com.ford.syncV4.library.ACTION_REQ_LOCK_SCREEN";
    public static final String ACTION_REQ_UNLOCK_SCREEN = "com.ford.syncV4.library.ACTION_REQ_UNLOCK_SCREEN";

    public static final IntentFilter FILTER_PROXY_SERVICE = new IntentFilter(ACTION_BUTTONS_SUBSCRIBED);

    static {
        FILTER_PROXY_SERVICE.addAction(ACTION_BUTTON_PRESSED);
        FILTER_PROXY_SERVICE.addAction(ACTION_PROXY_CLOSED);
        FILTER_PROXY_SERVICE.addAction(ACTION_CREATE_INTERACTION_CHOICE_RESPONSE);
        FILTER_PROXY_SERVICE.addAction(ACTION_TEST_CUSTOM_COMMAND);
        FILTER_PROXY_SERVICE.addAction(ACTION_REQ_LOCK_SCREEN);
        FILTER_PROXY_SERVICE.addAction(ACTION_REQ_UNLOCK_SCREEN);
    }

    public static void broadcastButtonsSubscribed(Context context, ButtonNameParcel buttonNameParcel) {
        Intent intent = new Intent(ACTION_BUTTONS_SUBSCRIBED);
        intent.putExtra(ButtonNameParcel.EXTRA_BUTTON_NAME_PARCEL, buttonNameParcel);
        context.sendBroadcast(intent);
    }

    public static void broadcastButtonPressed(Context context, ButtonPressedParcel buttonPressedParcel) {
        Intent intent = new Intent(ACTION_BUTTON_PRESSED);
        intent.putExtra(ButtonPressedParcel.EXTRA_BUTTON_PRESSED_PARCEL, buttonPressedParcel);
        context.sendBroadcast(intent);
    }

    public static void broadcastTestCustomCommand(Context context) {
        Intent intent = new Intent(ACTION_TEST_CUSTOM_COMMAND);
        context.sendBroadcast(intent);
    }

    public static void broadcastProxyClosed(Context context) {
        Intent intent = new Intent(ACTION_PROXY_CLOSED);
        context.sendBroadcast(intent);
    }

    public static void broadcastCreateInteractionChoiceSetResponded(Context context, CreateChoiceSetParcel createChoiceSetParcel) {
        Intent intent = new Intent(ACTION_CREATE_INTERACTION_CHOICE_RESPONSE);
        intent.putExtra(CreateChoiceSetParcel.EXTRA_CREATE_CHOICE_SET_PARCEL, createChoiceSetParcel);
        context.sendBroadcast(intent);
    }

    public static void broadcastScreenLockRequest(Context context) {
        Intent intent = new Intent(ACTION_REQ_LOCK_SCREEN);
        context.sendBroadcast(intent);

    }

    public static void broadcastScreenUnlockRequest(Context context) {
        Intent intent = new Intent(ACTION_REQ_UNLOCK_SCREEN);
        context.sendBroadcast(intent);
    }
}
