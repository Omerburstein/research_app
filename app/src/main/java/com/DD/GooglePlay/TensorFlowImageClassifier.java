package com.DD.GooglePlay;

import static com.DD.GooglePlay.MainActivity.path;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;

public class TensorFlowImageClassifier {
    Module module;

    public Module getModule(){ return module; }

    public TensorFlowImageClassifier(Context context, String model){
        module = LiteModuleLoader.load(assetFilePath(context, model));
    }

    public String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e("BRUH", "Error process asset " + assetName + " to file path");
        }
        return null;
    }
    public int recognizeImage(Bitmap bitmap1, Bitmap bitmap2){
        Tensor inputTensor2 = TensorImageUtils.bitmapToFloat32Tensor(bitmap2,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
        Tensor outputTensor2 = module.forward(IValue.from(inputTensor2)).toTensor();
        float[] scores2 = outputTensor2.getDataAsFloatArray();
        return recognizeImage(bitmap1, scores2);
    }

    public int recognizeImage(Bitmap bitmap1, float[] knownScores) {
        try {
            Tensor inputTensor1 = TensorImageUtils.bitmapToFloat32Tensor(bitmap1,
                    TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
            Tensor outputTensor1 = module.forward(IValue.from(inputTensor1)).toTensor();
            float[] scores1 = outputTensor1.getDataAsFloatArray();
            if(similirty(scores1, knownScores) < 0.15) return 1;
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Bitmap convertFileToBit(File file){
        return BitmapFactory.decodeFile(file.getPath());
    }

    public float[][] readFile(String fileName, Context context){
        String ret = "";

        try {
            InputStream inputStream = context.openFileInput(fileName);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ((receiveString = bufferedReader.readLine()) != null) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }catch (FileNotFoundException e){
            savePicsToFile(path + File.separator + "DataSet", context);
            readFile(fileName, context);
        } catch (Exception e) {
        }
        String[] strings = ret.replace('[', ' ').split("]");
        float[][] scores = new float[strings.length][strings[0].split(",").length];
        float tempFloat;
        String[] tempString;
        for(int i = 0; i < strings.length; i++){
            tempString = strings[i].split(",");
            for(int j = 0; j < tempString.length; j++) {
                tempFloat =  Float.parseFloat(tempString[j]);
                scores[i][j] = tempFloat;
            }
        }
        return scores;
    }


    public boolean recognizeAllPic(Bitmap bitmap1, Context context) {
        float[][] scores = readFile("scores.txt", context);
        int counter = 0;
        for (float[] score : scores) {
            counter += recognizeImage(bitmap1, score);
        }
        return counter > scores.length * 0.6;
    }

    static double similirty(float[] score1, float[] score2){
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < score1.length; i++) {
            dotProduct += Math.abs(score1[i] * score2[i]);
            normA += Math.pow(score1[i], 2);
            normB += Math.pow(score2[i], 2);
        }
        return 1 - dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public void savePicsToFile(String path, Context context){
        File directory = new File(path);
        Log.d("PYTORCH", "Saving Files!!!!!!!!!!!!!!!!!!");
        File[] files = directory.listFiles();
        StringBuilder scores = new StringBuilder();
        if(files != null) {
            Bitmap bitmap = null;
            for (File file : files) {
                bitmap = convertFileToBit(file);
                Tensor inputTensor1 = TensorImageUtils.bitmapToFloat32Tensor(bitmap,
                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
                Tensor outputTensor1 = module.forward(IValue.from(inputTensor1)).toTensor();
                scores.append(Arrays.toString(outputTensor1.getDataAsFloatArray()));
            }
            writeToFile(scores.toString(), context);
            if(bitmap != null) bitmap.recycle();
        }
    }
    private void writeToFile(String data,Context context) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("scores.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (Exception e) {
        }
    }
}