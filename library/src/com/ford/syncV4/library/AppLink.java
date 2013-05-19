package com.ford.syncV4.library;

import com.ford.syncV4.library.service.AppLinkService;
import com.ford.syncV4.library.service.AppLinkServiceConnection;

public interface AppLink {
    void startAppLinkService(AppLinkServiceConnection.ServiceListener listener);

    AppLinkService getAppLinkService();

    void stopAppLinkService();
}
