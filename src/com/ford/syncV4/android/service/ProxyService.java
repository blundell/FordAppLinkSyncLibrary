package com.ford.syncV4.android.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import com.ford.syncV4.android.R;
import com.ford.syncV4.android.activity.AppLinkTesterActivity;
import com.ford.syncV4.android.logging.Log;
import com.ford.syncV4.android.persistance.ConnectionPreferences;
import com.ford.syncV4.android.persistance.Const;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.*;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.transport.TCPTransportConfig;
import com.ford.syncV4.transport.TransportType;

import java.util.Arrays;
import java.util.Vector;

import static com.ford.syncV4.exception.SyncExceptionCause.BLUETOOTH_DISABLED;
import static com.ford.syncV4.exception.SyncExceptionCause.SYNC_PROXY_CYCLED;

public class ProxyService extends Service implements IProxyListenerALM, MyAppLinkProxy {
    private static final int COMMAND_ID_CUSTOM = 100;
    private static ProxyService _instance;
    private static SyncProxyALM _syncProxy;

    private MediaPlayer embeddedAudioPlayer;
    private boolean playingAudio = false;
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
        Log.d("ProxyService.onCreate");
        _instance = this;
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
            Log.d("Transport = Network.");
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
                _syncProxy = new SyncProxyALM(this, appName, isMediaApp);
            } else {
                int tcpPort = connectionPreferences.getTcpPort();
                String ipAddress = connectionPreferences.getIpAddress();
                boolean autoReconnect = connectionPreferences.shouldAutoReconnect();
                _syncProxy = new SyncProxyALM(this, appName, isMediaApp, new TCPTransportConfig(tcpPort, ipAddress, autoReconnect));
            }
        } catch (SyncException e) {
            Log.e("Sync Wut", e);
        }
    }

    @Override
    public void playPauseAnnoyingRepetitiveAudio() {
        if (embeddedAudioPlayer != null && embeddedAudioPlayer.isPlaying()) {
            pauseAnnoyingRepetitiveAudio();
            playingAudio = false;
        } else {
            playAnnoyingRepetitiveAudio();
            playingAudio = true;
        }
    }

    private void playAnnoyingRepetitiveAudio() {
        if (embeddedAudioPlayer == null) {
            embeddedAudioPlayer = MediaPlayer.create(this, R.raw.arco);
            embeddedAudioPlayer.setLooping(true);
        }
        embeddedAudioPlayer.start();

        Log.d("Playing audio"); // TODO add callback
    }

    public void pauseAnnoyingRepetitiveAudio() {
        if (embeddedAudioPlayer != null && embeddedAudioPlayer.isPlaying()) {
            embeddedAudioPlayer.pause();

            Log.d("Pausing audio"); // TODO add callback
        }
    }

    @Override
    public SyncProxyALM getSyncProxyInstance() {
        return _syncProxy;
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
                if (playingAudio) {
                    playAnnoyingRepetitiveAudio();
                }
                break;
            case NOT_AUDIBLE:
                pauseAnnoyingRepetitiveAudio();
                break;
            default:
                break;
        }

        switch (notification.getHmiLevel()) {
            case HMI_FULL:
                if (notification.getFirstRun()) {
                    showLockMain();
                    initialize();
                } else {
                    try {
                        _syncProxy.show("Sync Proxy", "Tester Ready", null, null, null, null, nextCorrelationID());
                    } catch (SyncException e) {
                        Log.e("Error sending show", e);
                    }
                }
                break;
            case HMI_LIMITED:
                break;
            case HMI_BACKGROUND:
                break;
            case HMI_NONE:
                break;
            default:
                break;
        }
    }

    private void showLockMain() {
        Intent i = new Intent(this, AppLinkTesterActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(i);
    }

    private void initialize() {
        playingAudio = true;
        playAnnoyingRepetitiveAudio();

        try {
            _syncProxy.show("AppLink", "Playing", null, null, null, null, nextCorrelationID());
        } catch (SyncException e) {
            Log.e("Error sending show", e);
        }

        try {
            _syncProxy.subscribeButton(ButtonName.OK, nextCorrelationID());
            _syncProxy.subscribeButton(ButtonName.SEEKLEFT, nextCorrelationID());
            _syncProxy.subscribeButton(ButtonName.SEEKRIGHT, nextCorrelationID());
            _syncProxy.subscribeButton(ButtonName.TUNEUP, nextCorrelationID());
            _syncProxy.subscribeButton(ButtonName.TUNEDOWN, nextCorrelationID());

            ButtonNameParcel buttonNameParcel = new ButtonNameParcel(Arrays.asList(
                    ButtonName.OK,
                    ButtonName.SEEKLEFT, ButtonName.SEEKRIGHT,
                    ButtonName.TUNEUP, ButtonName.TUNEDOWN));

            ProxyServiceAction.broadcastButtonsSubscribed(this, buttonNameParcel);

        } catch (SyncException e) {
            Log.e("Error subscribing to buttons", e);
        }

        try {
            String menuText = "Custom Command";
            Vector<String> voiceCommands = new Vector<String>(Arrays.asList("Custom Command", "Run Me"));
            _syncProxy.addCommand(COMMAND_ID_CUSTOM, menuText, voiceCommands, nextCorrelationID());
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
            reset();
        }
    }

    @Override
    public void reset() {
        if (_syncProxy != null) {
            if (_syncProxy.getCurrentTransportType() == TransportType.BLUETOOTH) {
                try {
                    _syncProxy.resetProxy();
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
                playPauseAnnoyingRepetitiveAudio();
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
        Log.d("ProxyService.onDestroy");
        disposeSyncProxy();
        if (embeddedAudioPlayer != null) {
            embeddedAudioPlayer.release();
        }
        super.onDestroy();
    }

    private void disposeSyncProxy() {
        Log.d("ProxyService.disposeSyncProxy()");

        if (_syncProxy != null) {
            try {
                _syncProxy.dispose();
            } catch (SyncException ignore) {
                Log.e("Dispose failed.", ignore);
            }
            _syncProxy = null;
        }
    }

    public class ProxyBinder extends Binder {

        public ProxyService getService() {
            return ProxyService.this;
        }

    }
}
