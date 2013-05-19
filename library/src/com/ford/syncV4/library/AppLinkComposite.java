package com.ford.syncV4.library;

import android.content.Context;
import android.content.Intent;

import com.ford.syncV4.library.service.AppLinkService;
import com.ford.syncV4.library.service.AppLinkServiceConnection;
import com.ford.syncV4.library.service.ProxyAppLinkService;

public class AppLinkComposite implements AppLink {

    private final Context context;
    private AppLinkServiceConnection appLinkServiceConnection;

    public AppLinkComposite(Context context) {
        this.context = context;
    }

    @Override
    public void startAppLinkService(AppLinkServiceConnection.ServiceListener listener) {
        bindToProxyService(listener);
    }

    private void bindToProxyService(AppLinkServiceConnection.ServiceListener listener) {
        appLinkServiceConnection = new AppLinkServiceConnection(listener);
        context.bindService(new Intent(context, ProxyAppLinkService.class), appLinkServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public AppLinkService getAppLinkService() {
        return appLinkServiceConnection;
    }

    @Override
    public void stopAppLinkService() {
        appLinkServiceConnection.resetConnection();
        context.unbindService(appLinkServiceConnection);
    }
}
