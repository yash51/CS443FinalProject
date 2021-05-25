package com.kit.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.soundcloud.android.crop.Crop;

import java.io.File;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int SELECT_PDF = 4;
    private static final int SELECT_PICTURE = 5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[]{Manifest.permission.CAMERA,Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},7);


    }


    public void textToSpeech(View view) {
        startActivity(new Intent(this,Text2Speech.class));
    }


    public void pickPdf(View view) {
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), SELECT_PDF);

    }


    public void help(View view){
        startActivity(new Intent(this,HelpActivity.class));
    }


    public void dialog(){
        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Use")
                .setMessage("Upload from")
                .setPositiveButton("CAMERA", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ImagePicker.Companion.with(MainActivity.this)
                                .cameraOnly()
                                .start();
                    }
                })
                .setNegativeButton("GALLERY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Crop.pickImage(MainActivity.this);
                    }
                })
                .create()
                .show();

    }


    public void pickImage(View view) {
        dialog();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK) {
                Uri currentUri = data.getData();
                if (requestCode == SELECT_PICTURE) {
                    startActivity(new Intent(this, ImageActivity.class)
                            .setData(data.getData()));
                } else if (requestCode == SELECT_PDF) {
                    startActivity(new Intent(this, PdfActivity.class)
                            .setData(data.getData()));
                }
                else {
                    if(requestCode==Crop.REQUEST_PICK){
                        handleCrop(resultCode, currentUri);
                    }
                    else if (requestCode == Crop.REQUEST_CROP) {
                        handleCrop(resultCode, currentUri);
                    }
                    else if(requestCode==ImagePicker.REQUEST_CODE){
                        handleCrop(resultCode, currentUri);
                    }

                }
            }
        }
        catch (Exception e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }


    private void beginCrop(Uri source) {
        Uri destination = Uri.fromFile(new File(getCacheDir(), "cropped"+ UUID.randomUUID()));
        Crop.of(source, destination).asSquare().start(this);
    }


    //ToDo:use Glide to load photos
    private void handleCrop(int resultCode, Uri result) {
        if (resultCode == RESULT_OK) {
            startActivity(new Intent(this,ImageActivity.class).setData(result));;

        }
    }





}