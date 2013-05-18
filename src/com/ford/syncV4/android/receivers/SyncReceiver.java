package com.ford.syncV4.android.receivers;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.view.KeyEvent;

import com.ford.syncV4.android.logging.Log;
import com.ford.syncV4.android.service.ProxyService;

public class SyncReceiver extends BroadcastReceiver {
    private Intent intent;
    private ProxyService serviceInstance;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("SyncReceiver Received Intent with action: " + intent.getAction());
        this.intent = intent;
        this.serviceInstance = peekService(context);

        if (phoneJustTurnedOn() && phoneHasBluetoothOn()) {
            startService(context);
        } else if (changeOfBluetoothState()) {
            if (bluetoothTurningOff()) {
                stopService(context);
            } else if (bluetoothTurningOn()) {
                if (serviceRunning()) {
                    resetService();
                } else {
                    startService(context);
                }
            }
        } else if (mediaButtonPressed()) {
            KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                abortBroadcast();
            }
        } else if (audioBecomingTooNoisy()) {
            if (serviceRunning()) {
                serviceInstance.pauseAnnoyingRepetitiveAudio();
            }
        }
    }

    private ProxyService peekService(Context context) {
        IBinder iBinder = peekService(context, new Intent(context, ProxyService.class));
        if (iBinder == null) {
            return null;
        }
        return ((ProxyService.ProxyBinder) iBinder).getService();
    }

    private boolean phoneJustTurnedOn() {
        return intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED);
    }

    private boolean phoneHasBluetoothOn() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    private void startService(Context context) {
        Log.d("Bt on start service");
        Intent startIntent = new Intent(context, ProxyService.class);
        startIntent.putExtras(intent);
        context.startService(startIntent);
    }

    private boolean changeOfBluetoothState() {
        return intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED);
    }

    private boolean bluetoothTurningOff() {
        return (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == (BluetoothAdapter.STATE_TURNING_OFF));
    }

    private void stopService(Context context) {
        Log.d("Bt off stop service");
        Intent stopIntent = new Intent(context, ProxyService.class);
        stopIntent.putExtras(intent);
        context.stopService(stopIntent);
    }

    private boolean bluetoothTurningOn() {
        return (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == (BluetoothAdapter.STATE_TURNING_ON));
    }

    private boolean serviceRunning() {
        return serviceInstance != null;
    }

    private void resetService() {
        serviceInstance.reset();
    }

    private boolean mediaButtonPressed() {
        return intent.getAction().compareTo(Intent.ACTION_MEDIA_BUTTON) == 0;
    }

    private boolean audioBecomingTooNoisy() {
        return intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    }
}
