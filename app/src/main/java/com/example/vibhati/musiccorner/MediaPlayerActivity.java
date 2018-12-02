package com.example.vibhati.musiccorner;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

//import com.example.vibhati.musiccorner.MediaPlaybackService.LocalBinder;

public class MediaPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_SONG = "SONG_POSITION";
    Button mPlayPause;
    private static final String TAG = "MediaPlayerActivity";
    private Song mCurrentSong;
    private MediaPlaybackService mService;
    private boolean mBound = false;

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
//            LocalBinder localBinder = (LocalBinder) service;
//            mService = localBinder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.i(TAG,"onCreate entered");

        getSongFromIntent();
        setupUI();
        bindTheService();
    }

    private void getSongFromIntent() {
        Intent intent = getIntent();
        if(intent.hasExtra(EXTRA_SONG)){
            mCurrentSong = intent.getParcelableExtra(EXTRA_SONG);
        }
        if(mCurrentSong != null)
        Toast.makeText(this, mCurrentSong.getTitle(), Toast.LENGTH_SHORT).show();
    }

    private void setupUI() {
        mPlayPause = findViewById(R.id.play_pause);
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mBound){
                    mService.showLogs();
                }
            }
        });
    }

    private void bindTheService() {
        Intent serviceIntent = new Intent(this,MediaPlaybackService.class);
        bindService(serviceIntent,mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"onStart entered");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"onResume entered");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG,"onStop entered");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy entered");
        unbindService(mServiceConnection);
        mBound = false;
    }
}
