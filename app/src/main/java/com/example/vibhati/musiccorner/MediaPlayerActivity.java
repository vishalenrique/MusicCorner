package com.example.vibhati.musiccorner;

import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
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

public class MediaPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_SONG = "SONG_POSITION";
    Button mPlayPause;
    private static final String TAG = "MediaPlayerActivity";

    private MediaBrowserCompat mMediaBrowser;
    private final MediaBrowserCompat.ConnectionCallback mConnectionCallbacks =
            new MediaBrowserCompat.ConnectionCallback() {
        //0
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

                    // app scenario -- to directly play as soon as it enters the activity
//                    MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();


                    MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().playFromMediaId(String.valueOf(mCurrentSong.getId()),null);

                    // Finish building the UI
                    buildTransportControls();
                    Log.i(TAG,"onConnected entered");
                }

                @Override
                public void onConnectionSuspended() {
                    // The Service has crashed. Disable transport controls until it automatically reconnects
                }

                @Override
                public void onConnectionFailed() {
                    // The Service has refused our connection
                }
            };

    MediaControllerCompat.Callback controllerCallback =

            //3
            new MediaControllerCompat.Callback() {
                @Override
                public void onMetadataChanged(MediaMetadataCompat metadata) {
                    //update the UI
                    Log.i(TAG,"onMetaData entered");
                }

                @Override
                public void onPlaybackStateChanged(PlaybackStateCompat state) {
                    Log.i(TAG,"onPlaybackStateChanged entered");
                    if(state.getState() == PlaybackStateCompat.STATE_PLAYING){
                        mPlayPause.setText("Pause");
                    }else if(state.getState() == PlaybackStateCompat.STATE_PAUSED){
                        mPlayPause.setText("Play");
                    }
                }
            };
    private Song mCurrentSong;

    void buildTransportControls() {

        Log.i(TAG,"buildTransportControls entered");
        //1
        // Grab the view for the play/pause button
        mPlayPause = findViewById(R.id.play_pause);

        // Attach a listener to the button
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Since this is a play/pause button, you'll need to test the current state
                // and choose the action accordingly

                int pbState = MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getPlaybackState().getState();
                if (pbState == PlaybackStateCompat.STATE_PLAYING) {
                    mPlayPause.setText("Pause");
                    MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().pause();
                } else {
                    mPlayPause.setText("Play");
                    MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();
                }
            }
            });

           // for displaying the initial state of the controls

            MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaPlayerActivity.this);

            // Display the initial state
            MediaMetadataCompat metadata = mediaController.getMetadata();
            PlaybackStateCompat pbState = mediaController.getPlaybackState();

            if(pbState.getState() == PlaybackStateCompat.STATE_PLAYING){
                mPlayPause.setText("Pause");
            }else if(pbState.getState() == PlaybackStateCompat.STATE_PAUSED){
            mPlayPause.setText("Play");
            }

            // Register a Callback to stay in sync
            mediaController.registerCallback(controllerCallback);
        Log.i(TAG,"buildTransportControls exit");
        }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Log.i(TAG,"onCreate entered");

        Intent intent = getIntent();
        if(intent.hasExtra(EXTRA_SONG)){
            mCurrentSong = intent.getParcelableExtra(EXTRA_SONG);
        }

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class),
                mConnectionCallbacks,
                null); // optional Bundle
        Log.i(TAG,"onCreate exit");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG,"onStart entered");
        mMediaBrowser.connect();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG,"onResume entered");
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.i(TAG,"onStop entered");
        // (see "stay in sync with the MediaSession")
        if (MediaControllerCompat.getMediaController(MediaPlayerActivity.this) != null) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).unregisterCallback(controllerCallback);
        }
        mMediaBrowser.disconnect();

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
        }
        return super.onKeyDown(keyCode, event);
    }
}
