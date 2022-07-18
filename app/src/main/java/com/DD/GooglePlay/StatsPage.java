package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.temp_time;
import static com.DD.GooglePlay.MainService.sharedPref;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StatsPage extends AppCompatActivity {

    GraphView graphView;
    SimpleDateFormat sdf = new SimpleDateFormat("EE");
    SimpleDateFormat sdf2 = new SimpleDateFormat("D");
    int feelingsLength;
    int j;
    static Date[] dates;
    Date[] days;
    Date date;

    public static Date[] Load(){
        int i = 0;
        dates = new Date[180];
        while(i < sharedPref.getInt("feelingsLength", 0)){
            if(sharedPref.contains("Days" + i)){
                dates[i] = new Date(sharedPref.getLong("Days" + i, temp_time));
            }
            i++;
        }
        return dates;
    }

    public void BackToMain(View view){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        this.finish();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stats_layout);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        Window window = this.getWindow();
        // clear FLAG_TRANSLUCENT_STATUS flag:
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        // finally change the color
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.IconColor));
        try {
            graphView = findViewById(R.id.graphid);
            days = Load();
            feelingsLength = sharedPref.getInt("feelingsLength", 0);
            if (feelingsLength > 0) {
                Date FirstDate = days[feelingsLength - 1];
                for (j = feelingsLength - 14; j < feelingsLength; j++) {
                    if (j >= 0) {
                        date = days[j];
                        if (Integer.parseInt(sdf2.format(FirstDate)) - Integer.parseInt(sdf2.format(date)) <= 7) {
                            break;
                        }
                    }
                }
                date = days[j];
                Date NextDate = date;
                DataPoint[] dataPoint = new DataPoint[feelingsLength - j];
                int i = j;
                int counter = 0;
                while (i < feelingsLength) {
                    if (sharedPref.getInt("feelings" + i, -1) == -1) {
                        System.arraycopy(dataPoint, j, dataPoint, j, i - j - counter);
                        break;
                    }
                    date = days[i];
                    if (i + 1 < feelingsLength) NextDate = days[i + 1];
                    if (date.getTime() == NextDate.getTime()) {
                        if (sharedPref.getInt("feelings" + (i + 1), -1) != -1) {
                            dataPoint[i - j - counter] = new DataPoint(date, (sharedPref.getInt("feelings" + i, -1) + sharedPref.getInt("feelings" + (i + 1), -1)) / 2.0f);
                            i++;
                            counter++;
                        } else
                            dataPoint[i - j - counter] = new DataPoint(date, sharedPref.getInt("feelings" + i, -1));
                    } else
                        dataPoint[i - j - counter] = new DataPoint(date, sharedPref.getInt("feelings" + i, -1));
                    i++;
                }
                if (counter != 0) {
                    DataPoint[] d = new DataPoint[dataPoint.length - counter];
                    for (i = 0; i < dataPoint.length - counter; i++) {
                        d[i] = dataPoint[i];
                    }
                    dataPoint = d;
                }
                Date minDay = days[j];
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPoint);
                graphView.addSeries(series);
                graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                    @Override
                    public String formatLabel(double value, boolean isValueX) {
                        if (isValueX) {
                            return sdf.format(new Date((long) value));
                        } else {
                            return super.formatLabel(value, isValueX);
                        }
                    }
                });

                graphView.getGridLabelRenderer().setHumanRounding(false);
                graphView.getViewport().setMinY(1);
                graphView.getViewport().setMaxY(10);
                graphView.getViewport().setMaxX(FirstDate.getTime());
                graphView.getViewport().setMinX(minDay.getTime());
                graphView.getViewport().setYAxisBoundsManual(true);
                graphView.getViewport().setXAxisBoundsManual(true);
                int hori = 1 + Integer.parseInt(sdf2.format(FirstDate)) - Integer.parseInt(sdf2.format(minDay));
                if(hori > 1) graphView.getGridLabelRenderer().setNumHorizontalLabels(hori);
                else graphView.getGridLabelRenderer().setNumHorizontalLabels(2);
                graphView.getGridLabelRenderer().setNumVerticalLabels(10);
                double sum = 0;
                for (i = 0; i < dataPoint.length; i++) {
                    sum += dataPoint[i].getY();
                }
                TextView moodText = findViewById(R.id.AvgMood);
                double avg = (sum / dataPoint.length);

                if (dataPoint.length > 0) moodText.setText("מצב הרוח הממוצע: " + (int)avg + "." + (int)((avg * 10)%10));
                float[] scores = {0,0,0,0,0,0,0};
                int[] counters = {0,0,0,0,0,0,0};
                i = 0;
                while (days[i] != null) {
                    scores[days[i].getDay()] += sharedPref.getInt("feelings" + i, 0);
                    counters[days[i].getDay()]++;
                    i++;
                }
                for(int day = 0; day < scores.length; day++){
                    scores[day] = scores[day]/counters[day];
                }
                float Best = scores[0];
                int BestIndex = 0;
                for (i = 0; i < 7; i++) {
                    if (Best < scores[i]) {
                        Best = scores[i];
                        BestIndex = i;
                    }
                }
                TextView BestScoreDay = findViewById(R.id.BestDay);
                String[] weekdays = new DateFormatSymbols().getWeekdays();
                BestScoreDay.setText("היום הטוב ביותר בשבוע לך: " + weekdays[BestIndex + 1]);
            }
        } catch (Exception e) {
        }
    }
}