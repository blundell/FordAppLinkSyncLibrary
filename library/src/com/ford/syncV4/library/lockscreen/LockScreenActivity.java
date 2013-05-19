package com.ford.syncV4.library.lockscreen;

import android.os.Bundle;

import com.ford.syncV4.library.AppLinkActivity;
import com.ford.syncV4.library.R;

public class LockScreenActivity extends AppLinkActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lockscreen);
    }

    @Override
    public void onBackPressed() {
        getAppLinkService().resetConnection();
        super.onBackPressed();
    }
}
