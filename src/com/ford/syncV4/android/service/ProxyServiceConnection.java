package com.ford.syncV4.android.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.ford.syncV4.android.logging.Log;
import com.ford.syncV4.proxy.RPCMessage;

public class ProxyServiceConnection implements ServiceConnection, AppLinkService {

    private final ProxyServiceListener proxyServiceListener;
    private AppLinkService proxyService;

    public interface ProxyServiceListener {
        void onProxyServiceStarted();
    }

    public ProxyServiceConnection(ProxyServiceListener proxyServiceListener) {
        this.proxyServiceListener = proxyServiceListener;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d("Service connected");
        proxyService = ((ProxyAppLinkService.ProxyBinder) iBinder).getService();
        if (proxyServiceListener != null) {
            proxyServiceListener.onProxyServiceStarted();
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        Log.d("Service disconnected");
    }

    @Override
    public void resetConnection() {
        proxyService.resetConnection();
    }

    @Override
    public void sendRPCRequest(RPCMessage message) {
        proxyService.sendRPCRequest(message);
    }

    @Override
    public void playPauseAudio() {
        proxyService.playPauseAudio();
    }
}
