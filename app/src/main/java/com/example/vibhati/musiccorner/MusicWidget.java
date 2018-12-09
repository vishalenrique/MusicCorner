package com.example.vibhati.musiccorner;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Implementation of App Widget functionality.
 */
public class MusicWidget extends AppWidgetProvider {

    private static final String TAG = "MusicWidget";
    public static final String SONG_NAME = "songName";
    public static final String isPlaying = "isPlaying";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        Log.i(TAG,"updateAppWidget called");

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String songTitle = defaultSharedPreferences.getString(SONG_NAME, "None");
        boolean isPlaying = defaultSharedPreferences.getBoolean(MusicWidget.isPlaying, false);

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.music_widget);

        views.setTextViewText(R.id.widget_title,songTitle);
        if(isPlaying){
            views.setTextViewText(R.id.widget_play,"pause");
        }else{
            views.setTextViewText(R.id.widget_play,"play");
        }
        Intent intentPlay = new Intent(context, MediaPlaybackService.class);
        intentPlay.setAction(MediaPlaybackService.ACTION_WIDGET_PLAY);
        views.setOnClickPendingIntent(R.id.widget_play, PendingIntent.getService(context,23,intentPlay,PendingIntent.FLAG_UPDATE_CURRENT));

        Intent intentPrevious = new Intent(context, MediaPlaybackService.class);
        intentPrevious.setAction(MediaPlaybackService.ACTION_WIDGET_PREVIOUS);
        views.setOnClickPendingIntent(R.id.widget_previous, PendingIntent.getService(context,24,intentPrevious,PendingIntent.FLAG_UPDATE_CURRENT));

        Intent intentNext = new Intent(context, MediaPlaybackService.class);
        intentNext.setAction(MediaPlaybackService.ACTION_WIDGET_NEXT);
        views.setOnClickPendingIntent(R.id.widget_next, PendingIntent.getService(context,25,intentNext,PendingIntent.FLAG_UPDATE_CURRENT));
        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.i(TAG,"onUpdate called");
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.i(TAG,"onEnabled called");
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        Log.i(TAG,"onDisabled called");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG,"onReceive called");
        super.onReceive(context, intent);
    }
}

