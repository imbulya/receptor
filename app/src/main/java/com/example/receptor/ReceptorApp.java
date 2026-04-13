package com.example.receptor;

import android.app.Application;
import android.content.Context;

public class ReceptorApp extends Application {

    private static ReceptorApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static Context getContext() {
        return instance;
    }
}
