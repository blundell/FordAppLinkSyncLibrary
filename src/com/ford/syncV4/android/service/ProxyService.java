package com.ford.syncV4.android.service;

import java.util.Arrays;
import java.util.Vector;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.util.Pair;

import com.ford.syncV4.android.R;
import com.ford.syncV4.android.activity.AppLinkTester;
import com.ford.syncV4.android.adapters.logAdapter;
import com.ford.syncV4.android.constants.Const;
import com.ford.syncV4.android.module.ModuleTest;
import com.ford.syncV4.android.receivers.SyncReceiver;
import com.ford.syncV4.exception.SyncException;
import com.ford.syncV4.exception.SyncExceptionCause;
import com.ford.syncV4.proxy.SyncProxyALM;
import com.ford.syncV4.proxy.interfaces.IProxyListenerALM;
import com.ford.syncV4.proxy.rpc.AddCommandResponse;
import com.ford.syncV4.proxy.rpc.AddSubMenuResponse;
import com.ford.syncV4.proxy.rpc.AlertResponse;
import com.ford.syncV4.proxy.rpc.CreateInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteCommandResponse;
import com.ford.syncV4.proxy.rpc.DeleteInteractionChoiceSetResponse;
import com.ford.syncV4.proxy.rpc.DeleteSubMenuResponse;
import com.ford.syncV4.proxy.rpc.EncodedSyncPDataResponse;
import com.ford.syncV4.proxy.rpc.GenericResponse;
import com.ford.syncV4.proxy.rpc.OnButtonEvent;
import com.ford.syncV4.proxy.rpc.OnButtonPress;
import com.ford.syncV4.proxy.rpc.OnCommand;
import com.ford.syncV4.proxy.rpc.OnDriverDistraction;
import com.ford.syncV4.proxy.rpc.OnEncodedSyncPData;
import com.ford.syncV4.proxy.rpc.OnHMIStatus;
import com.ford.syncV4.proxy.rpc.OnPermissionsChange;
import com.ford.syncV4.proxy.rpc.OnTBTClientState;
import com.ford.syncV4.proxy.rpc.PerformInteractionResponse;
import com.ford.syncV4.proxy.rpc.ResetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetGlobalPropertiesResponse;
import com.ford.syncV4.proxy.rpc.SetMediaClockTimerResponse;
import com.ford.syncV4.proxy.rpc.ShowResponse;
import com.ford.syncV4.proxy.rpc.SpeakResponse;
import com.ford.syncV4.proxy.rpc.SubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.UnsubscribeButtonResponse;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;
import com.ford.syncV4.proxy.rpc.enums.Result;
import com.ford.syncV4.transport.TCPTransportConfig;

public class ProxyService extends Service implements IProxyListenerALM {	
	static final String TAG = "AppLinkTester";
	private Integer autoIncCorrId = 1;

	private static AppLinkTester _mainInstance;	
	private static ProxyService _instance;
	private static SyncProxyALM _syncProxy;
	private static logAdapter _msgAdapter;
	private ModuleTest _testerMain;
	private BluetoothAdapter mBtAdapter;
	private MediaPlayer embeddedAudioPlayer;
	private Boolean playingAudio = false;
	protected SyncReceiver mediaButtonReceiver;
	
	private boolean firstHMIStatusChange = true;
	
	private static boolean waitingForResponse = false;
	
	public void onCreate() {
		super.onCreate();
		
		IntentFilter mediaIntentFilter = new IntentFilter();
		mediaIntentFilter.addAction(Intent.ACTION_MEDIA_BUTTON);
		
		mediaButtonReceiver = new SyncReceiver();
		registerReceiver(mediaButtonReceiver, mediaIntentFilter);
		
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.onCreate()", Log.INFO);
		else Log.i(TAG, "ProxyService.onCreate()");
		
		_instance = this;
	}
	
	public void showLockMain() {
		if(AppLinkTester.getInstance() == null) {
			Intent i = new Intent(this, AppLinkTester.class);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(i);
		}		
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.onStartCommand()", Log.INFO);
		else Log.i(TAG, "ProxyService.onStartCommand()");
		
		startProxyIfNetworkConnected();
		
        setCurrentActivity(AppLinkTester.getInstance());
			
        return START_STICKY;
	}

	private void startProxyIfNetworkConnected() {
		final SharedPreferences prefs = getSharedPreferences(Const.PREFS_NAME,
				MODE_PRIVATE);
		final int transportType = prefs.getInt(
				Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
				Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);

		if (transportType == Const.Transport.KEY_BLUETOOTH) {
			Log.d(TAG, "ProxyService. onStartCommand(). Transport = Bluetooth.");
			mBtAdapter = BluetoothAdapter.getDefaultAdapter();
			if (mBtAdapter != null) {
				if (mBtAdapter.isEnabled()) {
					startProxy();
				}
			}
		} else {
			startProxy();
		}
	}

	public void startProxy() {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.startProxy()", true);
		else Log.i(TAG, "ProxyService.startProxy()");
		
		if (_syncProxy == null) {
			try {
				SharedPreferences settings = getSharedPreferences(
						Const.PREFS_NAME, 0);
				boolean isMediaApp = settings.getBoolean(
						Const.PREFS_KEY_ISMEDIAAPP,
						Const.PREFS_DEFAULT_ISMEDIAAPP);
				String appName = settings.getString(Const.PREFS_KEY_APPNAME,
						Const.PREFS_DEFAULT_APPNAME);
				int transportType = settings.getInt(
						Const.Transport.PREFS_KEY_TRANSPORT_TYPE,
						Const.Transport.PREFS_DEFAULT_TRANSPORT_TYPE);
				String ipAddress = settings.getString(
						Const.Transport.PREFS_KEY_TRANSPORT_IP,
						Const.Transport.PREFS_DEFAULT_TRANSPORT_IP);
				int tcpPort = settings.getInt(
						Const.Transport.PREFS_KEY_TRANSPORT_PORT,
						Const.Transport.PREFS_DEFAULT_TRANSPORT_PORT);
				boolean autoReconnect = settings
						.getBoolean(
								Const.Transport.PREFS_KEY_TRANSPORT_RECONNECT,
								Const.Transport.PREFS_DEFAULT_TRANSPORT_RECONNECT_DEFAULT);

				if (transportType == Const.Transport.KEY_BLUETOOTH) {
					_syncProxy = new SyncProxyALM(this, appName, isMediaApp);
				} else {
					_syncProxy = new SyncProxyALM(this, appName, isMediaApp,
							new TCPTransportConfig(tcpPort, ipAddress, autoReconnect));
				}
			} catch (SyncException e) {
				e.printStackTrace();
				//error creating proxy, returned proxy = null
				if (_syncProxy == null){
					stopSelf();
				}
			}
		}
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.startProxy() returning", Log.INFO);
		else Log.i(TAG, "ProxyService.startProxy() returning");
	}
	
	public void onDestroy() {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.onDestroy()", Log.INFO);
		else Log.i(TAG, "ProxyService.onDestroy()");
		
		disposeSyncProxy();
		_instance = null;
		if (embeddedAudioPlayer != null) embeddedAudioPlayer.release();		
		unregisterReceiver(mediaButtonReceiver);	
		super.onDestroy();
	}
	
	public void disposeSyncProxy() {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("ProxyService.disposeSyncProxy()", Log.INFO);
		else Log.i(TAG, "ProxyService.disposeSyncProxy()");
		
		if (_syncProxy != null) {
			try {
				_syncProxy.dispose();
			} catch (SyncException e) {
				e.printStackTrace();
			}
			_syncProxy = null;
		}
	}
	
	private void initialize() {
		playingAudio = true;
		playAnnoyingRepetitiveAudio();
		
		try {
			_syncProxy.show("AppLink", "Tester", null, null, null, null, nextCorrID());
		} catch (SyncException e) {
			if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
			if (_msgAdapter != null) _msgAdapter.logMessage("Error sending show", Log.ERROR, e, true);
			else Log.e(TAG, "Error sending show", e);
		}

		try { 
			_syncProxy.subscribeButton(ButtonName.OK, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.SEEKLEFT, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.SEEKRIGHT, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.TUNEUP, nextCorrID());
			_syncProxy.subscribeButton(ButtonName.TUNEDOWN, nextCorrID());
			Vector<ButtonName> buttons = new Vector<ButtonName>(Arrays.asList(new ButtonName[] {
					ButtonName.OK, ButtonName.SEEKLEFT, ButtonName.SEEKRIGHT, ButtonName.TUNEUP,
					ButtonName.TUNEDOWN }));
			AppLinkTester.getInstance().buttonsSubscribed(buttons);
		} catch (SyncException e) {
			if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
			if (_msgAdapter != null) _msgAdapter.logMessage("Error subscribing to buttons", Log.ERROR, e, true);
			else Log.e(TAG, "Error subscribing to buttons", e);
		}

		
		try {
			_syncProxy.addCommand(100, "XML Test", new Vector<String>(Arrays.asList(new String[] {"XML Test", "XML"})), nextCorrID());
		} catch (SyncException e) {
			if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
			if (_msgAdapter != null) _msgAdapter.logMessage("Error adding AddCommands", Log.ERROR, e, true);
			else Log.e(TAG, "Error adding AddCommands", e);
		}
	}
	
	public void playPauseAnnoyingRepetitiveAudio() {
		if (embeddedAudioPlayer != null && embeddedAudioPlayer.isPlaying()) {
			playingAudio = false;
			pauseAnnoyingRepetitiveAudio();
		} else {
			playingAudio = true;
			playAnnoyingRepetitiveAudio();
		}
	}

	private void playAnnoyingRepetitiveAudio() {
		if (embeddedAudioPlayer == null) {
			embeddedAudioPlayer = MediaPlayer.create(this, R.raw.arco);
			embeddedAudioPlayer.setLooping(true);
		}
		embeddedAudioPlayer.start();
		
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("Playing audio", true);
		else Log.i(TAG, "Playing audio");
	}
	
	public void pauseAnnoyingRepetitiveAudio() {
		if (embeddedAudioPlayer != null && embeddedAudioPlayer.isPlaying()) {
			embeddedAudioPlayer.pause();
			
			if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
			if (_msgAdapter != null) _msgAdapter.logMessage("Paused audio", true);
			else Log.i(TAG, "Paused audio");
		}
	}
	
	public static SyncProxyALM getProxyInstance() {
		return _syncProxy;
	}

	public static ProxyService getInstance() {
		return _instance;
	}
	
	public AppLinkTester getCurrentActivity() {
		return _mainInstance;
	}

	public void startModuleTest() {
		_testerMain = new ModuleTest();
	}
	
	public static void waiting(boolean waiting) {
		waitingForResponse = waiting;
	}

	public void setCurrentActivity(AppLinkTester currentActivity) {
		if (this._mainInstance != null) {
			this._mainInstance.finish();
			this._mainInstance = null;
		}
		
		this._mainInstance = currentActivity;
		// update the _msgAdapter
		_msgAdapter = AppLinkTester.getMessageAdapter();
	}
	
	protected int nextCorrID() {
		autoIncCorrId++;
		return autoIncCorrId;
	}

	@Override
	public void onOnHMIStatus(OnHMIStatus notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);

		switch(notification.getSystemContext()) {
			case SYSCTXT_MAIN:
				break;
			case SYSCTXT_VRSESSION:
				break;
			case SYSCTXT_MENU:
				break;
			default:
				return;
		}
		
		switch(notification.getAudioStreamingState()) {
			case AUDIBLE:
				if (playingAudio) playAnnoyingRepetitiveAudio();
				break;
			case NOT_AUDIBLE:
				pauseAnnoyingRepetitiveAudio();
				break;
			default:
				return;
		}
		
		switch(notification.getHmiLevel()) {
			case HMI_FULL:
				if (notification.getFirstRun()) {
					showLockMain();
					_testerMain = new ModuleTest();
					_testerMain = ModuleTest.getModuleTestInstance();
					initialize();
				}
				else {
					try {
						if (!waitingForResponse && _testerMain.getThreadContext() != null) {
							_syncProxy.show("Sync Proxy", "Tester Ready", null, null, null, null, nextCorrID());
						}
					} catch (SyncException e) {
						if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
						if (_msgAdapter != null) _msgAdapter.logMessage("Error sending show", Log.ERROR, e, true);
						else Log.e(TAG, "Error sending show", e);
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
				return;
		}
	}
	
	@Override
	public void onOnCommand(OnCommand notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);
		
		switch(notification.getCmdID())
		{
			case 100: //XML Test
				_testerMain.restart();
				break;
			default:
				break;
		}
	}

	@Override
	public void onProxyClosed(String info, Exception e) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("onProxyClosed: " + info, Log.ERROR, e);
		else Log.e(TAG, "onProxyClosed: " + info, e);
		
		boolean wasConnected = !firstHMIStatusChange;
		firstHMIStatusChange = true;
		
		if (wasConnected) {
			final AppLinkTester mainActivity = AppLinkTester.getInstance();
			if (mainActivity != null) {
				mainActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mainActivity.onProxyClosed();
					}
				});
			} else {
				Log.w(TAG, "mainActivity not found");
			}
		}
		
		if(((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.SYNC_PROXY_CYCLED
				&& ((SyncException) e).getSyncExceptionCause() != SyncExceptionCause.BLUETOOTH_DISABLED) {
			reset();
		}
	}
	
	public void reset(){
	   try {
		   if (_syncProxy != null) _syncProxy.resetProxy();
           else startProxyIfNetworkConnected();
		} catch (SyncException e1) {
			e1.printStackTrace();
			//something goes wrong, & the proxy returns as null, stop the service.
			//do not want a running service with a null proxy
			if (_syncProxy == null){
				stopSelf();
			}
		}
	}
	
	/**
	 * Restarting SyncProxyALM. For example after changing transport type
	 */
	public void restart() {
		Log.i(TAG, "ProxyService.Restart SyncProxyALM.");
		disposeSyncProxy();
		startProxyIfNetworkConnected();
	}
	
	@Override
	public void onError(String info, Exception e) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
 		if (_msgAdapter != null) {
 			_msgAdapter.logMessage("******onProxyError******", Log.ERROR);
 			_msgAdapter.logMessage("ERROR: " + info, Log.ERROR, e);
		} else {
			Log.e(TAG, "******onProxyError******");
			Log.e(TAG, "ERROR: " + info, e);
 		}
	}
	
	/*********************************
	** SYNC AppLink Base Callback's **
	*********************************/
	@Override
	public void onAddSubMenuResponse(AddSubMenuResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onCreateInteractionChoiceSetResponse(CreateInteractionChoiceSetResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		final AppLinkTester mainActivity = AppLinkTester.getInstance();
		final boolean success = response.getSuccess();
		mainActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mainActivity.onCreateChoiceSetResponse(success);
			}
		});
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onDeleteCommandResponse(DeleteCommandResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onDeleteInteractionChoiceSetResponse(DeleteInteractionChoiceSetResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onDeleteSubMenuResponse(DeleteSubMenuResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onEncodedSyncPDataResponse(EncodedSyncPDataResponse response) {
		Log.i("syncp", response.getInfo() + response.getResultCode() + response.getSuccess());
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onResetGlobalPropertiesResponse(ResetGlobalPropertiesResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onSetMediaClockTimerResponse(SetMediaClockTimerResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onSpeakResponse(SpeakResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onSubscribeButtonResponse(SubscribeButtonResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onUnsubscribeButtonResponse(UnsubscribeButtonResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onOnDriverDistraction(OnDriverDistraction notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);
	}
	@Override
	public void onGenericResponse(GenericResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onOnButtonEvent(OnButtonEvent notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);
	}
	@Override
	public void onOnButtonPress(OnButtonPress notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);
		
		switch(notification.getButtonName())
		{
			case OK:
				playPauseAnnoyingRepetitiveAudio();
				break;
			case SEEKLEFT:
				break;
			case SEEKRIGHT:
				break;
			case TUNEUP:
				break;
			case TUNEDOWN:
				break;
			default:
				break;
		}
	}
	
	/*********************************
	** SYNC AppLink Updated Callback's **
	*********************************/
	@Override
	public void onAddCommandResponse(AddCommandResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onAlertResponse(AlertResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}	
	@Override
	public void onPerformInteractionResponse(PerformInteractionResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onSetGlobalPropertiesResponse(SetGlobalPropertiesResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onShowResponse(ShowResponse response) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(response, true);
		else Log.i(TAG, "" + response);
		
		if (waitingForResponse && _testerMain.getThreadContext() != null) {
			ModuleTest.responses.add(new Pair<Integer, Result>(response.getCorrelationID(), response.getResultCode()));
			synchronized (_testerMain.getThreadContext()) { _testerMain.getThreadContext().notify();};
		}
	}
	@Override
	public void onOnTBTClientState(OnTBTClientState notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);
	}

	/*********************************
	** SYNC AppLink Policies Callback's **
	*********************************/
	@Override
	public void onOnPermissionsChange(OnPermissionsChange notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);
	}
	@Override
	public void onOnEncodedSyncPData(OnEncodedSyncPData notification) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage(notification, true);
		else Log.i(TAG, "" + notification);
	}
	@Override
	public IBinder onBind(Intent intent) {
		if (_msgAdapter == null) _msgAdapter = AppLinkTester.getMessageAdapter();
		if (_msgAdapter != null) _msgAdapter.logMessage("Service on Bind");
		else Log.i(TAG, "Service on Bind");
		return new Binder();
	}
}
