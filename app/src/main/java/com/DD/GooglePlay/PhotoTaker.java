package com.DD.GooglePlay;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
import static com.DD.GooglePlay.DetectFaces.size;
import static com.DD.GooglePlay.MainActivity.cameraid;
import static com.DD.GooglePlay.MainActivity.path;
import static com.DD.GooglePlay.MainActivity.temp_time;
import static com.DD.GooglePlay.MainService.calcCurrentDay;
import static com.DD.GooglePlay.MainService.finished;
import static com.DD.GooglePlay.MainService.last_time;
import static com.DD.GooglePlay.MainService.sharedPref;
import static com.DD.GooglePlay.MainService.enteredStopCounter;
import static com.DD.GooglePlay.MainService.DifferentPCounter;
import static com.DD.GooglePlay.MainService.found;
import static com.DD.GooglePlay.MainService.stop_counter;

import static java.lang.Thread.sleep;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import org.pytorch.IValue;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.TimeZone;

public class PhotoTaker extends Service {
    final static DetectFaces faceDetector = new DetectFaces();
    Camera camera;
    SurfaceTexture surfaceTexture;
    NotificationManager foregroundManager;

    TensorFlowImageClassifier tfClassifier;
    static int counter;

    private final String MODEL_PATH = "model.ptl";
    /**
     * picture call back
     */

    static Thread analysisThread;
    static Bitmap bitmap;

    static int crashedCounter = 0;

    private void initTensorFlowAndLoadModel() {
        try {
            tfClassifier = new TensorFlowImageClassifier(
                    getApplicationContext(),
                    MODEL_PATH);
        } catch (Exception e) {
            tfClassifier = null;
        }
    }


    public Bitmap rescaleBit(Bitmap bitmap){
        return Bitmap.createScaledBitmap(bitmap,224,224, true);
    }

    Bitmap toGrayscale(Bitmap original)
    {
        Bitmap finalImage = Bitmap.createBitmap(original.getWidth(),
                original.getHeight(), original.getConfig());

        int A, R, G, B;
        int colorPixel;
        int width = original.getWidth();
        int height = original.getHeight();

        for (int x = 0; x < width; x++){
            for (int y = 0; y < height; y++){
                colorPixel = original.getPixel(x, y);
                A = Color.alpha(colorPixel);
                R = Color.red(colorPixel);
                G = Color.green(colorPixel);
                B = Color.blue(colorPixel);

                R = (R + G + B) / 3;
                G = R;
                B = R;

                finalImage.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        return finalImage;
    }


    void delete(){
        String p = path + File.separator + "DataSet";
        File directory = new File(p);
        File[] files = directory.listFiles();
        Bitmap sec = null;
        Bitmap bitmap = null;
        if(files != null) {
            int[][] counters = new int[files.length][files.length];
            int[] counters1 = new int[files.length];
            for (int i = 0; i < files.length; i++) {
                File first = files[i];
                bitmap = BitmapFactory.decodeFile(first.getPath());
                for (int j = i + 1; j < files.length; j++) {
                    File second = files[j];
                    sec = BitmapFactory.decodeFile(second.getPath(), new BitmapFactory.Options());
                    if (tfClassifier == null || tfClassifier.getModule() == null)
                        initTensorFlowAndLoadModel();
                    int pred = tfClassifier.recognizeImage(bitmap, sec);
                    counters[i][j] += pred;
                    counters[j][i] += pred;
                    counters1[i] += pred;
                    counters1[j] += pred;
                }
            }
            if (sec != null && bitmap != null) {
                sec.recycle();
                bitmap.recycle();
            }
            for (int i = 0; i < files.length; i++) {
                int c1 = 0;
                if (counters1[i] <= 5) {
                    for (int j = 0; j < files.length; j++) {
                        if (counters[i][j] == 1) {
                            if (counters1[j] > 6) c1++;
                        }
                    }
                    if (c1 < 3) {
                        System.out.println("DelPic " + i);
                        while(files[i].delete()) {
                            try {
                                sleep(100);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            if(files.length == 11){
                tfClassifier.savePicsToFile(path + File.separator + "DataSet", getApplicationContext());
            }
        }
    }

    public void delCount(int length, File[] files){
        for(int i = 11; i < length; i++){
            while(files[i].delete()) {
                try {
                    sleep(100);
                } catch (Exception e) {
                }
            }
        }
    }

    private Bitmap rotateBitmap(Bitmap bitmap){
        Matrix matrix = new Matrix();
        matrix.postRotate(270);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public boolean CreateCamera(){
        try {
            camera = Camera.open(cameraid);
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraid, info);
            if (info.canDisableShutterSound) {
                camera.enableShutterSound(false);
            }
            crashedCounter = 0;
        } catch (Exception e) {
            crashedCounter++;
            return false;
        }
        return true;
    }

    private Bitmap ByteToBit(byte[] data){

        BitmapFactory.Options bitmap_options = new BitmapFactory.Options();
        bitmap_options.inPreferredConfig = Bitmap.Config.RGB_565;

        return BitmapFactory.decodeByteArray(data,0 ,data.length, bitmap_options);

    }

    private final Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
       public void onPictureTaken(final byte[] data, Camera camera1) {
            bitmap = rotateBitmap(ByteToBit(data));
            if(camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
            try {
                if(analysisThread != null && analysisThread.isAlive()) analysisThread.interrupt();
                analysisThread = null;
                analysisThread = TookPic();
                analysisThread.start();
                } catch (Exception e) {
            }
       }
    };

    void takeSnapShot() {
        System.out.println("taking pic, smile");
        surfaceTexture = new SurfaceTexture(10);
        try {
            camera.setPreviewTexture(surfaceTexture);
            camera.startPreview();
            camera.takePicture(null, null, jpegCallback);
        } catch (Exception e) {
            if(camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
            // TODO Auto-generated catch block
        }
    }


    public void TakePic() {
        try {
            if (CreateCamera()) {
                finished = false;
                takeSnapShot();
                // call some methods here
                // make sure to finish the thread to avoid leaking memory
            } else {
                last_time = temp_time - 270;
            }
        } catch (Exception e) {
        }
    }

    static boolean Canceled(int NotificationId, NotificationManager notificationManager){
        if(notificationManager != null) {
            StatusBarNotification[] notifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification notification : notifications) {
                if (notification.getId() == NotificationId) {
                    return false;
                }
            }
        }
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "DepressionDetectionForegroundChannel";
        String channelName = "Depression Detection taking pics";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        foregroundManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (foregroundManager != null) {
            foregroundManager.createNotificationChannel(chan);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setSmallIcon(R.drawable.ic_stat_name)
                    .setColor(Color.argb(0,59, 132, 164))
                    .build();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(1, notification, FOREGROUND_SERVICE_TYPE_CAMERA);
            }else{
                startForeground(1, notification);
            }
        }
    }

    public void CloseForeground(){
        if (foregroundManager != null) {
            while (!Canceled(1, foregroundManager)) {
                this.stopForeground(false);
                foregroundManager.cancelAll();
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        try {
            if (sharedPref == null)
                sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            if (sharedPref != null)
                if (sharedPref.getBoolean("PTCloseForeground", false)) CloseForeground();
                else {
                    while (Canceled(1, foregroundManager)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startMyOwnForeground();
                        } else {
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext())
                                    .setContentTitle(getString(R.string.app_name))
                                    .setPriority(NotificationCompat.PRIORITY_MAX)
                                    .setColor(Color.argb(0, 59, 132, 164))
                                    .setSmallIcon(R.drawable.ic_stat_name);
                            Notification notification = builder.build();
                            startForeground(1, notification);
                        }
                    }
                    TakePic();
                }
        } catch (Exception e) {
            e.printStackTrace();
            onDestroy();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            if (camera != null) {
                camera.stopPreview();
                camera.release();
                camera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public Thread TookPic() {
        return new Thread(){
            @Override
            public void run() {
                String file_path = "";
                try {
                    if(this == analysisThread) {
                        FileOutputStream outStream;
                        //Resize it as you need
                        String p = path + File.separator + "DataSet";
                        File directory = new File(p);
                        File[] files2 = directory.listFiles();
                        if (files2 != null) {
                            counter = files2.length;
                            if (counter > 11) delCount(files2.length, files2);
                        }
                        Calendar rightNow = Calendar.getInstance();
                        rightNow.setTimeZone(TimeZone.getTimeZone("Israel"));
                        if (counter > 10) {
                            faceDetector.Detect(bitmap);
                            while (!DetectFaces.task.isComplete()) sleep(100) ;
                            DetectFaces.task = null;
                            if (size >= 1) {
                                enteredStopCounter = 0;
                                bitmap = faceDetector.crop(bitmap);
                                bitmap = rescaleBit(bitmap);
                                boolean same;
                                if (bitmap != null)
                                    if (tfClassifier == null || tfClassifier.getModule() == null)
                                        initTensorFlowAndLoadModel();
                                same = tfClassifier.recognizeAllPic(bitmap, getApplicationContext());
                                System.out.println("Detected: " + same);
                                if (same) {
                                    try {
                                        int Trues = sharedPref.getInt("Trues", 0);
                                        Trues++;
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putInt("Trues", Trues);
                                        editor.apply();
                                        DifferentPCounter = 0;
                                        file_path = path + File.separator + "Sending" + File.separator +
                                                rightNow.get(Calendar.MILLISECOND) + "-" + rightNow.get(Calendar.SECOND) + "-" + rightNow.get(Calendar.MINUTE)
                                                + "-" + rightNow.get(Calendar.HOUR_OF_DAY) + "-" + rightNow.get(Calendar.DAY_OF_MONTH) + "-" + rightNow.get(Calendar.MONTH) + "-" + rightNow.get(Calendar.YEAR) + ".jpg";
                                        if (bitmap != null) {
                                            outStream = new FileOutputStream(file_path);
                                            bitmap = toGrayscale(bitmap);
                                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream); // bmp is your Bitmap instance

                                            outStream.flush();
                                            outStream.close();
                                        }
                                    } catch (Exception e) {
                                        File file = new File(file_path);
                                        if (file.exists()) while (!file.delete()) ;
                                    } finally {
                                        found = true;
                                    }
                                } else {
                                    int Mistakes = sharedPref.getInt("Mistakes", 0);
                                    Mistakes++;
                                    SharedPreferences.Editor editor = sharedPref.edit();
                                    editor.putInt("Mistakes", Mistakes);
                                    editor.apply();
                                    DifferentPCounter++;
                                }
                            }
                        } else {
                            faceDetector.Detect(bitmap);
                            while (!DetectFaces.task.isComplete()) sleep(100);
                            if (size >= 1) {
                                enteredStopCounter = 0;
                                bitmap = faceDetector.crop(bitmap);
                                bitmap = rescaleBit(bitmap);
                                file_path = path + File.separator + "DataSet" + File.separator +
                                        rightNow.get(Calendar.MILLISECOND) + "-" + rightNow.get(Calendar.SECOND) + "-" + rightNow.get(Calendar.MINUTE)
                                        + "-" + rightNow.get(Calendar.HOUR_OF_DAY) + "-" + rightNow.get(Calendar.DAY_OF_MONTH) + "-" + rightNow.get(Calendar.MONTH) + "-" + rightNow.get(Calendar.YEAR) + ".jpg";
                                try {
                                    if (bitmap != null) {
                                        outStream = new FileOutputStream(file_path);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream); // bmp is your Bitmap instance
                                        outStream.flush();
                                        outStream.close();
                                    }
                                } catch (Exception e) {
                                    File file = new File(file_path);
                                    if (file.exists()) while (!file.delete()) ;
                                } finally {
                                    found = true;
                                }
                                if (counter == 10) {
                                    if (!sharedPref.getBoolean("Sendeddeleted", false)) {
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putInt("DataSetDay", calcCurrentDay());
                                        editor.apply();
                                    } else {
                                        SharedPreferences.Editor editor = sharedPref.edit();
                                        editor.putInt("DataSetDay", sharedPref.getInt("DataSetDay", 0) + 1);
                                        editor.apply();
                                    }
                                    delete();
                                }
                            }
                        }

                    }else found = false;
                    System.out.println("Counter: " + counter);
                    if (!found) stop_counter++;
                    else stop_counter = 0;
                } catch (Exception e) {
                    File file = new File(file_path);
                    if (file.exists()) while (!file.delete()) ;
                } finally {
                    if (bitmap != null) bitmap.recycle();
                    finished = true;
                    onDestroy();
                }
            }
        };
    }

}