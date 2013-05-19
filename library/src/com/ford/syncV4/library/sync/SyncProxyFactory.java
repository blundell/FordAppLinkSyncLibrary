package com.ford.syncV4.library.sync;

import android.content.res.Resources;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.library.R;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.transport.TCPTransportConfig;

public class SyncProxyFactory {

    public static SyncProxyALM getSyncProxyALM(Resources resources, IProxyListenerALM listener) throws SyncException {
        String appName = resources.getString(R.string.app_name);
        boolean isMediaApp = resources.getBoolean(R.bool.is_media_app);

        boolean debugUsingTcp = resources.getBoolean(R.bool.debug_using_tcp);
        if (debugUsingTcp) {
            String ipAddress = resources.getString(R.string.debug_ip);
            int tcpPort = resources.getInteger(R.integer.debug_port);
            boolean autoReconnect = resources.getBoolean(R.bool.debug_auto_reconnect);
            TCPTransportConfig transportConfig = new TCPTransportConfig(tcpPort, ipAddress, autoReconnect);
            return new SyncProxyALM(listener, appName, isMediaApp, transportConfig);
        } else {
            return new SyncProxyALM(listener, appName, isMediaApp);
        }
    }

}
