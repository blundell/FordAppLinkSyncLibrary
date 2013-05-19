package com.ford.syncV4.library.service;

import com.ford.syncV4.proxy.rpc.enums.ButtonName;

import java.io.Serializable;

public class ButtonPressedParcel implements Serializable {
    public static final String EXTRA_BUTTON_PRESSED_PARCEL = "com.ford.syncV4.library.service.EXTRA_BUTTON_PRESSED_PARCEL";

    private final ButtonName buttonName;

    public ButtonPressedParcel(ButtonName buttonName) {
        this.buttonName = buttonName;
    }

    public ButtonName getButtonName() {
        return buttonName;
    }
}
