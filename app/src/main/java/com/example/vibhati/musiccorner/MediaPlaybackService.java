package com.example.vibhati.musiccorner;

import android.app.Service;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

public class MediaPlaybackService extends Service implements MediaPlayer.OnPreparedListener,SharedPreferences.OnSharedPreferenceChangeListener {

    private LocalBinder mLocalBinder = new LocalBinder();
    private static final String TAG = "MediaPlaybackService";
    private static int mThreadId;
    private MediaPlayer mMediaPlayer;
    private boolean mIsReady = false;
    private ArrayList<Song> mSongList;
    private SharedPreferences mDefaultSharedPreferences;
    private boolean mIsPlaying =false;
    private String mDefaultValueInPrepare = "defaultValueInPrepare";;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"onCreate called: " + String.valueOf(mThreadId));
        startService(new Intent(this.getApplicationContext(),MediaPlaybackService.class));
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mDefaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mIsReady = true;
        playMusic();
    }

    void playMusic() {
        if (!mMediaPlayer.isPlaying()) {
            if (mIsReady) {
                mMediaPlayer.start();
                updatePlayingStateToSharedPreference(true);
            } else {
                prepare();
            }
        } else{
            prepare();
        }
    }

    private void updatePlayingStateToSharedPreference(boolean state) {
        SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
        editor.putBoolean("isPlaying",state);
        editor.apply();
    }

    void pauseMusic(){
        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.pause();
            updatePlayingStateToSharedPreference(false);
        }
    }

    void stopMusic(){
        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.stop();
            updatePlayingStateToSharedPreference(false);
        }
        mMediaPlayer.reset();
        mIsReady = false;
    }

    void prepare(){
        String trackUriString = mDefaultSharedPreferences.getString("key", mDefaultValueInPrepare);
        if(TextUtils.equals(trackUriString,mDefaultValueInPrepare)){
            Log.i(TAG,"Select a song to play");
        } else {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.reset();
            Uri trackUri = Uri.parse(trackUriString);
            try {
                mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
            } catch (Exception e) {
                Log.e("MUSIC SERVICE", "Error setting data source", e);
            }
            mMediaPlayer.prepareAsync();
        }
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

    public void setSongs(ArrayList<Song> songList) {
        mSongList = songList;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("key")) {
            Log.i(TAG, "onSharedPreferenceChanged called: " + sharedPreferences.getString(key, "defaultValue"));
        }
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
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        Log.i(TAG,"onDestroy called: " + String.valueOf(mThreadId));

    }

    public void showLogs(){
        Log.i(TAG,"showLogs called: " + String.valueOf(mThreadId));
    }
}
