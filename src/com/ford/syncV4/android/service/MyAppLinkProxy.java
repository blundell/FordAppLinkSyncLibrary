package com.ford.syncV4.android.service;

import com.ford.syncV4.proxy.SyncProxyALM;

public interface MyAppLinkProxy {
    SyncProxyALM getSyncProxyInstance();

    void playPauseAnnoyingRepetitiveAudio();

    void reset();
}
