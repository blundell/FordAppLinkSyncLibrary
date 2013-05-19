package com.ford.syncV4.library.service;

import com.ford.syncV4.proxy.rpc.enums.ButtonName;

import java.io.Serializable;
import java.util.List;

public class ButtonNameParcel implements Serializable {
    public static final String EXTRA_BUTTON_NAME_PARCEL = "com.ford.syncV4.library.service.EXTRA_BUTTON_NAME_PARCEL";

    private final List<ButtonName> buttonNames;

    public ButtonNameParcel(List<ButtonName> buttonNames) {
        this.buttonNames = buttonNames;
    }

    public List<ButtonName> getButtonNames() {
        return buttonNames;
    }
}
