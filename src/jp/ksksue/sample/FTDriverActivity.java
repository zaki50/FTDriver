package jp.ksksue.sample;
/*
 * Copyright (C) 2011 @ksksue
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

import jp.ksksue.serial.R;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

import jp.ksksue.driver.serial.*;
public class FTDriverActivity extends Activity {
	
	FTDriver mSerial;
	
	boolean mStop=false;
	boolean mStopped=true;
		
	String TAG = "FTDriverActivity";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // get service
        mSerial = new FTDriver((UsbManager)getSystemService(Context.USB_SERVICE));
                
        // listen for new devices
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mUsbReceiver, filter);
        
        if(mSerial.begin(9600)) {	// now only 9600 supported
        	mainloop();
        }
    }
    
    @Override
    public void onDestroy() {
		mSerial.end();
		mStop=true;
       unregisterReceiver(mUsbReceiver);
		super.onDestroy();
    }
    
	private void mainloop() {
		new Thread(mLoop).start();
	}
	
	private Runnable mLoop = new Runnable() {
		@Override
		public void run() {
			int i;
			int len;
			byte[] rbuf = new byte[60];
			byte[] wbuf = new byte[1];
			wbuf[0] = 0x21;
			byte[] wbuf2 = new byte[64];
			
			for(i=0;i<64;++i) {
				wbuf2[i] = (byte) (0x21 + i);
			}
			
			for(;;){//this is the main loop for transferring
				
				//////////////////////////////////////////////////////////
				// Sample for read(buf)
				//////////////////////////////////////////////////////////
				len = mSerial.read(rbuf);
				if(len > 0) {
					Log.i(TAG,"Read  Length : "+len);
					for(i=0;i<len;++i) {
						Log.i(TAG,"Read  Data["+i+"] : "+rbuf[i]);
					}
				}
				rbuf[0] = 0;	// clear
				
				//////////////////////////////////////////////////////////
				// Sample for write(buf)
				//////////////////////////////////////////////////////////
				len = mSerial.write(wbuf);
				Log.i(TAG,"Write Length : "+len);
				Log.i(TAG,"Write Data   : "+wbuf[0]);
				
				if(wbuf[0] == 0x7e) {
					wbuf[0] = 0x21;
				} else {
					++wbuf[0];
				}
				
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				//////////////////////////////////////////////////////////
				// Sample for write(buf,length)
				//////////////////////////////////////////////////////////
				len = mSerial.write(wbuf2,64);
				Log.i(TAG,"Write Length : "+len);

				// 
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if(mStop) {
					mStopped = true;
					return;
				}
			}
		}
	};
	
    // BroadcastReceiver when insert/remove the device USB plug into/from a USB port  
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
    		String action = intent.getAction();
    		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
    			mSerial.usbAttached(intent);
				mSerial.begin(9600);	// only 9600 supported 
    			mainloop();
				
    		} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
    			mSerial.usbDetached(intent);
    			mSerial.end();
    			mStop=true;
    		}
        }
    };
}