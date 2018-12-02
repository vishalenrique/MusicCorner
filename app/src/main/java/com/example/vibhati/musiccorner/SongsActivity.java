package com.example.vibhati.musiccorner;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;

public class SongsActivity extends AppCompatActivity implements SongAdapter.ClickListener{

    ArrayList<Song> songList;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final String TAG = "SongsActivity";
    RecyclerView recyclerView;
    private SongAdapter adapter;

    // Here
    Button mPlay;
    private MediaBrowserCompat mMediaBrowser;
    private MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            super.onPlaybackStateChanged(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            super.onMetadataChanged(metadata);
        }
    };
    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback (){

        @Override
        public void onConnected() {
            Log.i(TAG,"onConnected entered");

            // Get the token for the MediaSession
            MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();

            // Create a MediaControllerCompat
            MediaControllerCompat mediaController =
                    null;
            try {
                mediaController = new MediaControllerCompat(SongsActivity.this, // Context
                        token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // Save the controller
            MediaControllerCompat.setMediaController(SongsActivity.this, mediaController);

            buildTransportControls();

            Log.i(TAG,"onConnected exit");
        }

        @Override
        public void onConnectionSuspended() {
            super.onConnectionSuspended();
        }

        @Override
        public void onConnectionFailed() {
            super.onConnectionFailed();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.i(TAG,"onCreate called");

        recyclerView = findViewById(R.id.rv_main);
        songList = new ArrayList<>();
        adapter = new SongAdapter(this, null, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        //get the permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }else{
            mMediaBrowser = new MediaBrowserCompat(this,
                    new ComponentName(this, MediaPlaybackService.class),
                    mConnectionCallbacks,
                    null);
            mMediaBrowser.connect();
        }
    }

    private void buildTransportControls() {
        mPlay = findViewById(R.id.play_pause_main);
        updateSongs();

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(SongsActivity.this);

        // Display the initial state
        MediaMetadataCompat metadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();

        if( pbState.getState() != PlaybackStateCompat.STATE_PLAYING){
            mPlay.setText("Play");
        }else{
            mPlay.setText("Pause");
        }

        // Register a Callback to stay in sync
        mediaController.registerCallback(mControllerCallback);

        // Here
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Play method
                if(mMediaBrowser.isConnected()){
                    if(TextUtils.equals(mPlay.getText().toString(),"Play")) {
                        MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().play();
                        mPlay.setText("Pause");
                    }else{
                        MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().pause();
                        mPlay.setText("Play");
                    }
                }
            }
        });
    }


    public void pauseService(View view) {
        if(mMediaBrowser.isConnected()){
            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().pause();
        }
    }

    public void previousSong(View view) {
        Log.i(TAG,"previousSong");
        MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().skipToPrevious();
    }

    public void nextSong(View view) {
        Log.i(TAG,"nextSong");
        MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().skipToNext();
    }

    public void stopService(View view) {
        if(mMediaBrowser.isConnected()){
            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().stop();
        }
        Log.i(TAG,"stopService called");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart called");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "onRequestPermissionResult called");
                    mMediaBrowser = new MediaBrowserCompat(this,
                            new ComponentName(this, MediaPlaybackService.class),
                            mConnectionCallbacks,
                            null);
                    mMediaBrowser.connect();
                }
            }
        }
    }

    private void updateSongs() {
        songList = MediaLibrary.getData(SongsActivity.this);
        Log.i(TAG,"songsList" + songList.size());
        adapter.dataChanged(songList);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (item.getItemId()) {
            case R.id.action_shuffle:
                return true;
            case R.id.action_end:
                System.exit(0);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume(){
        super.onResume();
        Log.i(TAG,"onResume called");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.i(TAG,"onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG,"onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy called");
        if (MediaControllerCompat.getMediaController(SongsActivity.this) != null) {
            MediaControllerCompat.getMediaController(SongsActivity.this).unregisterCallback(mControllerCallback);
        }
            mMediaBrowser.disconnect();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG,"onBackPressed called");
        moveTaskToBack(true);
    }
    @Override
    public void onClick(Song song, int position) {
        if(mMediaBrowser.isConnected()) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("position",position);
            editor.apply();
            mPlay.setText("Pause");
            Toast.makeText(this, song.getTitle(), Toast.LENGTH_SHORT).show();
            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().play();
        }else{
            Toast.makeText(this, "Service is not connected", Toast.LENGTH_SHORT).show();
        }
    }
}
