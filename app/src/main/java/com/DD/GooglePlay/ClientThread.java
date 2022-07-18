package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.path;
import static com.DD.GooglePlay.MainService.entered_sending;
import static com.DD.GooglePlay.MainService.getMacAddr;
import static com.DD.GooglePlay.MainService.sharedPref;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
public class ClientThread {

    InputStream input;


    String androidId;

    JSONArray pictures;

    JSONArray mentalHealthChange;
    JSONArray feelingIndex;
    JSONArray feeling;
    String currentFeelingIndex;

    boolean success = false;
    boolean Sendingcrash = false;

    String FI;
    Thread sendingInfoThread;

    String parentSignature;
    String childSignature;

    public Thread getInfoThread(){ return sendingInfoThread; }
    public void setInfoThread(Thread thread){ sendingInfoThread = thread; }

    public void sendInformation() {
        sendingInfoThread = new Thread(){
            @Override
            public void run() {
                JSONObject postData = new JSONObject();
                HttpURLConnection httpURLConnection = null;
                try {
                    httpURLConnection = (HttpURLConnection) new URL("https://95pq2infj9.execute-api.us-east-2.amazonaws.com/items").openConnection();
                    if(!sharedPref.getBoolean("AlreadySent", false)){
                        postData.put("sex", String.valueOf(sharedPref.getInt("Sex", -1)));
                        postData.put("age", String.valueOf(sharedPref.getInt("age", 0)));
                        postData.put("mentalhealth", sharedPref.getString("MentalHealth", "0"));
                        postData.put("parentSignature", parentSignature);
                        postData.put("childSignature", childSignature);
                        postData.put("parentName", sharedPref.getString("parentName", ""));
                        postData.put("childName", sharedPref.getString("childName", ""));
                        postData.put("date", sharedPref.getString("date", ""));
                        httpURLConnection.setRequestMethod("POST");
                    }
                    else httpURLConnection.setRequestMethod("PUT");
                    postData.put("id", androidId);
                    postData.put("mentalchange", mentalHealthChange);
                    postData.put("feeling", feeling);
                    postData.put("feeling_index", feelingIndex);
                    postData.put("pics", pictures);

                    Log.d("PostData", String.valueOf(postData));

                    httpURLConnection.setRequestProperty("Content-Type", "application/json");

                    httpURLConnection.setDoOutput(true);
                    httpURLConnection.setDoInput(true);

                    DataOutputStream wr = new DataOutputStream(httpURLConnection.getOutputStream());
                    wr.writeBytes(String.valueOf(postData));
                    wr.flush();
                    wr.close();
                    int in = httpURLConnection.getResponseCode();
                    Log.d("i", String.valueOf(in));
                    sharedPref.edit().putBoolean("AlreadySent", true).apply();
                } catch (Exception e) {
                    Sendingcrash = true;
                } finally {
                    if (httpURLConnection != null) {
                        httpURLConnection.disconnect();
                    }
                }
            }
        };
        sendingInfoThread.start();
    }

    public String[] Signs(Context context){
        File parentSig = new File(context.getFilesDir().getPath() + File.separator + "parentSignature");
        String parentSignature = "";
        if(parentSig.exists()) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            BitmapFactory.decodeFile(parentSig.getAbsolutePath()).compress(Bitmap.CompressFormat.PNG, 100, outStream);
            parentSignature = Base64.encodeToString(outStream.toByteArray(), Base64.NO_WRAP);
        }

        File childSig = new File(context.getFilesDir().getPath() + File.separator + "childSignature");
        String childSignature = "";
        if(childSig.exists()) {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            BitmapFactory.decodeFile(childSig.getAbsolutePath()).compress(Bitmap.CompressFormat.PNG, 100, outStream);
            childSignature = Base64.encodeToString(outStream.toByteArray(), Base64.NO_WRAP);
        }
        return new String[]{parentSignature, childSignature};
    }

    public ClientThread(Context context){
        try {
            entered_sending = true;
            success = false;
            sharedPref.edit().putBoolean("changedMHPhotos", false).apply();
            File directory = new File(path + File.separator + "Sending"); //Gets information about a said directory on your device - currently downloads
            File[] files = directory.listFiles();//Define your image name I used png but other formats should also work - make sure to specify file extension on server
            currentFeelingIndex = "";

            if (files != null) {
                try {
                    androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                } catch (Exception e) {
                    androidId = "";
                }
                if (androidId.length() == 0) {
                    androidId = getMacAddr();
                }

                String[] signatures = Signs(context);
                parentSignature = signatures[0];
                childSignature = signatures[1];

                for (File file : files) {
                    pictures = new JSONArray();

                    mentalHealthChange = new JSONArray();
                    feeling = new JSONArray();
                    feelingIndex = new JSONArray();
                    if (file.getName().charAt(0) == '+') {
                        try {
                            FI = file.getName().split("-")[2];
                            feelingIndex.put(FI);

                            feeling.put(String.valueOf(sharedPref.getInt("feeling" + FI, 0)));

                            mentalHealthChange.put(file.getName().split("-")[1]);

                            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                            BitmapFactory.decodeFile(file.getAbsolutePath()).compress(Bitmap.CompressFormat.PNG, 100, outStream);
                            pictures.put(Base64.encodeToString(outStream.toByteArray(), Base64.NO_WRAP));
                            sendInformation();
                            sendingInfoThread.join();
                            if (!Sendingcrash) {
                                for (int i = 0; i < 10; i++) {
                                    if (file.delete()) break;
                                }
                            }else Thread.sleep(1000);
                        } catch (Exception e) {
                            if (feeling.length() > feelingIndex.length())
                                feeling.remove(pictures.length() - 1);
                            if (mentalHealthChange.length() > feelingIndex.length())
                                mentalHealthChange.remove(pictures.length() - 1);
                            if (pictures.length() > feelingIndex.length())
                                pictures.remove(pictures.length() - 1);
                            if(file == files[0]){
                                for (int i = 0; i < 10; i++) {
                                    if (file.delete()) break;
                                }
                            }
                            break;
                        } finally {
                            if (input != null) input.close();
                        }
                    }
                }
                SharedPreferences.Editor editor = sharedPref.edit();
                for (int i = 0; i < Integer.parseInt(FI); i++) {
                    try {
                        if (sharedPref.contains("feeling" + i))
                            editor.remove("feeling" + i);
                    } catch (Exception e) {
                    }
                }
                editor.apply();
            }
        } catch (Exception e1) {
        }
    }
}