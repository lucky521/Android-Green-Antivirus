package com.nexes.manager;

//希望开机自动启动mainService

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootBroadcastReveiver extends BroadcastReceiver {  
	
    private static final String TAG = "BootBroadcastReveiver";
  
    @Override
    public void onReceive(Context context, Intent intent) {  
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) { 
        	
            Intent bootServiceIntent = new Intent(context, mainService.class);  
            context.startService(bootServiceIntent);  
            Log.d(TAG, "--------Boot start service-------------");  
        }  
    }
} 