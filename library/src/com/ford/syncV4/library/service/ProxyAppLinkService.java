package com.ford.syncV4.library.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.library.R;
import com.ford.syncV4.library.logging.Log;
import com.ford.syncV4.library.persistance.ConnectionPreferences;
import com.ford.syncV4.library.persistance.Const;
import com.ford.syncV4.proxy.RPCMessage;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.ford.syncV4.transport.TransportType;

import java.util.Arrays;
import java.util.Vector;

import static com.ford.syncV4.exception.SyncExceptionCause.BLUETOOTH_DISABLED;
import static com.ford.syncV4.exception.SyncExceptionCause.SYNC_PROXY_CYCLED;

public class ProxyAppLinkService extends Service implements IProxyListenerALM, AppLinkService {
    private static final int COMMAND_ID_CUSTOM = 100;
    private static SyncProxyALM proxy;

    private boolean firstHMIStatusChange = true;
    private int autoIncrementedCorrId = 1;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("Service on Bind");
        return new ProxyBinder();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ProxyAppLinkService.onCreate");
        startProxyIfNetworkConnected();
    }

    private void startProxyIfNetworkConnected() {
        ConnectionPreferences connectionPreferences = new ConnectionPreferences(this);
        int transportType = connectionPreferences.getTransportType();

        if (transportType == Const.Transport.KEY_BLUETOOTH) {
            Log.d("Transport = Bluetooth.");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                startSyncProxy();
            }
        } else {
            Log.d("Transport = Network. Used for Development Mode.");
            startSyncProxy();
        }
    }

    private void startSyncProxy() {
        try {
            ConnectionPreferences connectionPreferences = new ConnectionPreferences(this);
            int transportType = connectionPreferences.getTransportType();
            boolean isMediaApp = connectionPreferences.isAnMediaApp();

            String appName = getString(R.string.app_name);

            if (transportType == Const.Transport.KEY_BLUETOOTH) {
                proxy = new SyncProxyALM(this, appName, isMediaApp);
            } else {
                // Dev mode - move these out of preferences
                // and into a resource file, which can be toggle on
                // with a boolean
                int tcpPort = connectionPreferences.getTcpPort();
                String ipAddress = connectionPreferences.getIpAddress();
                boolean autoReconnect = connectionPreferences.shouldAutoReconnect();
                proxy = new SyncProxyALM(this, appName, isMediaApp, new TCPTransportConfig(tcpPort, ipAddress, autoReconnect));
            }
        } catch (SyncException e) {
            Log.e("Sync Wut", e);
            stopSelf();
        }
    }

    @Override
    public void resetConnection() {
        if (proxy != null) {
            if (proxy.getCurrentTransportType() == TransportType.BLUETOOTH) {
                try {
                    proxy.resetProxy();
                } catch (SyncException ignore) {
                    Log.e("Reset failed", ignore);
                }
            } else {
                Log.e("endSyncProxyInstance. No reset required if transport is TCP");
            }
        } else {
            startProxyIfNetworkConnected();
        }
    }

    @Override
    public void sendRPCRequest(RPCMessage message) {
        if (proxy != null) {
            try {
                proxy.sendRPCRequest(message);
            } catch (SyncException e) {
                Log.e("Error sending message", e);
            }
        } else {
            Log.e("Sync Proxy null, can't send a message");
        }
    }

    private int nextCorrelationID() {
        autoIncrementedCorrId++;
        return autoIncrementedCorrId;
    }

    @Override
    public void onOnHMIStatus(OnHMIStatus notification) {
        Log.d("OnHMIStatus notificiation ");

        switch (notification.getAudioStreamingState()) {
            case AUDIBLE:
                // Play audio if applicable - i.e. add callback
                break;
            case NOT_AUDIBLE:
                // Pause audio if applicable - i.e. add callback
                break;
            default:
                break;
        }

        switch (notification.getHmiLevel()) {
            case HMI_FULL:
                showLockScreen();
                //setup app on SYNC
                if (notification.getFirstRun()) {  // add callback?
                    showMessage("AppLink", "Welcome");
                    subscribeToButtonEvents();
                    addCustomVoiceCommands();
                } else {
                    showMessage("Synced Before", "Welcome Back");
                }
                break;
            case HMI_LIMITED:
                showLockScreen();
                break;
            case HMI_BACKGROUND:
                showLockScreen();
                break;
            case HMI_NONE:
                clearLockScreen();
                break;
            default:
                break;
        }
    }

    private void showLockScreen() {
//        Intent i = new Intent(this, AppLinkTesterActivity.class);
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
//        startActivity(i);   TODO
    }

    private void clearLockScreen() {
        // TODO
    }

    private void showMessage(String line1, String line2) {
        try {
            proxy.show(line1, line2, null, null, null, null, nextCorrelationID());
        } catch (SyncException e) {
            Log.e("Error sending show " + line1 + " " + line2 + ".", e);
        }
    }

    private void subscribeToButtonEvents() {
        try {
            proxy.subscribeButton(ButtonName.OK, nextCorrelationID());
            proxy.subscribeButton(ButtonName.SEEKLEFT, nextCorrelationID());
            proxy.subscribeButton(ButtonName.SEEKRIGHT, nextCorrelationID());
            proxy.subscribeButton(ButtonName.TUNEUP, nextCorrelationID());
            proxy.subscribeButton(ButtonName.TUNEDOWN, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_1, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_2, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_3, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_4, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_5, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_6, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_7, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_8, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_9, nextCorrelationID());
            proxy.subscribeButton(ButtonName.PRESET_0, nextCorrelationID());

            ButtonNameParcel buttonNameParcel = new ButtonNameParcel(Arrays.asList(
                    ButtonName.OK,
                    ButtonName.SEEKLEFT, ButtonName.SEEKRIGHT,
                    ButtonName.TUNEUP, ButtonName.TUNEDOWN));

            ProxyServiceAction.broadcastButtonsSubscribed(this, buttonNameParcel);

        } catch (SyncException e) {
            Log.e("Error subscribing to buttons", e);
        }
    }

    private void addCustomVoiceCommands() {
        try {
            String menuText = "Custom Command";
            Vector<String> voiceCommands = new Vector<String>(Arrays.asList("Custom Command", "Run Me"));
            proxy.addCommand(COMMAND_ID_CUSTOM, menuText, voiceCommands, nextCorrelationID());
        } catch (SyncException e) {
            Log.e("Error adding AddCommands", e);
        }
    }

    @Override
    public void onOnCommand(OnCommand notification) {
        Log.d("An OnCommand was received. " + notification.getCmdID());

        switch (notification.getCmdID()) {
            case COMMAND_ID_CUSTOM:
                ProxyServiceAction.broadcastTestCustomCommand(this);
                break;
            default:
                break;
        }
    }

    @Override
    public void onProxyClosed(String info, Exception e) {
        Log.e("onProxyClosed: " + info, e);

        boolean wasConnected = !firstHMIStatusChange;
        firstHMIStatusChange = true;

        if (wasConnected) { // always false?
            ProxyServiceAction.broadcastProxyClosed(this);
        }

        SyncExceptionCause syncExceptionCause = ((SyncException) e).getSyncExceptionCause();
        if (syncExceptionCause != SYNC_PROXY_CYCLED && syncExceptionCause != BLUETOOTH_DISABLED) {
            resetConnection();
        }
    }

    @Override
    public void onError(String info, Exception e) {
        Log.e("******onProxyError******");
        Log.e("ERROR: " + info, e);
    }

    /**
     * ******************************
     * * SYNC AppLink Base Callback's **
     * *******************************
     */
    @Override
    public void onAddSubMenuResponse(AddSubMenuResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {

        Log.d(response.toString());

        CreateChoiceSetParcel createChoiceSetParcel = new CreateChoiceSetParcel(response);
        ProxyServiceAction.broadcastCreateInteractionChoiceSetResponded(this, createChoiceSetParcel);
    }

    @Override
    public void onDeleteCommandResponse(DeleteCommandResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse response) {
        Log.d(response.getInfo() + response.getResultCode() + response.getSuccess());

        Log.d(response.toString());
    }

    @Override
    public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onSpeakResponse(SpeakResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onSubscribeButtonResponse(SubscribeButtonResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onOnDriverDistraction(OnDriverDistraction notification) {
        Log.d(notification.toString());
        if (notification.getState() == DriverDistractionState.DD_OFF) {
            Log.i("clear lock, DD_OFF");
            clearLockScreen();
        } else {
            Log.i("show lockscreen, DD_ON");
            showLockScreen();
        }
    }

    @Override
    public void onGenericResponse(GenericResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {

        Log.d(notification.toString());
    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {

        Log.d(notification.toString());

        switch (notification.getButtonName()) {
            case OK:
                Log.d("Ok pressed");
                break;
            case SEEKLEFT:
                Log.d("Seek left pressed");
                break;
            case SEEKRIGHT:
                Log.d("Seek right pressed");
                break;
            case TUNEUP:
                Log.d("Tune up pressed");
                break;
            case TUNEDOWN:
                Log.d("Tune down pressed");
                break;
            default:
                Log.d("Something else pressed: " + notification.getButtonName());
                break;
        }
    }

    /**
     * ******************************
     * * SYNC AppLink Updated Callback's **
     * *******************************
     */
    @Override
    public void onAddCommandResponse(AddCommandResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onAlertResponse(AlertResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onPerformInteractionResponse(PerformInteractionResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onShowResponse(ShowResponse response) {

        Log.d(response.toString());
    }

    @Override
    public void onOnTBTClientState(OnTBTClientState notification) {

        Log.d(notification.toString());
    }

    /**
     * ******************************
     * * SYNC AppLink Policies Callback's **
     * *******************************
     */
    @Override
    public void onOnPermissionsChange(OnPermissionsChange notification) {

        Log.d(notification.toString());
    }

    @Override
    public void onOnEncodedSyncPData(OnEncodedSyncPData notification) {

        Log.d(notification.toString());
    }

    @Override
    public void onDestroy() {
        Log.d("ProxyAppLinkService.onDestroy");
        disposeSyncProxy();
        clearLockScreen();
        super.onDestroy();
    }

    private void disposeSyncProxy() {
        Log.d("ProxyAppLinkService.disposeSyncProxy()");

        if (proxy != null) {
            try {
                proxy.dispose();
            } catch (SyncException ignore) {
                Log.e("Dispose failed.", ignore);
            }
            proxy = null;
        }
    }

    public class ProxyBinder extends Binder {

        public AppLinkService getService() {
            return ProxyAppLinkService.this;
        }

    }
}
