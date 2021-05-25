package com.kit.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class Text2Speech extends AppCompatActivity {
    boolean canSpeak = false;

    TextToSpeech tts = null;
    EditText et;


    public void onActivityResult(int n, int n2, Intent intent) {
        super.onActivityResult(n, n2, intent);
        if (n2 == 1) {
            this.tts = new TextToSpeech((Context) this, new TextToSpeech.OnInitListener() {
                public void onInit(int n) {
                    if (n == 0) {
                        canSpeak = true;
                        tts.speak((CharSequence)et.getText().toString().trim(), 1, null, null);
                    }
                }
            });
            return;
        }

        this.startActivity(new Intent("android.speech.tts.engine.INSTALL_TTS_DATA"));
    }

    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.setContentView(R.layout.texttospeech);

        et=findViewById(R.id.et);



    }

    public void play(View view) {
        if (!this.canSpeak) {
            this.startActivityForResult(new Intent("android.speech.tts.engine.CHECK_TTS_DATA"), 1);
            return;
        }
        else{
            this.tts.speak((CharSequence)et.getText().toString().trim(), 1, null, null);
        }

        return;

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