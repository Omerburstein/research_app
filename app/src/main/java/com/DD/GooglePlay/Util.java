package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.isJobIdRunning;
import static com.DD.GooglePlay.MainActivity.isMyServiceRunning;
import static com.DD.GooglePlay.PhotoTaker.crashedCounter;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class Util {

    NotificationManager notificationManager;
    NotificationCompat.Builder builder;
    Intent resultIntent;
    TaskStackBuilder stackBuilder;
    NotificationManagerCompat notificationManage;
    PendingIntent resultPendingIntent;

    String CHANNEL_ID = "5";
    String channel_description = "Open App";
    String channel_name = "BackUpChannel";
    String textTitle = "App Crashed";
    String textContent = "Please enter the app to restart it";
    final static int CRASH_NOTIFICATION_ID = 51;
    Thread UtilThread;

    public Util() {
        builder = null;
        resultIntent = null;
        stackBuilder = null;
        notificationManage = null;
        resultPendingIntent = null;
    }

    public void showCrashNotification(Context context) {
        if (builder == null || notificationManage == null) InitializeNot(context);
        if (notificationManage != null && builder != null)
            notificationManage.notify(CRASH_NOTIFICATION_ID, builder.build());
    }

    public void InitializeNot(Context context) {
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        notificationManager = context.getSystemService(NotificationManager.class);
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        stackBuilder = TaskStackBuilder.create(context);
        notificationManage = NotificationManagerCompat.from(context);
        resultIntent = new Intent(context, com.DD.GooglePlay.MainActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = channel_name;
            String description = channel_description;
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_name)
                .setContentTitle(textTitle)
                .setContentText(textContent)
                .setColor(Color.argb(0, 59, 132, 164))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(resultPendingIntent)
                .setOngoing(true);
        notificationManage = NotificationManagerCompat.from(context);
    }

    // schedule the start of the service every 10 - 30 seconds
    public void scheduleJob(Context context, Class<?> tClass, int jobId) {
        try {
            UtilThread = new Thread() {
                @Override
                public void run() {
                    ComponentName serviceComponent = new ComponentName(context, tClass);
                    JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);
                    builder.setPeriodic(25 * 60 * 1000); // wait at least
                    builder.setPersisted(true);
                    //builder.setOverrideDeadline(5 * 1000);
                    //builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
                    //builder.setRequiresDeviceIdle(true); // device should be idle
                    //builder.setRequiresCharging(false); // we don't care if the device is charging or not
                    JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
                    int result_code = JobScheduler.RESULT_FAILURE;
                    if (jobScheduler != null) {
                        result_code = jobScheduler.schedule(builder.build());
                    }
                    if(crashedCounter > 5) showCrashNotification(context);
                    else notificationManager.cancel(CRASH_NOTIFICATION_ID);
                    if (result_code == JobScheduler.RESULT_FAILURE)
                            scheduleJob(context, tClass, jobId);
                }
            };
            UtilThread.start();
        } catch (Exception e) {
            showCrashNotification(context);
        }
    }

    public void scheduleJob(Context context) {
        try {
            if (builder == null || notificationManage == null) InitializeNot(context);
                if(!isJobIdRunning(context, 1) && !isMyServiceRunning(MainService.class, context)) {
                    if (UtilThread != null) {
                        UtilThread.interrupt();
                        UtilThread = null;
                    }
                    scheduleJob(context, MainService.class, 1);
                }
        } catch (Exception e) {
            showCrashNotification(context);
        }
    }
}