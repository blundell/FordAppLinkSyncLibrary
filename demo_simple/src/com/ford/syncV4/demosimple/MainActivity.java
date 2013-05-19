/**Ford Motor Company
 * September 2012
 * Elizabeth Halash
 */

package com.ford.syncV4.demosimple;

import java.util.Set;

import com.ford.syncV4.proxy.SyncProxyALM;

import android.os.Bundle;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends Activity {

    private static final String TAG = "hello";
	private static MainActivity instance = null;
	private boolean activityOnTop;
	
	public static MainActivity getInstance() {
		return instance;
	}
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;
        startSyncProxyService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    
    @Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	        case R.id.exit:
	        	super.finish();
	            return true;
	        case R.id.reset:
	        	endSyncProxyInstance();
	        	startSyncProxyService();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}

    public void startSyncProxyService() {
		boolean isSYNCpaired = false;
	        // Get the local Bluetooth adapter
            BluetoothAdapter mBtAdapter = BluetoothAdapter.getDefaultAdapter();

            //BT Adapter exists, is enabled, and there are paired devices with the name SYNC
    		//Ideally start service and start proxy if already connected to sync
    		//but, there is no way to tell if a device is currently connected (pre OS 4.0)
            
            if (mBtAdapter != null)
    		{
    			if ((mBtAdapter.isEnabled() && mBtAdapter.getBondedDevices().isEmpty() != true)) 
    			{
    				// Get a set of currently paired devices
    				Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
    		
    				// Check if there is a paired device with the name "SYNC"
    		        if (pairedDevices.size() > 0) {
    		            for (BluetoothDevice device : pairedDevices) {
    		               if (device.getName().toString().equals("SYNC")) {
    		            	   isSYNCpaired = true;
    		            	   break;
    		               }
    		            }
    		        } else {
    		        	Log.i("TAG", "A No Paired devices with the name sync");
    		        }
    		        
    		        if (isSYNCpaired == true) { 		        	
    		        	if (AppLinkService.getInstance() == null) {
    		        		Intent startIntent = new Intent(this, AppLinkService.class);
    						startService(startIntent);
    		        	} else {
    		        		//if the service is already running and proxy is up, set this as current UI activity
    		    			AppLinkService.getInstance().setCurrentActivity(this);	
    		    			Log.i("TAG", " proxyAlive == true success");
    		        	}
    		        }
    			}
    		}
    	}
   
    	
    	//upon onDestroy(), dispose current proxy and create a new one to enable auto-start
    	//call resetProxy() to do so
    	public void endSyncProxyInstance() {	
    		AppLinkService serviceInstance = AppLinkService.getInstance();
    		if (serviceInstance != null){
    			SyncProxyALM proxyInstance = serviceInstance.getProxy();
    			//if proxy exists, reset it
    			if(proxyInstance != null){			
    				serviceInstance.reset();
    			//if proxy == null create proxy
    			} else {
    				serviceInstance.startProxy();
    			}
    		}
    	}
    	
    	protected void onDestroy() {
    		Log.v(TAG, "onDestroy main");
    		endSyncProxyInstance();
    		instance = null;
    		AppLinkService serviceInstance = AppLinkService.getInstance();
    		if (serviceInstance != null){
    			serviceInstance.setCurrentActivity(null);
    		}
    		super.onDestroy();
    	}
    	
    	protected void onResume() {
			activityOnTop = true;
			//check if lockscreen should be up
			AppLinkService serviceInstance = AppLinkService.getInstance();
    		if (serviceInstance != null){
    			if (serviceInstance.getLockScreenStatus() == true) {
					if(LockScreenActivity.getInstance() == null) {
						Intent i = new Intent(this, LockScreenActivity.class);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						i.addFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION);
						startActivity(i);
					}	
    			}
			}
    		super.onResume();
		}
    	
    	protected void onPause() {
			activityOnTop = false;
			super.onPause();
		}

    	public boolean isActivityonTop(){
			return activityOnTop;
		}
}
