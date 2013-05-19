package com.ford.syncV4.library;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.ford.syncV4.library.service.AppLinkService;
import com.ford.syncV4.library.service.AppLinkServiceConnection;
import com.ford.syncV4.library.service.ProxyAppLinkService;

public abstract class AppLinkActivity extends FragmentActivity implements AppLink, AppLinkServiceConnection.ServiceListener {

    private AppLinkServiceConnection appLinkServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startAppLinkService(this);
    }

    @Override
    public void startAppLinkService(AppLinkServiceConnection.ServiceListener listener) {
        bindToProxyService(listener);
    }

    @Override
    public void onProxyServiceStarted() {
        // Hook
    }

    private void bindToProxyService(AppLinkServiceConnection.ServiceListener listener) {
        appLinkServiceConnection = new AppLinkServiceConnection(listener);
        bindService(new Intent(this, ProxyAppLinkService.class), appLinkServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public AppLinkService getAppLinkService() {
        return appLinkServiceConnection;
    }

    @Override
    protected void onDestroy() {
        stopAppLinkService();
        super.onDestroy();
    }

    //upon onDestroy(), dispose current proxy and create a new one to enable auto-start
    @Override
    public void stopAppLinkService() {
        appLinkServiceConnection.resetConnection();
        unbindService(appLinkServiceConnection);
    }
}
