package com.example.vibhati.musiccorner;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

public class MediaPlaybackService extends Service {

    private LocalBinder mLocalBinder = new LocalBinder();
    private static final String TAG = "MediaPlaybackService";
    private static int mThreadId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate called: " + String.valueOf(mThreadId));
        startService(new Intent(this.getApplicationContext(),MediaPlaybackService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mThreadId = startId;
        Log.i(TAG,"onStartCommand called: " + String.valueOf(startId));
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG,"onBind called: " + String.valueOf(mThreadId));
        return mLocalBinder;
    }

    public void releaseResources() {
        Log.i(TAG,"releaseResources called: " + String.valueOf(mThreadId));
        stopSelf(mThreadId);
    }

    class LocalBinder extends Binder{
       MediaPlaybackService getService(){
            return  MediaPlaybackService.this;
        }
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        Log.i(TAG,"unbindService called: " + String.valueOf(mThreadId));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy called: " + String.valueOf(mThreadId));
    }

    public void showLogs(){
        Log.i(TAG,"showLogs called: " + String.valueOf(mThreadId));
    }
}
