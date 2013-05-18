package com.ford.syncV4.android.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.ford.syncV4.android.logging.Log;
import com.ford.syncV4.proxy.SyncProxyALM;

public class ProxyServiceConnection implements ServiceConnection, MyAppLinkProxy {

    private final ProxyServiceListener proxyServiceListener;
    private ProxyService proxyService;

    public interface ProxyServiceListener {
        void onProxyServiceStarted();
    }

    public ProxyServiceConnection(ProxyServiceListener proxyServiceListener) {
        this.proxyServiceListener = proxyServiceListener;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d("Service connected");
        proxyService = ((ProxyService.ProxyBinder) iBinder).getService();
        if (proxyServiceListener != null) {
            proxyServiceListener.onProxyServiceStarted();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d("Service disconnected");
    }

    @Override
    public SyncProxyALM getSyncProxyInstance() {
        return proxyService.getSyncProxyInstance();
    }

    @Override
    public void playPauseAnnoyingRepetitiveAudio() {
        proxyService.playPauseAnnoyingRepetitiveAudio();
    }

    @Override
    public void reset() {
        proxyService.reset();
    }
}
