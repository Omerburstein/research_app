package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainService.sharedPref;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;

import com.amazonaws.RequestClientOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;


public class Questionnaire extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    final String parentTextView = "שם ההורה/ אפוטרופוס:";
    final String childTextView = "שם המשתתף";

    final String parentString = "\n \n" + "אני מאשר/ת  שקראתי את דף ההסבר למחקר ושניתנה לי האפשרות לברר את המידע המופיע בו עם צוות המחקר, אשר התחייב/ה בפני בכתב, להבטיח סודיות בכל הנוגע לפרטים האישיים  של בני/בתי/החסוי/החסויה ולכל פרט אחר, שעלול לחשוף את זהותו/ה.\n" +
            "בדף ההסבר למחקר הובהרו לי הדברים הבאים:\n" +
            "•\tמטרת המחקר וחשיבותו.\n" +
            "•\tההשתתפות במחקר הינה מבחירה בלבד, ושהיא מותנית בהסכמת בני/בתי/החסוי/החסויה להשתתף.\n" +
            "•\tבני/בתי/החסוי/החסויה יכול/ה להפסיק את השתתפותו/ה י בכל שלב שהוא, ולא תהיה לכך השלכה כלשהי למעט אי קבלת 15 שעות של מחויבות אישית.\n" +
            "אני מצהיר/ה כי הנני מסכימ/ה להשתתפות בני/בתי/החסוי/החסויה במחקר זה ומוכנ/ה שייעשה שימוש בשאלון/תמונות/נתונים שיאספו לצורכי מחקר בלבד.\n";

    final String childString = "\n \n" + "אני מאשר/ת  את הדברים הבאים:\n" +
            "•\tקראתי את דף ההסבר ושניתנה לי האפשרות לברר את המידע המופיע בו עם צוות המחקר.\n" +
            "•\tנותנ/ת את הסכמתי להשתתפותי במחקר.\n" +
            "•\tמבינ/ה כי אני יכול/ה להפסיק את השתתפותי בכל שלב שהוא, ולא תהיה לכך השלכה כלשהי למעט אי קבלת 15 שעות של מחויבות אישית.  \n";

    final String[][] HebrewAnswers = {{"איני מרגיש/ה את עצמי עצוב/ה", "אני עצוב/ה", "אני עצוב/ה כל הזמן ואינני יכול/ה לצאת מזה", "אני כה עצוב/ה או אומלל/ה עד כי אינני יכול/ה לשאת זאת"}, {"איני מיואש/ת במיוחד ביחס לעתיד", "אני מיואש/ת לגבי העתיד", "אני חש/ה שאין לי למה לצפות", "אני מרגיש/ה שהעתיד הוא חסר תקווה ושהדברים לא יכולים להשתפר"}, {"איני מרגיש/ה ככשלון", "אני מרגיש/ה שנכשלתי יותר מהאדם הממוצע", " בהביטי לאחור על חיי כל מה שאני רואה הוא כשלונות רבים", "אני מרגיש/ה שאני כישלון גמור כאדם"}, {"אני מפיק/ה את אותה ההנאה מדברים", "איני נהנה/ית מדברים כפי שנהניתי", "איני מקבל/ת יותר סיפוק משום דבר", "אני בלתי מרוצה או משועמם/ת מכל דבר"},
            {"איני חש/ה אשם/ה במיוחד", "אני חש/ה אשם/ה חלק גדול מהזמן", "אני מרגיש/ה אשם/ה רוב הזמן", "אני חש/ה אשם/ה כל הזמן"}, {"איני חש/ה את עצמי מוענש/ת", "אני מרגיש/ה שאני עלול/ה להיענש", "אני צופה להיענש", "אני חש/ה שאני מוענש/ת"}, {"אינני מאוכזב/ת מעצמי", "אני מאוכזב/ת מעצמי", "אני נגעל/ת מעצמי", "אני שונא/ית את עצמי"}, {"איני מרגיש/ה שאני רע/ה יותר מאדם אחר", "אני ביקורתי/ת מאד ביחס לעצמי בשל החולשות והשגיאות שלי", "אני מאשים/מה את עצמי כל הזמן על חסרונותי", "אני מאשים/מה את עצמי על כל דבר רע שקורה"},
            {"אין לי כל מחשבות להרוג את עצמי", "יש לי מחשבות להרוג את עצמי, אך לא אבצע אותן", "הייתי רוצה להרוג את עצמי", "הייתי הורג/ת את עצמי אילו היתה לי ההזדמנות"}, {"איני בוכה יותר מהרגיל", "אני בוכה עכשיו יותר מאשר בעבר", "אני בוכה כל הזמן עכשיו", "בעבר יכולתי לבכות, אולם עתה איני יכול/ה לבכות אפילו שאני רוצה בכך"}, {"אני לא מרוגז/ת עכשיו יותר מאשר בדרך כלל", "אני מתרגז/ת עתה ביתר קלות מאשר בעבר", "אני מרוגז/ת כל הזמן", "איני מתרגז/ת בכלל בשל דברים אשר הרגיזו אותי בעבר"}, {"לא איבדתי עניין באנשים אחרים", "אני מתעניין/נת עכשיו באנשים פחות מאשר בעבר", "איבדתי את רוב התעניינותי באנשים", "איבדתי כל עניין באנשים"},
            {"אני מגיע/ה להחלטות כמו תמיד", "אני משתדל/ת לדחות החלטות יותר משעשיתי זאת בעבר", "אני מתקשה להגיע להחלטות יותר מבעבר", "איני יכול/ה להגיע להחלטות בכלל"},
            {"אני לא מרגיש/ה שאני נראה/ית גרוע יותר מאשר נראיתי בעבר", "אני מודאג/ת מכך שאני נראה/ית מבוגר/ת מכפי גילי או בלתי מושך/ת", "אני מרגיש/ה שחלים שינויים בהופעתי ושהם גורמים לי להרגיש בלתי מושך/ת", "אני מרגיש/ה שאני מכוער/ת"}, {"אני יכול/ה לעבוד כמו בעבר", "נדרש ממני מאמץ מיוחד כדי להתחיל לעשות משהו", "עלי לדחוף את עצמי מאד כדי להתחיל לעשות משהו", "אני לא מסוגל/ת לעבוד כלל"}, {"אני יכול/ה לישון כרגיל", "איני ישן/ה טוב כבעבר", "אני מתעורר/ת בשעה שעתיים מוקדם מהרגיל ומתקשה להירדם שוב", "אני מתעורר/ת מספר שעות מוקדם יותר מבעבר ואיני יכול/ה להירדם שוב"}, {"איני מתעייף/ת יותר מהרגיל", "אני מתעייף/ת ביתר קלות מאשר בעבר", "אני מתעייף/ת מאי עשית כמעט מאומה", "אני עייף/ה מדי מכדי שאוכל לעשות משהו"},
            {"תיאבוני אינו גרוע מהרגיל", "תיאבוני אינו טוב כפי שהיה", "תיאבוני הרבה יותר גרוע עכשיו", "אין לי יותר תיאבון כלל"}, {"לא הפסדתי הרבה ממשקלי, אם בכלל, לאחרונה / אני מנסה להוריד ממשקלי במתכוון ע\"י אכילה פחותה", "לאחרונה הפסדתי למעלה מ 2.5 ק\"ג ממשקלי", "לאחרונה הפסדתי למעלה מ 5 ק\"ג ממשקלי", "לאחרונה הפסדתי למעלה מ 7.5 ק\"ג ממשקלי"}, {"אני לא מודאג/ת מבריאותי יותר מהרגיל", "אני מוטרד/ת מבעיות גופניות כגון: מיחושים וכאבים, קיבה לא סדירה או עצירות", "אני מוטרד/ת מבעיות גופניות וקשה לי לחשוב על משהו אחר", "אני מוטרד/ת מבעיות גופניות וקשה לי לחשוב על משהו אחר"}, {"לא הבחנתי בשינוי כלשהו בהתעניינותי במין לאחרונה", "אני פחות מעוניין/ת במין כבעבר", "אני הרבה פחות מעוניין/ת במין עכשיו", "איבדתי לחלוטין כל עניין במין"}};


    String[][] answers = HebrewAnswers;

    final int[] answersIds = {R.id.answer11, R.id.answer12, R.id.answer13, R.id.answer14};

    int page = -2;
    int counter = 0;

    int[] choices = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    int[] optionsId = {R.id.op11, R.id.op12, R.id.op13, R.id.op14};

    RadioGroup question;

    Spinner sexSpinner;
    final String[] SexOption = new String[]{"", "זכר", "נקבה", "אחר"};
    ArrayAdapter<String> SAdapter;

    private NumberPicker agePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.questionnaire);
        Window window = this.getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.IconColor));

        question = findViewById(R.id.options);

        sexSpinner = findViewById(R.id.sex);
        SAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SexOption);
        sexSpinner.setAdapter(SAdapter);
        sexSpinner.setOnItemSelectedListener(this);


    }

    public void NextBtn(View view){
        choices[page] = question.indexOfChild(question.findViewById(question.getCheckedRadioButtonId()));
        page++;
        movePage();
    }

    public void movePage(View view){
        page++;
        sharedPref.edit().putInt("age", agePicker.getValue()).apply();
        agePicker.setVisibility(View.INVISIBLE);
        findViewById(R.id.ageText).setVisibility(View.INVISIBLE);
        findViewById(R.id.continueBtn).setVisibility(View.INVISIBLE);
        if(agePicker.getValue() < 18 && !sharedPref.contains("parentName")) {
            //findViewById(R.id.backBtn).setVisibility(View.INVISIBLE);
            showDialog(parentString, "", parentTextView);
        }
        else if(!sharedPref.contains("childName")){
            showDialog(childString, "", childTextView);
            /*
            findViewById(R.id.question).setVisibility(View.VISIBLE);
            movePage();*/
        }
        else{
            findViewById(R.id.question).setVisibility(View.VISIBLE);
            movePage();
        }
    }

    public void showDialog(String explanation, String name, String viewString){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // set the custom layout
        final View customLayout = getLayoutInflater().inflate(R.layout.get_approvals, null);
        ScrollView scrollView = customLayout.findViewById(R.id.scroll);
        ConstraintLayout layout = (ConstraintLayout) scrollView.getChildAt(0);
        TextView textView = (TextView) layout.getChildAt(0);
        textView.setText(explanation);

        EditText editText = customLayout.findViewById(R.id.parentName);
        editText.setText(name);

        TextView nameTextView = (TextView) layout.getChildAt(1);
        nameTextView.setText(viewString);

        builder.setView(customLayout);

        // add a button
        builder.setPositiveButton("המשך",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // send data from the
                        // AlertDialog to the Activity
                        SignitureView signitureView = customLayout.findViewById(R.id.parentSig);
                        EditText editText = customLayout.findViewById(R.id.parentName);
                        if(!signitureView.path.isEmpty()) {
                            if(!editText.getText().toString().equals("")) {
                                if (explanation.equals(parentString)) {
                                    signitureView.saveSignature(getApplicationContext(), "parentSignature");
                                    sharedPref.edit().putString("parentName", new String(editText.getText().toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)).apply();
                                    showDialog(childString, "", childTextView);
                                } else {
                                    signitureView.saveSignature(getApplicationContext(), "childSignature");
                                    try {
                                        sharedPref.edit().putString("childName", new String(editText.getText().toString().getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1)).apply();
                                    } catch (Exception e) {
                                    }
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
                                    String currentDateandTime = sdf.format(new Date());
                                    sharedPref.edit().putString("date", currentDateandTime).apply();
                                    findViewById(R.id.question).setVisibility(View.VISIBLE);
                                    movePage();
                                }
                            }else{
                                showDialog(explanation, name, viewString);
                            }
                        }else{
                            showDialog(explanation, editText.getText().toString(), viewString);
                        }
                    }
                });
        builder.setNegativeButton("נקה חתימה", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // send data from the
                // AlertDialog to the Activity
                SignitureView signitureView = customLayout.findViewById(R.id.parentSig);
                signitureView.clearSign();
                EditText editText = customLayout.findViewById(R.id.parentName);
                showDialog(explanation, editText.getText().toString(), viewString);
            }
        });
        // create and show
        // the alert dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void movePage() {
        for(int j = 0; j < 4; j++) Log.d("heights", j + ": " + findViewById(answersIds[j]).getHeight());
        if (page == 21) {
            for (int choice : choices) counter += choice;
            calcScore(counter);
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            this.finish();
        } else if(page >= 0) {
            TextView answer;
            question.clearCheck();
            if (choices[page] != -1) question.check(optionsId[choices[page]]);
            for (int j = 0; j < 4; j++) {
                answer = findViewById(answersIds[j]);
                answer.setText(answers[page][j]);
            }
            setHeights();
        }
    }

    public void setHeights(){
        TextView answer;
        RadioButton radioButton;
        for(int i =0; i < 4; i++) {
            answer = findViewById(answersIds[i]);
            radioButton = (RadioButton) question.getChildAt(i);
            radioButton.setHeight(answer.getLineCount() * (answer.getLineHeight() + 9));
        }
    }

    public void calcScore(int score){
        sharedPref.edit().putString("MentalHealth", String.valueOf(score)).apply();
    }

    public void prevPage(View view){
        if(page > 0) {
            //Submit results
            choices[page] = question.indexOfChild(question.findViewById(question.getCheckedRadioButtonId()));

            page--;
            TextView q;
            TextView answer;
            question.clearCheck();
            question.check(optionsId[choices[page]]);
            for (int j = 0; j < 4; j++) {
                answer = findViewById(answersIds[j]);
                answer.setText(answers[page][j]);
            }
            setHeights();
        }
        else if(page == 0){
            page--;
            setVisibilities();
        }
        else{
            page--;
            sexSpinner.setVisibility(View.VISIBLE);
            findViewById(R.id.sexText).setVisibility(View.VISIBLE);
            findViewById(R.id.ageText).setVisibility(View.INVISIBLE);
            findViewById(R.id.continueBtn).setVisibility(View.INVISIBLE);
            agePicker = findViewById(R.id.age);
            agePicker.setVisibility(View.INVISIBLE);
        }
    }


    public void setVisibilities(){
        findViewById(R.id.question).setVisibility(View.INVISIBLE);

        sexSpinner.setVisibility(View.INVISIBLE);
        findViewById(R.id.sexText).setVisibility(View.INVISIBLE);
        findViewById(R.id.ageText).setVisibility(View.VISIBLE);
        findViewById(R.id.continueBtn).setVisibility(View.VISIBLE);
        agePicker = findViewById(R.id.age);
        agePicker.setVisibility(View.VISIBLE);
        agePicker.setMinValue(0);
        agePicker.setMaxValue(120);
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
        if(position != 0) {
            page++;
            sharedPref.edit().putInt("Sex", position).apply();
            setVisibilities();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void showAskParentDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Parent permitted")
                .setMessage("Please make sure you have your parents approval to use this app and send the data filled and face pictures that will be taken to a server where it will be used to create an AI machine that can detect depression")
                .setPositiveButton("I have got my parents approval", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("parentsApproval", true);
                        editor.apply();
                        dialog.dismiss();
                        findViewById(R.id.question).setVisibility(View.VISIBLE);
                        movePage();
                    }
                })
                .create().show();
    }
}
