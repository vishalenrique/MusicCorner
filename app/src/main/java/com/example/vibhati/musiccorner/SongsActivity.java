package com.example.vibhati.musiccorner;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
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

public class SongsActivity extends AppCompatActivity implements SongAdapter.ClickListener,SharedPreferences.OnSharedPreferenceChangeListener{

    ArrayList<Song> songList;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final String TAG = "SongsActivity";
    RecyclerView recyclerView;
    private SongAdapter adapter;

    // Here
    Button mPlay;
    private MediaPlaybackService mService;
    private boolean mBound = false;

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MediaPlaybackService.LocalBinder localBinder = (MediaPlaybackService.LocalBinder) service;
            mService = localBinder.getService();
            mBound = true;
            mService.setSongs(songList);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };
    private SharedPreferences mDefaultSharedPreferences;
    private boolean mIsCurrentlyPlaying;

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

        mPlay = findViewById(R.id.play_pause_main);

        //get Current state of the play button
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mIsCurrentlyPlaying = mDefaultSharedPreferences.getBoolean("isPlaying", false);

        if(mIsCurrentlyPlaying){
            mPlay.setText("Pause");
        }else{
            mPlay.setText("Play");
        }

        // Here
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Play method
                if(mBound){
                    if(TextUtils.equals(mPlay.getText().toString(),"Play")) {
                        mService.showLogs();
                        mService.playMusic();
                        mPlay.setText("Pause");
                    }else{
                        mService.pauseMusic();
                        mPlay.setText("Play");
                    }
                }
            }
        });

        //get the permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }else{
            updateSongs();
            bindTheService();

        }

        mDefaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }


    public void pauseService(View view) {
        // pause
        if(mBound){
            mService.pauseMusic();
        }
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("key","value");
//        editor.apply();
    }

    public void previousSong(View view) {
        Log.i(TAG,"previousSong");
        mService.previousSong();
    }

    public void nextSong(View view) {
        Log.i(TAG,"nextSong");
        mService.nextSong();
    }

    public void stopService(View view) {

        // Stop Method
        if(mBound){
            mService.stopMusic();
        }

        Log.i(TAG,"stopService called");
        //mService.releaseResources();

//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        editor.putString("key","newValue");
//        editor.apply();


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
                    updateSongs();
                    bindTheService();
                }
            }
        }
    }

    private void updateSongs() {
        songList = MediaLibrary.getData(SongsActivity.this);
        Log.i(TAG,"songsList" + songList.size());
        adapter.dataChanged(songList);
    }

    private void bindTheService() {
        Intent serviceIntent = new Intent(this.getApplicationContext(),MediaPlaybackService.class);
//        startService(serviceIntent);
        bindService(serviceIntent,mServiceConnection, Context.BIND_AUTO_CREATE);
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
        // it automatically unbinds and onDestroy gets called
            unbindService(mServiceConnection);
        mBound = false;
        mDefaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG,"onBackPressed called");
        moveTaskToBack(true);
    }
    @Override
    public void onClick(Song song, int position) {
//        Intent intent = new Intent(this,MediaPlayerActivity.class);
//        intent.putExtra(MediaPlayerActivity.EXTRA_SONG,song);
//        startActivity(intent);
        if(mBound) {
            //get id
            long currSong = song.getId();
            //set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("key", trackUri.toString());
            editor.putInt("position",position);
            editor.apply();
            mPlay.setText("Pause");
            Toast.makeText(this, song.getTitle(), Toast.LENGTH_SHORT).show();
            mService.playMusic();
        }else{
            Toast.makeText(this, "Service is not connected, mBound: "+mBound, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals("isPlaying")){
            if(sharedPreferences.getBoolean(key,false)){
                mPlay.setText("Pause");
            }else{
                mPlay.setText("Play");
            }
        }
    }
}
