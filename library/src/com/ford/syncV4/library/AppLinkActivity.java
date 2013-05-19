package com.ford.syncV4.library;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.ford.syncV4.library.service.AppLinkService;
import com.ford.syncV4.library.service.AppLinkServiceConnection;

public abstract class AppLinkActivity extends FragmentActivity implements AppLink, AppLinkServiceConnection.ServiceListener {

    private AppLinkComposite appLinkComposite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appLinkComposite = new AppLinkComposite(this);
        startAppLinkService(this);
    }

    @Override
    public void startAppLinkService(AppLinkServiceConnection.ServiceListener listener) {
        appLinkComposite.startAppLinkService(this);
    }

    @Override
    public void onProxyServiceStarted() {
        // Hook
    }

    @Override
    public AppLinkService getAppLinkService() {
        return appLinkComposite.getAppLinkService();
    }

    @Override
    protected void onDestroy() {
        stopAppLinkService();
        super.onDestroy();
    }

    @Override
    public void stopAppLinkService() {
        appLinkComposite.stopAppLinkService();
    }
}
