package com.ford.syncV4.library.service;

import com.ford.syncV4.proxy.RPCMessage;

public interface AppLinkService {
    void sendRPCRequest(RPCMessage message);

    void resetConnection();
}
