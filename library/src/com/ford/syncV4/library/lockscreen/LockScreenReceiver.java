package com.ford.syncV4.library.lockscreen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.ford.syncV4.library.service.ProxyServiceAction;

public class LockScreenReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ProxyServiceAction.ACTION_REQ_LOCK_SCREEN.equals(action)) {
            Intent lockIntent = new Intent(context, LockScreenActivity.class);
            context.startActivity(lockIntent);
        } else if (ProxyServiceAction.ACTION_REQ_UNLOCK_SCREEN.equals(action)) {
            Intent unlockIntent = new Intent(context, LockScreenActivity.class);
//            TODO extras or intent flags or something
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
//        i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
            context.startActivity(unlockIntent);
        }
    }
}
