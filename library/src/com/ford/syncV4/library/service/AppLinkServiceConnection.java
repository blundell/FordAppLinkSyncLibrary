package com.ford.syncV4.library.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.ford.syncV4.library.logging.Log;
import com.ford.syncV4.proxy.RPCMessage;

public class AppLinkServiceConnection implements ServiceConnection, AppLinkService {

    private final ServiceListener serviceListener;
    private AppLinkService proxyService;

    public interface ServiceListener {
        void onProxyServiceStarted();
    }

    public AppLinkServiceConnection(ServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d("Service connected");
        proxyService = ((ProxyAppLinkService.ProxyBinder) iBinder).getService();
        if (serviceListener != null) {
            serviceListener.onProxyServiceStarted();
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
