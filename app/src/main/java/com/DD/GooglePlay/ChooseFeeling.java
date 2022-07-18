package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.isJobIdRunning;
import static com.DD.GooglePlay.MainActivity.killService;
import static com.DD.GooglePlay.MainActivity.temp_time;
import static com.DD.GooglePlay.MainService.NOTIFICATION_ID;
import static com.DD.GooglePlay.MainService.TakePic;
import static com.DD.GooglePlay.MainService.changeMHPhotos;
import static com.DD.GooglePlay.MainService.opened;
import static com.DD.GooglePlay.MainService.schedulejob;
import static com.DD.GooglePlay.MainService.sharedPref;

import static java.lang.Thread.sleep;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ChooseFeeling extends AppCompatActivity implements AdapterView.OnItemSelectedListener{
    final String[] mHChange = new String[]{"", "הדרדר", "לא השתנה", "השתפר"};
    ArrayAdapter<String> mHChangeAdapter;
    Spinner mHSpinner;
    long time;
    boolean mHchoose = false;
    int currentPosition;

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
                                com.DD.GooglePlay.ChooseFeeling.this,
                                // Include a request code to later monitor this update request.
                                2);
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
                                com.DD.GooglePlay.ChooseFeeling.this,
                                // Include a request code to later monitor this update request.
                                2);
                    } catch (Exception e) {
                    }
                }
            }
        });
    }
    RadioGroup radioGroup;
    boolean clicked;

    private void PleaseOpenWifiMes() {
        new AlertDialog.Builder(this)
                .setTitle("Please Connect to Wifi")
                .setMessage("Please connect to a wifi network, to allow us send the pictures taken by the app.")
                .setNeutralButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create().show();
    }

    public void showMHQuestion(){
        mHchoose = false;
        mHSpinner = findViewById(R.id.mentalHealthChange);
        mHChangeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, mHChange);
        mHSpinner.setAdapter(mHChangeAdapter);
        mHSpinner.setOnItemSelectedListener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            if(sharedPref == null) sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            time = temp_time;
            killService(getApplicationContext());
            schedulejob(new Util(), getApplicationContext());
            TakePic(getApplicationContext());
            setContentView(R.layout.choosefeeling);
            TextView mHText = findViewById(R.id.mHTextView);
            Spinner mHSpinner = findViewById(R.id.mentalHealthChange);
            Window window = this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.IconColor));

            // Creates instance of the manager.
            CheckForUpdate();
            if (sharedPref == null) PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putBoolean("FinishedChangingPhotoes", false);
            if (temp_time - sharedPref.getInt("wifiTimer", 0) >= 7 * 24 * 60 * 60) {
                PleaseOpenWifiMes();
                editor.putInt("wifiTimer", temp_time);
            }
            editor.apply();
            if (temp_time - sharedPref.getInt("mHChangeTime", 0) >= 7 * 24 * 60 * 60) {
                mHText.setVisibility(View.VISIBLE);
                mHSpinner.setVisibility(View.VISIBLE);
                showMHQuestion();
            } else mHchoose = true;
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            clicked = false;
            opened = true;
            radioGroup = findViewById(R.id.radioGroup);
        } catch (Exception e) {
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        try {
            mHchoose = true;
            currentPosition = position;
        } catch (Exception e) {
        }
    }

    public void setMHValues(SharedPreferences.Editor editor){
        editor.putInt("mentalHealthChange", currentPosition);
        editor.putInt("mHChangeTime", temp_time);
        editor.putBoolean("mHChangeAnswered", true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void Finish(View view) {
        try {
            if(temp_time - time < 1 * 1000) sleep(1 * 1000 - (temp_time - time));
            if (clicked && mHchoose) {
                SharedPreferences.Editor editor = sharedPref.edit();
                int feeling = Integer.parseInt(String.valueOf(((RadioButton) findViewById(radioGroup.getCheckedRadioButtonId())).getText()));
                int feelingsLength = sharedPref.getInt("feelingsLength", 0);
                editor.putInt("feelings" + feelingsLength, feeling);
                Calendar rightNow = Calendar.getInstance();
                rightNow.setTimeZone(TimeZone.getTimeZone("Israel"));
                editor.putLong("Days" + feelingsLength, new Date(rightNow.get(Calendar.YEAR) - 1900, rightNow.get(Calendar.MONTH), rightNow.get(Calendar.DAY_OF_MONTH)).getTime());
                editor.putInt("feelingsLength", ++feelingsLength);
                editor.putInt("feelingChosenHour", rightNow.get(Calendar.HOUR_OF_DAY));
                feeling--;
                editor.putInt("feeling" + (sharedPref.getInt("feelingIndex", 0) + 1), feeling);
                editor.putInt("feelingIndex", sharedPref.getInt("feelingIndex", 0) + 1);
                editor.putInt("asked", 1);
                editor.putBoolean("finished_feeling", true);
                if(temp_time - sharedPref.getInt("mHChangeTime", 0) >= 7 * 24 * 60 * 60) setMHValues(editor);
                editor.apply();
                opened = false;
                this.finish();
            }
        } catch (Exception e) {
        }
    }

    public void onClick(View view) {
        try {
            radioGroup.clearCheck();
            RadioButton radioButton = (RadioButton) view;
            radioButton.setChecked(true);
            clicked = true;
        } catch (Exception e) {
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        opened = false;
    }
}