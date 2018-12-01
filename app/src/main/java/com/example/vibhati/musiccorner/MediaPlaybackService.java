package com.example.vibhati.musiccorner;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
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
    private int mPosition;
    private AudioFocusRequest audioFocusRequest;
    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange){
                case AudioManager.AUDIOFOCUS_LOSS:
                    stopMusic();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    pauseMusic();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    pauseMusic();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN:
                    playMusic();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    stopMusic();
                    break;
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    stopMusic();
                    break;
            }
        }
    };

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();


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
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        // Request audio focus for playback, this registers the afChangeListener
        AudioAttributes attrs = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            attrs = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setOnAudioFocusChangeListener(afChangeListener)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    .build();
        }
        int result = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            result = am.requestAudioFocus(audioFocusRequest);
        }else {
            result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {

            if (!mMediaPlayer.isPlaying()) {
                if (mIsReady) {
                    registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
                    mMediaPlayer.start();
                    updatePlayingStateToSharedPreference(true);
                } else {
                    prepare();
                }
            } else {
                prepare();
            }
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
            unregisterReceiver(myNoisyAudioStreamReceiver);
        }
    }

    void stopMusic(){
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        // Abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.abandonAudioFocusRequest(audioFocusRequest);
        }else{
            am.abandonAudioFocus(afChangeListener);
        }
        if(mMediaPlayer.isPlaying()){
            mMediaPlayer.stop();
            updatePlayingStateToSharedPreference(false);
        }
        mMediaPlayer.reset();
        mIsReady = false;
        unregisterReceiver(myNoisyAudioStreamReceiver);
    }

    void prepare(){
        mPosition = mDefaultSharedPreferences.getInt("position", -1);
        if(mPosition == -1) {
            Log.i(TAG,"Select a song to play");
        }else{
            Song playSong = mSongList.get(mPosition);
            //get id
            long currSong = playSong.getId();
            //set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                }
                mMediaPlayer.reset();
                try {
                    mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
                } catch (Exception e) {
                    Log.e("MUSIC SERVICE", "Error setting data source", e);
                }
                mMediaPlayer.prepareAsync();
            }
//        String trackUriString = mDefaultSharedPreferences.getString("key", mDefaultValueInPrepare);
//        if(TextUtils.equals(trackUriString,mDefaultValueInPrepare)){
//            Log.i(TAG,"Select a song to play");
//        } else {
//            if (mMediaPlayer.isPlaying()) {
//                mMediaPlayer.stop();
//            }
//            mMediaPlayer.reset();
//            Uri trackUri = Uri.parse(trackUriString);
//            try {
//                mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
//            } catch (Exception e) {
//                Log.e("MUSIC SERVICE", "Error setting data source", e);
//            }
//            mMediaPlayer.prepareAsync();
//        }
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

    public void previousSong() {
        if(mPosition >= 0) {
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt("position", --mPosition);
            editor.commit();
            prepare();
        }

    }

    public void nextSong() {
        if(mPosition == mSongList.size()) {
        }else{
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt("position", ++mPosition);
            editor.commit();
            prepare();
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

    public class BecomingNoisyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
              pauseMusic();
            }
        }
    }
}
