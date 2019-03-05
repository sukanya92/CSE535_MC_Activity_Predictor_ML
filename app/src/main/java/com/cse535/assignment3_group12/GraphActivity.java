package com.cse535.assignment3_group12;

import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class GraphActivity extends AppCompatActivity {

    WebView webViewGraph;
    String URL;
    CheckBox checkWalk, checkRun, checkJump;
    final String URL_WALK="file:///sdcard/Android/Data/CSE535_ASSIGNMENT_3/Walk.html";
    final String URL_RUN = "file:///sdcard/Android/Data/CSE535_ASSIGNMENT_3/Run.html";
    final String URL_JUMP = "file:///sdcard/Android/Data/CSE535_ASSIGNMENT_3/Jump.html";
    final String URL_WALKRUN = "file:///sdcard/Android/Data/CSE535_ASSIGNMENT_3/WalkRun.html";
    final String URL_RUNJUMP = "file:///sdcard/Android/Data/CSE535_ASSIGNMENT_3/RunJump.html";
    final String URL_WALKJUMP ="file:///sdcard/Android/Data/CSE535_ASSIGNMENT_3/WalkJump.html";
    final String URL_WALKRUNJUMP ="file:///sdcard/Android/Data/CSE535_ASSIGNMENT_3/WalkRunJump.html";
    final String URL_NOTHING = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        webViewGraph = (WebView)findViewById(R.id.webViewGraph);
        checkWalk = (CheckBox) findViewById(R.id.checkBoxWalk);
        checkRun = (CheckBox) findViewById(R.id.checkBoxRun);
        checkJump = (CheckBox) findViewById(R.id.checkBoxJump);
        WebSettings webSettings = webViewGraph.getSettings();
        webSettings.setJavaScriptEnabled(true);
        WebViewClient webViewClient = new WebViewClient();
        webViewGraph.setWebViewClient(webViewClient);


        webViewGraph.loadUrl(URL_WALKRUNJUMP);

        checkWalk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean statusRun = checkRun.isChecked();
                boolean statusJump = checkJump.isChecked();
                if(isChecked){
                    if(statusRun && statusJump){
                        //walk/run/jump
                        URL = URL_WALKRUNJUMP;
                    }else if(statusRun){
                        //walk/run
                        URL = URL_WALKRUN;
                    }else if(statusJump){
                        //walk/Jump
                        URL = URL_WALKJUMP;
                    }else{
                        //walk
                        URL = URL_WALK;
                    }
                }else{
                    if(statusRun && statusJump){
                        //run/jump
                        URL = URL_RUNJUMP;
                    }else if(statusRun){
                        //run
                        URL = URL_RUN;
                    }else if(statusJump){
                        //jump
                        URL = URL_JUMP;
                    }else{
                        //nothing
                        URL = URL_NOTHING;

                    }
                }
                webViewGraph.loadUrl(URL);

            }
        });


        checkRun.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean statusWalk = checkWalk.isChecked();
                boolean statusJump = checkJump.isChecked();
                if(isChecked){
                    if(statusWalk && statusJump){
                        //walk/run/jump
                        URL = URL_WALKRUNJUMP;
                    }else if(statusWalk){
                        //walk/run
                        URL = URL_WALKRUN;
                    }else if(statusJump){
                        //run/Jump
                        URL = URL_RUNJUMP;
                    }else{
                        //run
                        URL = URL_RUN;
                    }
                }else{
                    if(statusWalk && statusJump){
                        //walk/jump
                        URL = URL_WALKJUMP;
                    }else if(statusWalk){
                        //walk
                        URL = URL_WALK;
                    }else if(statusJump){
                        //jump
                        URL = URL_JUMP;
                    }else{
                        //nothing
                        URL = URL_NOTHING;
                    }
                }
                webViewGraph.loadUrl(URL);
            }
        });

        checkJump.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                boolean statusWalk = checkWalk.isChecked();
                boolean statusRun  = checkRun.isChecked();
                if(isChecked){
                    if(statusWalk && statusRun){
                        //walk/run/jump
                        URL = URL_WALKRUNJUMP;
                    }else if(statusWalk){
                        //walk/jump
                        URL = URL_WALKJUMP;
                    }else if(statusRun){
                        //run/Jump
                        URL = URL_RUNJUMP;
                    }else{
                        //jump
                        URL = URL_JUMP;
                    }
                }else{
                    if(statusWalk && statusRun){
                        //walk/run
                        URL = URL_WALKRUN;
                    }else if(statusWalk){
                        //walk
                        URL = URL_WALK;
                    }else if(statusRun){
                        //run
                        URL = URL_RUN;
                    }else{
                        //nothing
                        URL = URL_NOTHING;
                    }
                }
                webViewGraph.loadUrl(URL);


            }
        });





    }





}
