package com.example.vibhati.musiccorner;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerActivity extends AppCompatActivity {
    private static final String TAG = "MediaPlayerActivity";

    // Here
    ImageView mPlayPause;
    ImageView mAlbumArt;
    TextView mSongTitle;
    SeekBar mSeekBar;
    Button mButton;
    private ArrayList<Song> mSongList;
    private Song mSong;
    private MediaBrowserCompat mMediaBrowser;
    private SongViewModel mSongViewModel;
    private List<Song> mFavoriteSongList;
    private boolean isFavorite;
    private boolean mUserIsSeeking;

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
                mediaController = new MediaControllerCompat(MediaPlayerActivity.this, // Context
                        token);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            // Save the controller
            MediaControllerCompat.setMediaController(MediaPlayerActivity.this, mediaController);

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

    private MediaControllerCompat.Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            switch(state.getState()){
                case PlaybackStateCompat.STATE_PLAYING:
                    mPlayPause.setImageResource(R.drawable.pause);
                    break;
                case PlaybackStateCompat.STATE_PAUSED:
                    mPlayPause.setImageResource(R.drawable.play);
                    break;
                case PlaybackStateCompat.STATE_STOPPED:
                    mPlayPause.setImageResource(R.drawable.play);
                    break;
                case PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS:
                    updateSeekBarState(state);
                case PlaybackStateCompat.STATE_SKIPPING_TO_NEXT:
                    updateSeekBarState(state);
                case PlaybackStateCompat.STATE_BUFFERING:
                    updateSeekBarState(state);
            }

            updateSeekBarState(state);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            mSongTitle.setText(metadata.getDescription().getTitle());
            mSeekBar.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        Log.i(TAG,"onCreate called");

        mMediaBrowser = new MediaBrowserCompat(this,
                    new ComponentName(this, MediaPlaybackService.class),
                    mConnectionCallbacks,
                    null);
    }

    private void updateSeekBarState(PlaybackStateCompat state) {
//        int bufferedPosition = (int) state.getBufferedPosition();
        mSeekBar.setProgress((int) state.getPosition());
    }

    private void buildTransportControls() {
        mPlayPause = findViewById(R.id.iv_play_media);
        mAlbumArt = findViewById(R.id.iv_art_media);
        mSongTitle = findViewById(R.id.tv_title_media);
        mSeekBar = findViewById(R.id.sb_media);
        mButton = findViewById(R.id.favorite_media);
        AdView adView = findViewById(R.id.adView);
        initializeSeekBar();

        mSongList = MediaLibrary.getData(this);
        mSong = mSongList.get(PreferenceManager.getDefaultSharedPreferences(this).getInt("position", 0));


        mSongViewModel = ViewModelProviders.of(this).get(SongViewModel.class);
        mSongViewModel.getAllSongs().observe(this, new Observer<List<Song>>() {
            @Override
            public void onChanged(@Nullable List<Song> songs) {
                Log.i(TAG,"list size: " + songs.size());
                mFavoriteSongList = songs;
                isFavorite = mFavoriteSongList.contains(mSong);
                updateState();
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.logEvent(MediaPlayerActivity.this,"LIKE_UNLIKE",null);
                if(!isFavorite){
                    mSongViewModel.insert(mSong);

                }else{
                    mSongViewModel.deleteSong(mSong.getId());
                }
                isFavorite = !isFavorite;
                updateState();
            }
        });


        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        adView.loadAd(adRequest);


//        getSupportFragmentManager().beginTransaction().replace(R.id.fl_container_main,SongsFragment.newInstance()).commit();
        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaPlayerActivity.this);

        // Display the initial state
        MediaMetadataCompat metadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();

        if( pbState.getState() == (PlaybackStateCompat.STATE_PLAYING | PlaybackStateCompat.STATE_BUFFERING)){
            mPlayPause.setImageResource(R.drawable.pause);
        }else{
            mPlayPause.setImageResource(R.drawable.play);
        }
        MediaDescriptionCompat description = metadata.getDescription();
        mSongTitle.setText(description.getTitle());

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), description.getIconUri());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mAlbumArt.setImageBitmap(bitmap);

        int maxWithoutCasting = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mSeekBar.setMax(maxWithoutCasting);

        Log.i(TAG,"maxWithoutCasting: "+ maxWithoutCasting);
        Log.i(TAG,"maxWithCasting: "+ mSeekBar.getMax());

        Log.i(TAG,"Current playback state: "+pbState.getState());

        // Register a Callback to stay in sync
        mediaController.registerCallback(mControllerCallback);

        // Here
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Play method
                if(mMediaBrowser.isConnected()){
                    int state = MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getPlaybackState().getState();

                    switch (state){
                        case PlaybackStateCompat.STATE_NONE:
                            Log.i(TAG,"STATE_NONE");
                            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                            Log.i(TAG,"STATE_PAUSED");
                            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();
                            break;
                        case PlaybackStateCompat.STATE_PLAYING:
                            Log.i(TAG,"STATE_PLAYING");
                            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().pause();
                            break;
                    }
                }
            }
        });
    }

        private void updateState() {
        if(isFavorite){
            mButton.setText("unlike");
        }else{
            mButton.setText("like");
        }
    }

    private void initializeSeekBar() {
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int userSelectedPosition = 0;

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mUserIsSeeking = true;
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser){
                    userSelectedPosition = progress;
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserIsSeeking = false;
                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().seekTo(userSelectedPosition);
            }
        });
    }

    public void previousSong(View view) {
        Log.i(TAG,"previousSong");
        MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().skipToPrevious();
    }

    public void nextSong(View view) {
        Log.i(TAG,"nextSong");
        MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().skipToNext();
    }

    public void stopService(View view) {
        if(mMediaBrowser.isConnected()){
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().stop();
        }
        Log.i(TAG,"stopService called");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG,"onStart called");
        mMediaBrowser.connect();
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
        if (MediaControllerCompat.getMediaController(MediaPlayerActivity.this) != null) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).unregisterCallback(mControllerCallback);
        }
        mMediaBrowser.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"onDestroy called");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG,"onBackPressed called");
       // moveTaskToBack(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.onKeyDown(keyCode, event);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).dispatchMediaButtonEvent(event);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).dispatchMediaButtonEvent(event);
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).dispatchMediaButtonEvent(event);
                return true;
            case KeyEvent.KEYCODE_MEDIA_CLOSE:
                MediaControllerCompat.getMediaController(MediaPlayerActivity.this).dispatchMediaButtonEvent(event);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
