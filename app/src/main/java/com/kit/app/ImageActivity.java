package com.kit.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bogdwellers.pinchtozoom.ImageMatrixTouchHandler;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;

public class ImageActivity extends AppCompatActivity {

    boolean canSpeak = false;

    TextToSpeech tts = null;

    private ImageView ivImage;
    private String text="";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_selected);

        ivImage = findViewById(R.id.ivImage);

        ivImage.setOnTouchListener(new ImageMatrixTouchHandler(ivImage.getContext()));


        Uri data=getIntent().getData();
       com.bumptech.glide.Glide.with(this)
                .load(data)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable GlideException e, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, DataSource dataSource, boolean isFirstResource) {

                        return false;
                    }
                })

                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(ivImage);

        FirebaseVisionImage image;
        try {
            image = FirebaseVisionImage.fromFilePath(this, data);
            FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                    .getOnDeviceTextRecognizer();
            Task<FirebaseVisionText> result =
                    detector.processImage(image)
                            .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                                @Override
                                public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                    // Task completed successfully
                                    // ...
                                    text = firebaseVisionText.getText();

                                    if (!canSpeak) {
                                        startActivityForResult(new Intent("android.speech.tts.engine.CHECK_TTS_DATA"), 1);
                                        return;
                                    }
                                    else{
                                        tts.speak(text, 1, null, null);
                                    }



                                }
                            })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Task failed with an exception
                                            // ...
                                        }
                                    });





        } catch (IOException e) {
            e.printStackTrace();
        }







    }


    public void onActivityResult(int n, int n2, Intent intent) {
        super.onActivityResult(n, n2, intent);
        if (n2 == 1) {
            this.tts = new TextToSpeech((Context) this, new TextToSpeech.OnInitListener() {
                public void onInit(int n) {
                    if (n == 0) {
                        canSpeak = true;
                        tts.speak(text, 1, null, null);
                    }
                }
            });
            return;
        }

        this.startActivity(new Intent("android.speech.tts.engine.INSTALL_TTS_DATA"));
    }



    @Override
    protected void onDestroy() {
        if(tts!=null)
        if (this.tts.isSpeaking()) {
            this.tts.stop();
        }
        super.onDestroy();
    }
}
