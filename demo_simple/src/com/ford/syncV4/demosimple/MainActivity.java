package com.ford.syncV4.demosimple;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;

import com.ford.syncV4.library.AppLinkActivity;
import com.ford.syncV4.library.service.AppLinkServiceConnection;

import java.util.Set;

public class MainActivity extends AppLinkActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void startAppLinkService(AppLinkServiceConnection.ServiceListener listener) {
        if (isPairedWithCar()) {
            super.startAppLinkService(listener);
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
}
