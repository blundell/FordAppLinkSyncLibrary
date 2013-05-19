/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package com.ford.syncV4.demosimple;

import com.ford.syncV4.proxy.SyncProxyALM;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class LockScreenActivity extends Activity{
	int itemcmdID = 0;
	int subMenuId = 0;
	private static LockScreenActivity instance = null;
	
	public static LockScreenActivity getInstance() {
		return instance;
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.lockscreen);
		
		final Button resetSYNCButton = (Button)findViewById(R.id.lockreset);
		resetSYNCButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//if not already started, show main activity and end lock screen activity
				if(MainActivity.getInstance() == null) {
					Intent i = new Intent(getBaseContext(), MainActivity.class);
					i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getApplication().startActivity(i);
				}
				
				//reset proxy; do not shut down service
				AppLinkService serviceInstance = AppLinkService.getInstance();
				if (serviceInstance != null){
					SyncProxyALM proxyInstance = serviceInstance.getProxy();
					if(proxyInstance != null){
						serviceInstance.reset();
					} else {
						serviceInstance.startProxy();
					}
				}
				
				exit();
			}
		});
    }
    
    //disable back button on lockscreen
    @Override
    public void onBackPressed() {
    }
    
    public void exit() {
    	super.finish();
    }
    
    public void onDestroy(){
    	super.onDestroy();
    	instance = null;
    }
}
