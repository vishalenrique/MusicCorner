package com.example.vibhati.musiccorner;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

public class Analytics {

    public static void logEvent(Context context, String eventName, Bundle bundle){
        FirebaseAnalytics.getInstance(context).logEvent(eventName,bundle);
    }
}
