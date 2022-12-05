package com.example.ponyo;

import android.app.Application;

public class Language extends Application {
    private String sourceLang;
    private String targetLang;

    public String getSourceLang() {
        return sourceLang;
    }

    public void setSourceLang( String sourceLang ) {
        this.sourceLang = sourceLang;
    }

    public String getTargetLang() {
        return targetLang;
    }

    public void setTargetLang( String targetLang ) {
        this.targetLang = targetLang;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sourceLang = "ko";
        targetLang = "en";
    }
}
