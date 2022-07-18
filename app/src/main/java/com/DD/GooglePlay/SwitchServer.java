package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.createTime;
import static com.DD.GooglePlay.MainActivity.isJobIdRunning;
import static com.DD.GooglePlay.MainActivity.temp_time;
import static com.DD.GooglePlay.MainService.ChangePhotoes;
import static com.DD.GooglePlay.MainService.changeMHPhotos;
import static com.DD.GooglePlay.MainService.local_hour;
import static com.DD.GooglePlay.MainService.schedulejob;
import static com.DD.GooglePlay.MainService.sharedPref;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.time.LocalTime;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class SwitchServer extends Service {
    Timer timer;
    TimerTask timerTask;
    int asked;
    int feeling_time;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if(timer == null){
            if(!sharedPref.getBoolean("switchbtn", true)) startTimer();
            else stopSelf();
        }
        else{
            if(!sharedPref.getBoolean("switchbtn", true)) stoptimertask();
            else stopSelf();
        }
        return START_STICKY;
    }


    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        stoptimertask();
        super.onDestroy();
        startTimer();
    }

    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();


    public void startTimer() {
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 10 * 1000, 10 * 1000);
    }

    public void stoptimertask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        try {
                            sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                            //TODO CALL NOTIFICATION FUNC
                            temp_time = createTime();
                            asked = sharedPref.getInt("asked", 0);
                            feeling_time = sharedPref.getInt("feeling_time", temp_time);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                LocalTime localTime = LocalTime.now();
                                local_hour = localTime.getHour();
                            } else local_hour = Calendar.getInstance().getTime().getHours();
                            SharedPreferences.Editor editor = sharedPref.edit();
                            if ((local_hour > 6 && local_hour < 19 && asked != 0) || temp_time - feeling_time > 54000) {
                                editor.remove("feeling_time");
                                editor.putInt("asked", 0);
                                editor.putBoolean("finished_feeling", true);
                                ChangePhotoes();
                            }
                            boolean mHChangeAnswered = sharedPref.getBoolean("mHChangeAnswered", false);
                            if (mHChangeAnswered) {
                                editor.putBoolean("mHChangeAnswered", false);
                                changeMHPhotos();
                            }
                            if (sharedPref.getBoolean("switchbtn", true)) {
                                editor.putInt("SwitchTime", temp_time);
                                editor.apply();
                            }
                            if (temp_time - sharedPref.getInt("SwitchTime", temp_time) >= sharedPref.getInt("SwitchDelay", 30 * 60)) {
                                editor.putBoolean("switchbtn", true);
                                editor.apply();
                                schedulejob(new Util(), getApplicationContext());
                                stopSelf();
                            }
                            editor.apply();
                        } catch (Exception e) {
                            startTimer();
                        }
                    }
                });
            }
        };
    }
}
