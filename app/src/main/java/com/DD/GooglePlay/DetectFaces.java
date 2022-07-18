package com.DD.GooglePlay;

import android.graphics.Bitmap;
import android.graphics.Rect;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

public class DetectFaces {
    static Task<List<Face>> task;
    static int size = 0;
    Rect bound;
    static FaceDetector detector;
    private FaceDetectorOptions configeOptions(){
        return new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
    }

    void Detect(Bitmap myBitmap){
        try {
            if(detector == null) detector = FaceDetection.getClient(configeOptions());
            InputImage image = InputImage.fromBitmap(myBitmap, 0);
            detect(image);
        } catch (Exception e) {
        }
    }

    void detect(InputImage image){
        task = detector.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<List<Face>>() {
                            @Override
                            public void onSuccess(List<Face> faces) {
                                if(faces != null){
                                    size = faces.size();
                                    if(size > 0){
                                        bound = faces.get(0).getBoundingBox();
                                    }
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                size = 0;
                            }
                        });
    }

    Bitmap crop(Bitmap bitmap){
        try {
            int top = bound.top;
            int bottom = bound.bottom;
            int left = bound.left;
            int right = bound.right;
            if (left < 0) left = 0;
            if (top < 0) top = 0;
            if (bottom > bitmap.getHeight()) bottom = bitmap.getHeight();
            if (right > bitmap.getWidth()) right = bitmap.getWidth();
            return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
        } catch (Exception e) {
            return null;
        }
    }
}