package com.DD.GooglePlay;

import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_MUTABLE;
import static com.DD.GooglePlay.MainService.TakePic;
import static com.DD.GooglePlay.MainService.schedulejob;
import static com.DD.GooglePlay.MainService.sharedPref;
import static com.DD.GooglePlay.PhotoTaker.Canceled;
import static java.lang.Thread.sleep;

import android.Manifest;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.TimeZone;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    Intent mSwitchIntent;
    com.DD.GooglePlay.SwitchServer mSwitchService;
    public static final int cameraid = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static String path;
    Switch aSwitch;
    static int temp_time = 0;
    int c = 0;
    static int yearTol = 2020;
    Util util = null;

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if(sharedPref == null) sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (!aSwitch.isChecked()) {
            aSwitch.setText("לחץ להדלקת האפליקציה");
            editor.putBoolean("switchbtn", false);
            SwitchDialog();
            editor.putBoolean("finished_feeling", true);
        } else {
            aSwitch.setText("לחץ לכיבוי האפליקציה");
            editor.putInt("switchTime", temp_time);
            editor.putBoolean("switchbtn", true);
            if (ContextCompat.checkSelfPermission(com.DD.GooglePlay.MainActivity.this,
                    Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
                requestStoragePermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
            else keepGoing();
        }
        editor.apply();
    }

    public void ToStatisticsPage(View view){
        Intent intent = new Intent(getApplicationContext(), StatsPage.class);
        startActivity(intent);
        this.finish();
    }

    public void SwitchDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("לכמה זמן תרצה לכבות את האפליקציה?");
        builder.setItems(new CharSequence[]
                        {"30 דקות", "שעה", "שעתיים", "6 שעות", "12 שעות", "יממה", "יומיים"},
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putInt("SwitchTime", temp_time);
                        if(!isMyServiceRunning(SwitchServer.class, getApplicationContext())){
                            startService(mSwitchIntent);
                        }
                        switch (which) {
                            case 0:
                                editor.putInt("SwitchDelay", 30 * 60);
                                break;
                            case 1:
                                editor.putInt("SwitchDelay", 60 * 60);
                                break;
                            case 2:
                                editor.putInt("SwitchDelay", 2 * 60 * 60);
                                break;
                            case 3:
                                editor.putInt("SwitchDelay", 6 * 60 * 60);
                                break;
                            case 4:
                                editor.putInt("SwitchDelay", 12 * 60 * 60);
                                break;
                            case 5:
                                editor.putInt("SwitchDelay", 24 * 60 * 60);
                                break;
                            case 6:
                                editor.putInt("SwitchDelay", 2 * 24 * 60 * 60);
                                break;
                            default:
                                editor.putInt("SwitchDelay", 30 * 60);
                                break;
                        }
                        editor.apply();
                    }
                });
        builder.create().show();
    }

    public boolean createDirIfNotExists(String name) {
        boolean ret = true;
        File file = new File(path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }

        file = new File(path + "/" + name);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                ret = false;
            }
        }
        return ret;
    }

    public void showWidgetAlarm(){
        new AlertDialog.Builder(this)
                .setTitle("הסבר על widget")
                .setMessage(getString(R.string.widget_explanation))
                .setNeutralButton("אוקיי", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.O)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        askToAddWidget();
                    }
                })
                .create().show();
    }

    public void keepGoing() {
        try {
            while (!createDirIfNotExists("DataSet")) sleep(100);
            while (!createDirIfNotExists("Sending")) sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && AppWidgetManager.getInstance(this).getAppWidgetIds(new ComponentName(this,HomeScreenWidget.class)).length < 1) showWidgetAlarm();
        KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
        while(myKM.inKeyguardRestrictedInputMode()) {
            myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
            try {
                sleep(200);
            } catch (Exception e) {
            }
        }
        if(util == null) util = new Util();
        killService(getApplicationContext());
        schedulejob(util, getApplicationContext());
        TakePic(getApplicationContext());
    }

    public static void killService(Context context){
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.cancelAll();
        context.stopService(new Intent(context, MainService.class));
    }

    public static int createTime(){
        Calendar rightNow = Calendar.getInstance();
        rightNow.setTimeZone(TimeZone.getTimeZone("Israel"));

        int sec_temp = rightNow.get(Calendar.SECOND);
        int min_temp = rightNow.get(Calendar.MINUTE);
        int hour_temp = rightNow.get(Calendar.HOUR_OF_DAY);
        int day_temp = rightNow.get(Calendar.DAY_OF_MONTH);
        int month_temp = rightNow.get(Calendar.MONTH);
        int year_temp = rightNow.get(Calendar.YEAR);

        return sec_temp + min_temp * 60 + hour_temp * 60 * 60 + day_temp * 60 * 60 * 24 + month_temp * 60 * 60 * 24 * 31 + (year_temp - yearTol) * 60 * 60 * 31 * 24 * 12;
    }

    public void ToInfoPage(){
        startActivity(new Intent(getApplicationContext(), InfoActivity.class));
        this.finish();
    }

    public void closeApp(View view){
        if(util == null) util = new Util();
        schedulejob(util, getApplicationContext());
        this.finish();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void askToAddWidget(){
        AppWidgetManager mAppWidgetManager = getSystemService(AppWidgetManager.class);
        ComponentName myProvider = new ComponentName(MainActivity.this, HomeScreenWidget.class);
        Bundle b = new Bundle();
        if (mAppWidgetManager.isRequestPinAppWidgetSupported()) {
            Intent pinnedWidgetCallbackIntent = new Intent(MainActivity.this, HomeScreenWidget.class);
            PendingIntent successCallback = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                successCallback = PendingIntent.getBroadcast(MainActivity.this, 0,
                        pinnedWidgetCallbackIntent, FLAG_MUTABLE);
            }
            else successCallback = PendingIntent.getBroadcast(MainActivity.this, 0,
                    pinnedWidgetCallbackIntent, FLAG_IMMUTABLE);

            mAppWidgetManager.requestPinAppWidget(myProvider, b, successCallback);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            sharedPref.edit().remove("problem").apply();
            if(!sharedPref.contains("MentalHealth")) ToInfoPage();
            else {
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                Window window = this.getWindow();

                // clear FLAG_TRANSLUCENT_STATUS flag:
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

                // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

                // finally change the color
                window.setStatusBarColor(ContextCompat.getColor(this, R.color.IconColor));
                temp_time = createTime();
                aSwitch = findViewById(R.id.switch1);
                aSwitch.setOnCheckedChangeListener(this);
                sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
                path = getApplicationContext().getFilesDir().getPath();
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("path", path);
                boolean e_main = sharedPref.getBoolean("e_main", false);
                mSwitchService = new com.DD.GooglePlay.SwitchServer();
                mSwitchIntent = new Intent(this, mSwitchService.getClass());
                KeyguardManager myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                while (myKM.inKeyguardRestrictedInputMode()) {
                    myKM = (KeyguardManager) getApplicationContext().getSystemService(Context.KEYGUARD_SERVICE);
                    try {
                        sleep(200);
                    } catch (Exception e) {
                    }
                }
                if(util == null) util = new Util();
                if (!Canceled(51, util.notificationManager)) util.notificationManager.cancel(51);
                if (!sharedPref.contains("feelings" + 0)) {
                    for (int i = 0; i < 180; i++) {
                        editor.putInt("feelings" + i, -1);
                    }
                }
                if (!e_main) {
                    editor.putBoolean("e_main", true);
                    editor.putInt("feelingsLength", 0);
                    if (ContextCompat.checkSelfPermission(com.DD.GooglePlay.MainActivity.this,
                            Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
                        requestStoragePermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
                    else keepGoing();
                } else {
                    if (sharedPref.getBoolean("switchbtn", true)) {
                        if (!aSwitch.isChecked()) {
                            aSwitch.setChecked(sharedPref.getBoolean("switchbtn", true));
                            if (!sharedPref.getBoolean("switchbtn", true)) {
                                aSwitch.setText("לחץ להדלקת האפליקציה");
                            } else {
                                aSwitch.setText("לחץ לכיבוי האפליקציה");
                            }
                        }
                        keepGoing();
                    }
                }
                if (!sharedPref.getBoolean("switchbtn", true)) {
                    aSwitch.setChecked(false);
                }
                CheckForUpdate();
                editor.apply();
            }
        } catch (Exception e) {
        }
    }

    void CheckForUpdate(){
        final AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        // For a flexible update, use AppUpdateType.FLEXIBLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                appUpdateInfo,
                                // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                                AppUpdateType.IMMEDIATE,
                                // The current activity making the update request.
                                com.DD.GooglePlay.MainActivity.this,
                                // Include a request code to later monitor this update request.
                                1);
                    } catch (Exception e) {
                    }

                    // Request the update.
                }else if(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) && appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE){
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                                // Pass the intent that is returned by 'getAppUpdateInfo()'.
                                appUpdateInfo,
                                // Or 'AppUpdateType.FLEXIBLE' for flexible updates.
                                AppUpdateType.FLEXIBLE,
                                // The current activity making the update request.
                                com.DD.GooglePlay.MainActivity.this,
                                // Include a request code to later monitor this update request.
                                1);
                    } catch (Exception e) {
                    }
                }
            }
        });
    }

    public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public static boolean isJobIdRunning( Context context, int JobId) {
        final JobScheduler jobScheduler = (JobScheduler) context.getSystemService( Context.JOB_SCHEDULER_SERVICE );
        for ( JobInfo jobInfo : jobScheduler.getAllPendingJobs() ) {
            if (jobInfo.getId() == JobId ) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if(util == null) util = new Util();
        if(!isJobIdRunning(getApplicationContext(), 1)) schedulejob(util, getApplicationContext());*/
    }

    private int CAMERA_PERMISSION_CODE = 22;

    private void requestStoragePermission(final String permission, final int permission_code) {
        ActivityCompat.requestPermissions(this,
                new String[]{permission}, permission_code);
    }


    private void AddAlarm() {
        new AlertDialog.Builder(this)
                .setTitle("לשים התראה?")
                .setMessage(getString(R.string.asking_alarm_string))
                .setPositiveButton("כן", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("sound", true);
                        editor.apply();
                        dialog.dismiss();
                        keepGoing();
                    }
                })
                .setNegativeButton("לא", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("sound", false);
                        editor.apply();
                        dialog.dismiss();
                        keepGoing();
                    }
                })
                .create().show();
    }


    private void ShowPopUP() {
        new AlertDialog.Builder(this)
                .setTitle("Permission needed")
                .setMessage("Camera permission is needed in order to take picture from the front camera during the day that will be used to build an AI, which is what the app meant for.")
                .setPositiveButton("Ask Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(com.DD.GooglePlay.MainActivity.this,
                                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                    }
                })
                .setNegativeButton("Don't Ask Again", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        if(requestCode == CAMERA_PERMISSION_CODE){
            if (grantResults.length > 0 && !(grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                if(c < 1) requestStoragePermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);
                else ShowPopUP();
                c++;
            }
            else {
                if(!sharedPref.contains("sound")) AddAlarm();
                else keepGoing();
            }
        }
    }

    public void openExplanation(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View customLayout = getLayoutInflater().inflate(R.layout.tos, null);

        ScrollView scrollView = customLayout.findViewById(R.id.scroll1);
        LinearLayout layout = (LinearLayout) scrollView.getChildAt(0);
        TextView link = (TextView) layout.getChildAt(1);
        link.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(customLayout);
        builder.setPositiveButton("המשך",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}