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
import com.ford.syncV4.library.sync.SyncProxyFactory;
import com.ford.syncV4.proxy.RPCMessage;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.DriverDistractionState;
import com.ford.syncV4.transport.TransportType;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import static com.ford.syncV4.exception.SyncExceptionCause.BLUETOOTH_DISABLED;
import static com.ford.syncV4.exception.SyncExceptionCause.SYNC_PROXY_CYCLED;

public class ProxyAppLinkService extends Service implements IProxyListenerALM, AppLinkService {
    private static final int COMMAND_ID_CUSTOM = 100;

    private int autoIncrementedCorrId = 1;
    private SyncProxyALM proxy;

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
        boolean debugUsingTcp = getResources().getBoolean(R.bool.debug_using_tcp);

        if (debugUsingTcp) {
            Log.d("Transport = Network. Used for Development Mode.");
            startSyncProxy();
        } else {
            Log.d("Transport = Bluetooth.");
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                startSyncProxy();
            } else {
                Log.e("Unable to start proxy - Bluetooth is disabled");
            }
        }
    }

    private void startSyncProxy() {
        try {
            proxy = SyncProxyFactory.getSyncProxyALM(getResources(), this);
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
        Log.d("OnHMIStatus notification ");

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
        ProxyServiceAction.broadcastScreenLockRequest(this);
    }

    private void clearLockScreen() {
        ProxyServiceAction.broadcastScreenUnlockRequest(this);
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
            List<ButtonName> buttonNames = Arrays.asList(
                    ButtonName.OK,
                    ButtonName.SEEKLEFT, ButtonName.SEEKRIGHT,
                    ButtonName.TUNEUP, ButtonName.TUNEDOWN,
                    ButtonName.PRESET_1, ButtonName.PRESET_2, ButtonName.PRESET_3, ButtonName.PRESET_4,
                    ButtonName.PRESET_5, ButtonName.PRESET_6, ButtonName.PRESET_7, ButtonName.PRESET_8,
                    ButtonName.PRESET_9, ButtonName.PRESET_0);

            for (ButtonName buttonName : buttonNames) {
                proxy.subscribeButton(buttonName, nextCorrelationID());
            }

            ButtonNameParcel buttonNameParcel = new ButtonNameParcel(buttonNames);
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

    /**
     * ******************************
     * * SYNC AppLink Base Callbacks **
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
    public void onOnDriverDistraction(OnDriverDistraction notification) {
        Log.d(notification.toString());
        if (notification.getState() == DriverDistractionState.DD_OFF) {
            Log.i("clear lock, DD_OFF");
            clearLockScreen();
        } else {
            Log.i("show lock screen, DD_ON");
            showLockScreen();
        }
    }

    @Override
    public void onOnButtonPress(OnButtonPress notification) {
        Log.d(notification.toString());

        ButtonName buttonName = notification.getButtonName();
        switch (buttonName) {
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
                Log.d("Something else pressed: " + buttonName);
                break;
        }
        ProxyServiceAction.broadcastButtonPressed(this, new ButtonPressedParcel(buttonName));
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
    public void onGenericResponse(GenericResponse response) {
        Log.d(response.toString());
    }

    @Override
    public void onOnButtonEvent(OnButtonEvent notification) {
        Log.d(notification.toString());
    }

    /**
     * ******************************
     * * SYNC AppLink Updated Callbacks **
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
     * * SYNC AppLink Policies Callbacks **
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

    // -- end

    @Override
    public void onProxyClosed(String info, Exception e) {
        Log.e("onProxyClosed: " + info, e);

        ProxyServiceAction.broadcastProxyClosed(this);

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

    @Override
    public void onDestroy() {
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
