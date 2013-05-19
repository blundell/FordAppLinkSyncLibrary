package com.ford.syncV4.demosimple;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;

import com.ford.syncV4.library.AppLink;
import com.ford.syncV4.library.AppLinkComposite;

import java.util.Set;

/**
 * If you need to extend another Activity you can use the AppLinkComposite directly
 * just ensure you it to the correct Activity lifecycle methods
 */
public class AlternativeMainActivity extends Activity {

    private AppLink appLinkComposite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        appLinkComposite = new AppLinkComposite(this);
        startAppLinkService();
    }

    private void startAppLinkService() {
        if (isPairedWithCar()) {
            appLinkComposite.startAppLinkService(null);
        } else {
            Log.e("TAG", "BT disabled or Not Paired.");
        }
    }

    private boolean isPairedWithCar() {
        boolean isSYNCpaired = false;
        // Get the local Bluetooth adapter
        BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        //BT Adapter exists, is enabled, and there are paired devices with the name SYNC
        //Ideally start service and start proxy if already connected to sync
        //but, there is no way to tell if a device is currently connected (pre OS 4.0)

        if (mBtAdapter != null) {
            if ((mBtAdapter.isEnabled() && !mBtAdapter.getBondedDevices().isEmpty())) {
                // Get a set of currently paired devices
                Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

                // Check if there is a paired device with the name "SYNC"
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals("SYNC")) {
                        isSYNCpaired = true;
                        break;
                    }
                }
            }
        }
        return isSYNCpaired;
    }

    @Override
    protected void onDestroy() {
        stopAppLinkService();
        super.onDestroy();
    }

    private void stopAppLinkService() {
        appLinkComposite.stopAppLinkService();
    }
}
