package com.ford.syncV4.library.service;

import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;

import java.io.Serializable;

public class CreateChoiceSetParcel implements Serializable {
    public static final String EXTRA_CREATE_CHOICE_SET_PARCEL = "com.ford.syncV4.library.service.EXTRA_CREATE_CHOICE_SET_PARCEL";

    private final boolean successful;

    public CreateChoiceSetParcel(CreateInteractionChoiceSetResponse response) {
        this.successful = response.getSuccess();
    }

    public boolean isSuccessful() {
        return successful;
    }
}
