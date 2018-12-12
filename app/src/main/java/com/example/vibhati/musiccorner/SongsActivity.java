package com.example.vibhati.musiccorner;

import android.Manifest;
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

import java.io.IOException;
import java.util.ArrayList;

public class SongsActivity extends AppCompatActivity implements OnSongClickListener,FavoriteSongsFragment.OnFavoriteSongClickListener{

    ArrayList<Song> songList;
    private static final int MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final String TAG = SongsActivity.class.getSimpleName();
    ImageView mPlayPause;
    ImageView mAlbumArt;
    TextView mSongTitle;
    ViewPager mViewPager;
    TabLayout mTabLayout;
    SeekBar mSeekBar;
    private MediaBrowserCompat mMediaBrowser;
    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback (){

        @Override
        public void onConnected() {

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
    private boolean mUserIsSeeking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songList = new ArrayList<>();

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
        }
    }

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
            MediaDescriptionCompat description = metadata.getDescription();
            mSongTitle.setText(description.getTitle());
            setAlbumArt(description);
            mSeekBar.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
        }
    };

    private void updateSeekBarState(PlaybackStateCompat state) {
        mSeekBar.setProgress((int) state.getPosition());
    }

    private void buildTransportControls() {
        mTabLayout = findViewById(R.id.tl_main);
        mViewPager = findViewById(R.id.vp_main);
        mAlbumArt = findViewById(R.id.iv_art_main);
        mPlayPause = findViewById(R.id.play_pause_main);
        mSongTitle = findViewById(R.id.song_title_main);
        mSeekBar = findViewById(R.id.sb_main);
        initializeSeekBar();
        setupViewPagerAndTabLayout();
        updateSongs();

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(SongsActivity.this);

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
        int maxWithoutCasting = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mSeekBar.setMax(maxWithoutCasting);

        setAlbumArt(description);

        // Register a Callback to stay in sync
        mediaController.registerCallback(mControllerCallback);

        // Here
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mMediaBrowser.isConnected()){
                    int state = MediaControllerCompat.getMediaController(SongsActivity.this).getPlaybackState().getState();

                    switch (state){
                        case PlaybackStateCompat.STATE_NONE:
                            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().play();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().play();
                            break;
                        case PlaybackStateCompat.STATE_PLAYING:
                            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().pause();
                            break;
                    }
                }

            }
        });
    }

    private void setAlbumArt(MediaDescriptionCompat description) {
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), description.getIconUri());
        } catch (IOException e) {
            e.printStackTrace();
        }

        mAlbumArt.setImageBitmap(bitmap);
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
                MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().seekTo(userSelectedPosition);
            }
        });
    }

    private void setupViewPagerAndTabLayout() {
        SongsPagerAdapter adapter = new SongsPagerAdapter(getSupportFragmentManager());
        adapter.addFragment(SongsFragment.newInstance(),getString(R.string.fragment_name_songs));
        adapter.addFragment(FavoriteSongsFragment.newInstance(),getString(R.string.fragment_name_favorites));
        mViewPager.setAdapter(adapter);
        mTabLayout.setupWithViewPager(mViewPager);
    }

    public void previousSong(View view) {
        MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().skipToPrevious();
    }

    public void nextSong(View view) {
       MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().skipToNext();
    }

    public void stopService(View view) {
        if(mMediaBrowser.isConnected()){
            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().stop();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSION_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMediaBrowser = new MediaBrowserCompat(this,
                            new ComponentName(this, MediaPlaybackService.class),
                            mConnectionCallbacks,
                            null);
                }
            }
        }
    }

    private void updateSongs() {
        songList = MediaLibrary.getData(SongsActivity.this);
    }

    @Override
    protected void onResume(){
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(SongsActivity.this) != null) {
            MediaControllerCompat.getMediaController(SongsActivity.this).unregisterCallback(mControllerCallback);
        }
        if(MediaControllerCompat.getMediaController(SongsActivity.this).getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED ){
            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().stop();
        }
        mMediaBrowser.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return super.onKeyDown(keyCode, event);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                MediaControllerCompat.getMediaController(SongsActivity.this).dispatchMediaButtonEvent(event);
                return true;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                MediaControllerCompat.getMediaController(SongsActivity.this).dispatchMediaButtonEvent(event);
                return true;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                MediaControllerCompat.getMediaController(SongsActivity.this).dispatchMediaButtonEvent(event);
                return true;
            case KeyEvent.KEYCODE_MEDIA_CLOSE:
                MediaControllerCompat.getMediaController(SongsActivity.this).dispatchMediaButtonEvent(event);
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void nextActivity(View view) {
        Intent intent = new Intent(this,MediaPlayerActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSongClicked(Song song, int position) {
        if(mMediaBrowser.isConnected()) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(getString(R.string.isFavoriteMode),false);
            editor.putInt(getString(R.string.position),position);
            editor.apply();
            MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().play();
        }else{
            Toast.makeText(this, R.string.service_unavailable_message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavoriteSongClicked(Song song, int position) {
        Analytics.getInstance().logEvent(SongsActivity.this,getString(R.string.analytics_song_from_favorites),null);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(getString(R.string.isFavoriteMode),true);
        editor.putInt(getString(R.string.position),position);
        editor.apply();
        MediaControllerCompat.getMediaController(SongsActivity.this).getTransportControls().play();
    }
}
