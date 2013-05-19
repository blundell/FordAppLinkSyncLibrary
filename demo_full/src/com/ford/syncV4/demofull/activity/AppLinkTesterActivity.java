package com.ford.syncV4.demofull.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.Toast;

import com.ford.syncV4.demofull.R;
import com.ford.syncV4.demofull.activity.console.LogAdapter;
import com.ford.syncV4.demofull.activity.dialog.PropertiesDialog;
import com.ford.syncV4.demofull.activity.dialog.SendMessageDialog;
import com.ford.syncV4.demofull.logging.Log;
import com.ford.syncV4.library.AppLinkActivity;
import com.ford.syncV4.library.persistance.ConnectionPreferences;
import com.ford.syncV4.library.persistance.Const;
import com.ford.syncV4.library.service.AppLinkServiceConnection;
import com.ford.syncV4.library.service.ButtonNameParcel;
import com.ford.syncV4.library.service.CreateChoiceSetParcel;
import com.ford.syncV4.library.service.ProxyServiceAction;
import com.ford.syncV4.proxy.RPCMessage;
import com.ford.syncV4.proxy.rpc.enums.ButtonName;

import java.util.ArrayList;
import java.util.List;

import static com.ford.syncV4.library.service.ButtonNameParcel.EXTRA_BUTTON_NAME_PARCEL;

public class AppLinkTesterActivity extends AppLinkActivity implements OnClickListener {

    private SendMessageDialog sendMessageDialog;
    private LogAdapter _msgAdapter;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d("New Intent received " + getClass().getSimpleName());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnSendMessage).setOnClickListener(this);
        findViewById(R.id.btnPlayPause).setOnClickListener(this);

        _msgAdapter = new LogAdapter(false, this, R.layout.list_item_console, new ArrayList<Object>());
        ListView consoleListView = (ListView) findViewById(R.id.messageList);
        consoleListView.setAdapter(_msgAdapter);

        if (savedInstanceState == null) {
            showConnectionSetupDialog();
        } else {
            showPropertiesInTitle();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(proxyServiceReceiver, ProxyServiceAction.FILTER_PROXY_SERVICE);
    }

    /**
     * Shows a dialog where the user can select connection features (protocol
     * version, media flag, app name, language, HMI language, and transport
     * settings). Starts the proxy after selecting.
     */
    private void showConnectionSetupDialog() {
        new PropertiesDialog(this)
                .setOnClickListener(new PropertiesDialog.PropertiesDialogClickListener() {
                    @Override
                    public void onPropertiesSelected() {
                        stopAppLinkService();
                        showPropertiesInTitle();
                        startAppLinkService(null);
                    }
                }).temp().show(); // Move to another Activity, make it a fragment?
    }

    /**
     * Displays the current protocol properties in the activity's title.
     */
    private void showPropertiesInTitle() {
        ConnectionPreferences connectionPreferences = new ConnectionPreferences(this);
        boolean isMedia = connectionPreferences.isAnMediaApp();
        String transportType = connectionPreferences.getTransportType() == Const.Transport.KEY_TCP ? Const.Transport.TCP : Const.Transport.BLUETOOTH;
        setTitle(getString(R.string.app_name) + " (" + (isMedia ? "" : "non-") + "media, " + transportType + ")");
    }

    private static final int PROXY_START = 5;
    private static final int MNU_CLEAR = 10;
    private static final int MNU_UNREGISTER = 14;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        if (result) {
            menu.add(0, PROXY_START, 0, "Proxy Start");
            menu.add(0, MNU_CLEAR, 0, "Clear Messages");
            menu.add(0, MNU_UNREGISTER, 0, "Unregister");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PROXY_START:
                BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();
                if (mBtAdapter != null && !mBtAdapter.isEnabled()) {
                    mBtAdapter.enable();
                }

                startAppLinkService(new AppLinkServiceConnection.ServiceListener() {
                    @Override
                    public void onProxyServiceStarted() {
                        Log.d("Service started and onOptionsItem knows it");
                        getAppLinkService().resetConnection();
                    }
                });

                if (mBtAdapter != null && !mBtAdapter.isDiscovering()) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                }
                return true;
            case MNU_CLEAR:
                _msgAdapter.clear();
                return true;
            case MNU_UNREGISTER:
                stopAppLinkService();
                startAppLinkService(null);
                return true;
        }

        return super.

                onOptionsItemSelected(item);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSendMessage) {
            getSendMessageDialog().setListener(new SendMessageDialog.SendMessageDialogListener() {
                @Override
                public void onSendMessage(RPCMessage message, int correlationIdUsed) {
                    _msgAdapter.logMessage(message, true);
                    getAppLinkService().sendRPCRequest(message);
                    sendMessageDialog = null; // hack till we do fragments
                }
            });
        } else if (id == R.id.btnPlayPause) {
            getAppLinkService().playPauseAudio();
        }
    }

    private SendMessageDialog getSendMessageDialog() {
        if (sendMessageDialog == null) {
            sendMessageDialog = new SendMessageDialog(this);
        }
        return sendMessageDialog;
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(proxyServiceReceiver);
    }

    private BroadcastReceiver proxyServiceReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ProxyServiceAction.ACTION_PROXY_CLOSED.equals(action)) {
                onProxyClosed();
            } else if (ProxyServiceAction.ACTION_BUTTONS_SUBSCRIBED.equals(action)) {
                ButtonNameParcel buttonNameParcel = (ButtonNameParcel) intent.getSerializableExtra(EXTRA_BUTTON_NAME_PARCEL);
                onButtonsSubscribed(buttonNameParcel.getButtonNames());
            } else if (ProxyServiceAction.ACTION_CREATE_INTERACTION_CHOICE_RESPONSE.equals(action)) {
                CreateChoiceSetParcel createChoiceSetParcel = (CreateChoiceSetParcel) intent.getSerializableExtra(CreateChoiceSetParcel.EXTRA_CREATE_CHOICE_SET_PARCEL);
                onCreateChoiceSetResponse(createChoiceSetParcel.isSuccessful());
            } else if (ProxyServiceAction.ACTION_TEST_CUSTOM_COMMAND.equals(action)) {
                Toast.makeText(AppLinkTesterActivity.this, "Received a custom command", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Called when a connection to a SYNC device has been closed.
     */
    private void onProxyClosed() {
        Log.d("onProxyClosed");
        sendMessageDialog = null;
        _msgAdapter.logMessage("Disconnected", true);
    }

    /**
     * Called when the app is activated from HMI for the first time. ProxyAppLinkService
     * automatically subscribes to buttons, so we reflect that in the
     * subscription list.
     */
    private void onButtonsSubscribed(List<ButtonName> buttons) {
        Log.d("onButtonsSubscribed");
        if (sendMessageDialog != null) { // change to a fragment then change this behaviour
            sendMessageDialog.setSubscribedButtons(buttons);
        }
    }

    /**
     * Called when a CreateChoiceSetResponse comes. If successful, add it to the
     * adapter. In any case, remove the key from the map.
     */
    private void onCreateChoiceSetResponse(boolean success) {
        Log.d("onCreateChoiceSetResponse");
        if (sendMessageDialog != null) {
            sendMessageDialog.updateChoiceSet(success);
        }
    }
}

