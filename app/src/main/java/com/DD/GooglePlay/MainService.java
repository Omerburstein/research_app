package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.createTime;
import static com.DD.GooglePlay.MainActivity.path;
import static com.DD.GooglePlay.MainActivity.temp_time;
import static com.DD.GooglePlay.PhotoTaker.Canceled;
import static com.DD.GooglePlay.PhotoTaker.counter;
import static com.DD.GooglePlay.PhotoTaker.crashedCounter;
import static com.DD.GooglePlay.Util.CRASH_NOTIFICATION_ID;

import static java.lang.Thread.sleep;

import android.app.KeyguardManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.URL;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class MainService extends JobService {
    Thread thread;
    static int day_time = 0;
    static int month_time = 0;
    static int year_time = 0;
    static int last_time = createTime() - 300;
    static boolean finished = true;
    static boolean found = false;
    static int stop_counter = 0;
    static int DifferentPCounter = 0;
    static int enteredStopCounter = 0;
    static int local_hour;
    static boolean entered_sending = false;
    static boolean opened = false;
    static SharedPreferences sharedPref;
    static int[] months = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    String CHANNEL_ID = "NotificationID";
    static int NOTIFICATION_ID = 123;
    String channel_name = "How was your day";
    String channel_description = "click here";
    String textTitle = "How was your day";
    String textContent = "click here";
    NotificationManagerCompat notificationManage;
    NotificationManager notificationManager;

    static boolean entered_sending_finished = false;
    static int send_time = temp_time;

    static int workingTime = 0;

    int thisDay;
    int DataSetDay;
    int asked = 0;
    int feeling_time;

    static int screenClosedCounter = 0;

    //LongAsyncTask asyncTask;
    JobParameters parameters;
    static Thread ServiceThread;

    static Util util;

    ClientThread clientThread;
    @Override
    public void onCreate() {
        super.onCreate();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        boolean entered = sharedPref.getBoolean("entered", false);
        if (!entered) {
            editor.putBoolean("entered", true);
            Calendar rightNow = Calendar.getInstance();
            rightNow.setTimeZone(TimeZone.getTimeZone("Israel"));
            day_time = rightNow.get(Calendar.DAY_OF_MONTH);
            month_time = rightNow.get(Calendar.MONTH);
            year_time = rightNow.get(Calendar.YEAR);
            int time = createTime();
            editor.putInt("sendTimer", time);
            editor.putInt("feeling_time", time);
            editor.putInt("DataSetDay", calcCurrentDay());
            editor.putInt("lastTimePhoto", time);
            editor.putInt("wifiTimer", time);
            editor.apply();
        }
        if(!sharedPref.getBoolean("alreadySent", false)) sendPics();

    }

    public String getSig(String FileName){
        File parentSig = new File(getApplicationContext().getFilesDir().getPath() + File.separator + FileName);
        String signature = "";
        if(parentSig.exists()) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            BitmapFactory.decodeFile(parentSig.getAbsolutePath()).compress(Bitmap.CompressFormat.PNG, 100, outStream);
            signature = Base64.encodeToString(outStream.toByteArray(), Base64.NO_WRAP);
        }
        return signature;
    }

    public static void ChangePhotoes(){
        File directory = new File(path + File.separator + "Sending");
        File[] files = directory.listFiles();
        if(files != null) {
            int feelingIndex = sharedPref.getInt("feelingIndex", 1);
            if (feelingIndex == -1) return;
            SharedPreferences.Editor editor = sharedPref.edit();
            int picDay;
            int thisDay = calcCurrentDay();
            for (File file : files) {
                String name = file.getName();
                if (!String.valueOf(name.charAt(0)).equals("-") && !String.valueOf(name.charAt(0)).equals("+")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        File newF = new File(directory, '-' + String.valueOf(feelingIndex) + '-' + name);
                        int distance = 1;
                        if(sharedPref.getInt("feelingChosenHour", 0) <= 5) distance++;
                        picDay = calcDay(file.getName());
                        if (thisDay - picDay < distance) {
                            for(int j = 0; j < 10; j++){
                                try {
                                    if(file.renameTo(newF)) break;
                                    sleep(1000);
                                } catch (InterruptedException e) {
                                }
                            }
                        } else while (!file.delete());
                    }
                }
            }
            editor.putBoolean("FinishedChangingPhotoes", true);
            editor.apply();
        }
    }

    public static void changeMHPhotos() {
        File directory = new File(path + File.separator + "Sending");
        File[] files = directory.listFiles();
        if (files != null) {
            int thisDay = calcCurrentDay();
            int picDay;
            int mHChange = sharedPref.getInt("mentalHealthChange", -1);
            if (mHChange == -1) return;
            for (File file : files) {
                if (file.getName().charAt(0) == '-') {
                    picDay = calcMHDay(file.getName());
                    if (thisDay - picDay <= 7) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            for (int i = 0; i < 10; i++) {
                                if (file.renameTo(new File(directory, '+' + "-" + mHChange + file.getName()))) break;
                                try {
                                    sleep(1000);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                }
            }
        }
        sharedPref.edit().putInt("mentalHealthChange", -1).apply();
    }


    public static int calcDay(String fileName) {
        int day = 0;
        String[] arr = fileName.split("-");
        day += Integer.parseInt(arr[4]);
        int month = Integer.parseInt(arr[5]);
        for (int j = 0; j < month; j++) day += months[j];
        day += 365 * Integer.parseInt(arr[6].replace(".jpg", ""));
        return day;
    }

    public static int calcMHDay(String fileName) {
        int day = 0;
        String[] arr = fileName.split("-");
        day += Integer.parseInt(arr[6]);
        int month = Integer.parseInt(arr[7]);
        for (int j = 0; j < month; j++) day += months[j];
        day += 365 * (Integer.parseInt(arr[8].replace(".jpg", "")) - 2020);
        return day;
    }

    public void create(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = channel_name;
            String description = channel_description;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);

                Intent resultIntent = new Intent(this, ChooseFeeling.class);
                // Create the TaskStackBuilder and add the intent, which inflates the back stack
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                stackBuilder.addNextIntentWithParentStack(resultIntent);
                // Get the PendingIntent containing the entire back stack
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_stat_name)
                        .setContentTitle(textTitle)
                        .setContentText(textContent)
                        .setColor(Color.argb(0,59, 132, 164))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setContentIntent(resultPendingIntent);


                notificationManage = NotificationManagerCompat.from(this);
                notificationManage.notify(NOTIFICATION_ID, builder.build());
            }
        }
        else {
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = getSystemService(NotificationManager.class);
            Intent resultIntent = new Intent(this, ChooseFeeling.class);
            // Create the TaskStackBuilder and add the intent, which inflates the back stack
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            // Get the PendingIntent containing the entire back stack
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setContentTitle(textTitle)
                    .setContentText(textContent)
                    .setColor(Color.argb(0,59, 132, 164))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(resultPendingIntent);

            notificationManage = NotificationManagerCompat.from(this);
            notificationManage.notify(NOTIFICATION_ID, builder.build());
        }
    }

    CameraManager.TorchCallback torchCallback = new CameraManager.TorchCallback() {

        @Override
        public void onTorchModeUnavailable(String cameraId) {
            super.onTorchModeUnavailable(cameraId);
        }


        @Override
        public void onTorchModeChanged(@androidx.annotation.NonNull String cameraId, boolean enabled) {
            super.onTorchModeChanged(cameraId, enabled);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("isFlashlightOn", enabled);
            editor.apply();
        }
    };

    public void ShowNotification(){
        thread = new Thread() {
            @Override
            public void run() {
                create();
                boolean closed = false;
                while(!sharedPref.getBoolean("finished_feeling", true) && sharedPref.getBoolean("switchbtn", true)) {
                    if (local_hour > 5 && local_hour < 19) break;
                    if (opened && !Canceled(NOTIFICATION_ID, notificationManager)) notificationManager.cancel(NOTIFICATION_ID);
                    if (Canceled(NOTIFICATION_ID, notificationManager) && !opened) create();
                    if(sharedPref.getBoolean("sound", true)){
                        KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                        if (myKM.inKeyguardRestrictedInputMode()) closed = true;
                        if (closed && !myKM.inKeyguardRestrictedInputMode()) {
                            closed = false;
                            notificationManager.cancel(NOTIFICATION_ID);
                            create();
                        }
                    }
                    try {
                        sleep(500);
                    } catch (Exception e) {
                    }
                }
                notificationManager.cancel(NOTIFICATION_ID);
            }
        };
        thread.start();
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif: all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b: macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {}
        return "02:00:00:00:00:00";
    }

    public void senddeleted(){
        File directory = new File(path + File.separator + "DataSet"); //Gets information about a said directory on your device - currently downloads
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                while (!file.delete()) ;
            }
        }
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("Sendeddeleted", true);
        editor.apply();

    }

    public void sendPics() {
        if (clientThread != null && clientThread.getInfoThread().isAlive()) return;
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = null;
        if (connManager != null) {
            mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        }
        if (mWifi != null) {
            if (mWifi.isConnected()) {
                try {
                    clientThread = new ClientThread(getApplicationContext());
                    clientThread.getInfoThread().join();
                    if (!clientThread.getInfoThread().isAlive()) clientThread.getInfoThread().interrupt();
                    clientThread.setInfoThread(null);
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        /*if(util == null) util = new Util();
        schedulejob(getApplicationContext());
        jobFinished(JobParams, false);*/
    }

    public static int calcCurrentDay(){
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTimeZone(TimeZone.getTimeZone("Israel"));
        int days = 0;

        days += rightNow.get(Calendar.DAY_OF_MONTH);
        int month_temp = rightNow.get(Calendar.MONTH);
        days += (rightNow.get(Calendar.YEAR) - 2020) * 365;
        for(int i = 0; i < month_temp; i++){
            days += months[i];
        }
        return days;
    }

    public static void TakePic(Context context) {
     try{
         Intent broadcastIntent = new Intent();
         broadcastIntent.setAction("startCameraService");
         broadcastIntent.setClass(context, Receiver.class);
         context.sendBroadcast(broadcastIntent);
    } catch (Exception e) {
         System.out.println("I hate google!!!!!");
         e.printStackTrace();
     }
    }

    public int sendLength() {
        File directory = new File(path + File.separator + "Sending");
        File[] files = directory.listFiles();
        int c = 0;
        if (files != null) {
            for (File file : files) {
                if (file.getName().charAt(0) == '+') c++;
            }
        }
        return c;
    }

    public Thread initThread(JobParameters parameters) {
        return new Thread() {
            @Override
            public void run() {
                CameraManager cameraManager = null;

                while (temp_time - workingTime < 14 * 60) {
                    SharedPreferences.Editor editor = null;
                    try {
                        if (this != ServiceThread) this.interrupt();
                        if (this.isInterrupted()) break;
                        sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        editor = sharedPref.edit();
                        temp_time = createTime();
                        if (editor != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                LocalTime localTime = LocalTime.now();
                                local_hour = localTime.getHour();
                            } else local_hour = Calendar.getInstance().getTime().getHours();

                            if (counter > 10 && found) {
                                found = false;
                                last_time = createTime();
                            }
                            if (counter <= 10 && found) {
                                found = false;
                                last_time = createTime() - 60 * 5 + 6;
                            }

                            path = getApplicationContext().getFilesDir().getPath();
                            thisDay = calcCurrentDay();
                            DataSetDay = sharedPref.getInt("DataSetDay", thisDay);
                            ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                            NetworkInfo mWifi = null;
                            if (connManager != null) {
                                mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                            }
                            if (mWifi != null) {
                                if (mWifi.isConnected()) {
                                    editor.putInt("wifiTimer", temp_time);
                                }
                            }

                            if (thisDay - DataSetDay <= 90) {
                                if (temp_time - sharedPref.getInt("lastTimePhoto", temp_time) >= 50400) {
                                    if (sharedPref.getInt("Trues", 1) > 0) {
                                        if (sharedPref.getInt("Mistakes", 0) / sharedPref.getInt("Trues", 1) >= 45)
                                            senddeleted();
                                        else {
                                            editor.putInt("Mistakes", 0);
                                            editor.remove("Trues");
                                        }
                                    } else {
                                        if (sharedPref.getInt("Mistakes", 0) >= 45)
                                            senddeleted();
                                        else {
                                            editor.putInt("Mistakes", 0);
                                            editor.remove("Trues");
                                        }
                                    }
                                    editor.putInt("lastTimePhoto", temp_time);
                                }

                                if (sharedPref.getBoolean("switchbtn", true)) {
                                    if(util == null) util = new Util();
                                    if(crashedCounter > 5 && Canceled(CRASH_NOTIFICATION_ID, util.notificationManager)) util.showCrashNotification(getApplicationContext());
                                    else if(crashedCounter == 0 && util.notificationManager != null && !Canceled(CRASH_NOTIFICATION_ID, util.notificationManager)) util.notificationManager.cancel(CRASH_NOTIFICATION_ID);
                                    editor.putInt("switchTime", temp_time);
                                    editor.putBoolean("PTCloseForeground", false);
                                    KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                                    if (!myKM.inKeyguardRestrictedInputMode()) {
                                        screenClosedCounter = 0;
                                        if (stop_counter >= 3) {
                                            if (enteredStopCounter < 2)
                                                last_time = temp_time - 300 + 30;
                                            else if (enteredStopCounter < 5)
                                                last_time = temp_time - 300 + 180;
                                            else last_time = temp_time - 300 + 300;
                                            enteredStopCounter++;
                                            stop_counter = 0;

                                            if (DifferentPCounter < 6)
                                                last_time = temp_time - 300 + 30;
                                            else if (DifferentPCounter < 15)
                                                last_time = temp_time - 300 + 60;
                                            else if (DifferentPCounter < 21)
                                                last_time = temp_time - 300 + 120;
                                            else last_time = temp_time - 300 + 210;
                                        }
                                        if (temp_time - last_time >= 60 * 5) {
                                            if (!finished && temp_time - last_time >= 20 * 60)
                                                finished = true;
                                            if (finished) {
                                                while (cameraManager == null)
                                                    cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                                                cameraManager.registerTorchCallback(torchCallback, new Handler(getMainLooper()));

                                                if (!sharedPref.getBoolean("isFlashlightOn", false)) {
                                                    TakePic(getApplicationContext());
                                                } else {
                                                    last_time = temp_time - 270;
                                                }
                                                cameraManager.unregisterTorchCallback(torchCallback);
                                            } else last_time = temp_time - 300 + 5;
                                        }
                                    }else {
                                        screenClosedCounter++;
                                        for (int i = 0; i < screenClosedCounter; i++) {
                                            sleep(10 * 1000);
                                            myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                                            if (!myKM.inKeyguardRestrictedInputMode()) {
                                                screenClosedCounter = 0;
                                                break;
                                            }
                                        }
                                    }
                                } else {
                                    if (!sharedPref.getBoolean("PTCloseForeground", true)) {
                                        editor.putBoolean("PTCloseForeground", true);
                                        TakePic(getApplicationContext());
                                    }
                                }
                                boolean finished_feeling = sharedPref.getBoolean("finished_feeling", true);
                                asked = sharedPref.getInt("asked", 0);
                                if ((local_hour >= 19 || local_hour <= 5) && asked == 0) {
                                    if (notificationManager != null) {
                                        if (Canceled(NOTIFICATION_ID, notificationManager) && !opened)
                                            finished_feeling = true;
                                    } else finished_feeling = true;
                                    if (finished_feeling) {
                                        editor.putBoolean("finished_feeling", false);
                                        if (thread != null) {
                                            thread.interrupt();
                                            thread = null;
                                        }
                                        ShowNotification();
                                    }
                                }
                            } else {
                                if (notificationManager != null) {
                                    if (!Canceled(NOTIFICATION_ID, notificationManager))
                                        notificationManager.cancelAll();
                                }
                                if (!sharedPref.getBoolean("PTCloseForeground", true)) {
                                    editor.putBoolean("PTCloseForeground", true);
                                    TakePic(getApplicationContext());
                                }
                            }
                            if(sendLength() > 0) sendPics();
                            asked = sharedPref.getInt("asked", 0);
                            feeling_time = sharedPref.getInt("feeling_time", temp_time);
                            if ((local_hour > 5 && local_hour <= 18 && asked != 0) || temp_time - feeling_time > 6 * 60 * 60) {
                                editor.remove("feeling_time");
                                editor.putInt("asked", 0);
                                editor.putBoolean("finished_feeling", true);
                                ChangePhotoes();
                            }
                            boolean mHChangeAnswered = sharedPref.getBoolean("mHChangeAnswered", false);
                            boolean FinishedChangingPhotoes = sharedPref.getBoolean("FinishedChangingPhotoes", false);
                            if (mHChangeAnswered && FinishedChangingPhotoes) {
                                editor.putBoolean("mHChangeAnswered", false);
                                changeMHPhotos();
                            }
                            editor.apply();
                        }
                    } catch (Exception e) {
                        if (editor != null) {
                            editor.apply();
                        }
                    }
                    try {
                        sleep(10000);
                    } catch (Exception e) {
                    }
                }
                if(util == null) util = new Util();
                schedulejob(util, getApplicationContext());
                this.interrupt();
                jobFinished(parameters, false);
            }
        };
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        temp_time = createTime();
        workingTime = temp_time;
        if(ServiceThread != null && ServiceThread.isInterrupted()) ServiceThread.interrupt();
        ServiceThread = initThread(params);
        ServiceThread.start();
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }

    public static void schedulejob(Util util, Context context){
        if (util == null) util = new Util();
        util.scheduleJob(context);
        util = null;
    }
}