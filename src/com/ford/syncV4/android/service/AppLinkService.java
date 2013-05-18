package com.ford.syncV4.android.service;

import com.ford.syncV4.proxy.RPCMessage;

public interface AppLinkService {
    void sendRPCRequest(RPCMessage message);

    void playPauseAudio();

    void resetConnection();
}
