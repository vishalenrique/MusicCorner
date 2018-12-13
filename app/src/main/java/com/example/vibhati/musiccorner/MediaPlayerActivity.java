package com.example.vibhati.musiccorner;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ComponentName;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MediaPlayerActivity extends AppCompatActivity {
    private static final String TAG = MediaPlayerActivity.class.getSimpleName();
    ImageView mPlayPause;
    ImageView mAlbumArt;
    TextView mSongTitle;
    SeekBar mSeekBar;
    ImageView mLikeUnlike;
    private ArrayList<Song> mSongList;
    private Song mSong;
    private MediaBrowserCompat mMediaBrowser;
    private SongViewModel mSongViewModel;
    private List<Song> mFavoriteSongList;
    private boolean isFavorite;
    private boolean mUserIsSeeking;

    private MediaBrowserCompat.ConnectionCallback mConnectionCallbacks = new MediaBrowserCompat.ConnectionCallback() {

        @Override
        public void onConnected() {
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
            switch (state.getState()) {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mMediaBrowser = new MediaBrowserCompat(this,
                new ComponentName(this, MediaPlaybackService.class),
                mConnectionCallbacks,
                null);
    }

    private void updateSeekBarState(PlaybackStateCompat state) {
        mSeekBar.setProgress((int) state.getPosition());
    }

    private void buildTransportControls() {
        mPlayPause = findViewById(R.id.iv_play_media);
        mAlbumArt = findViewById(R.id.iv_art_media);
        mSongTitle = findViewById(R.id.tv_title_media);
        mSeekBar = findViewById(R.id.sb_media);
        mLikeUnlike = findViewById(R.id.favorite_media);
        AdView adView = findViewById(R.id.adView);
        initializeSeekBar();

        mSongList = MediaLibrary.getInstance().getData(this);
        mSong = mSongList.get(PreferenceManager.getDefaultSharedPreferences(this).getInt(getString(R.string.position), 0));


        mSongViewModel = ViewModelProviders.of(this).get(SongViewModel.class);
        mSongViewModel.getAllSongs().observe(this, new Observer<List<Song>>() {
            @Override
            public void onChanged(@Nullable List<Song> songs) {
                if (songs != null) {
                    mFavoriteSongList = songs;
                    isFavorite = mFavoriteSongList.contains(mSong);
                    updateState();
                }
            }
        });

        mLikeUnlike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Analytics.getInstance().logEvent(MediaPlayerActivity.this, getString(R.string.analytics_event_like_unlike), null);
                if (!isFavorite) {
                    mSongViewModel.insert(mSong);
                    Toast.makeText(MediaPlayerActivity.this, R.string.added_to_favorites, Toast.LENGTH_SHORT).show();

                } else {
                    mSongViewModel.deleteSong(mSong.getId());
                    Toast.makeText(MediaPlayerActivity.this, R.string.delete_from_favorites, Toast.LENGTH_SHORT).show();
                }
                isFavorite = !isFavorite;
                updateState();
            }
        });


        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        adView.loadAd(adRequest);

        MediaControllerCompat mediaController = MediaControllerCompat.getMediaController(MediaPlayerActivity.this);

        // Display the initial state
        MediaMetadataCompat metadata = mediaController.getMetadata();
        PlaybackStateCompat pbState = mediaController.getPlaybackState();

        if (pbState.getState() == (PlaybackStateCompat.STATE_PLAYING | PlaybackStateCompat.STATE_BUFFERING)) {
            mPlayPause.setImageResource(R.drawable.pause);
        } else {
            mPlayPause.setImageResource(R.drawable.play);
        }
        MediaDescriptionCompat description = metadata.getDescription();
        mSongTitle.setText(description.getTitle());

        setAlbumArt(description);

        int maxWithoutCasting = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
        mSeekBar.setMax(maxWithoutCasting);

        // Register a Callback to stay in sync
        mediaController.registerCallback(mControllerCallback);

        // Here
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Play method
                if (mMediaBrowser.isConnected()) {
                    int state = MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getPlaybackState().getState();

                    switch (state) {
                        case PlaybackStateCompat.STATE_NONE:
                            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();
                            break;
                        case PlaybackStateCompat.STATE_PAUSED:
                            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().play();
                            break;
                        case PlaybackStateCompat.STATE_PLAYING:
                            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().pause();
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

        if(bitmap == null){
            mAlbumArt.setImageResource(R.drawable.iconfinder_itunes);
        }else {
            mAlbumArt.setImageBitmap(bitmap);
        }
    }

    private void updateState() {
        if (isFavorite) {
            mLikeUnlike.setImageResource(R.drawable.heart);
        } else {
            mLikeUnlike.setImageResource(R.drawable.heart_outline);
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
                if (fromUser) {
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
        if (mMediaBrowser.isConnected()) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().skipToPrevious();
        }
    }

    public void nextSong(View view) {
        if (mMediaBrowser.isConnected()) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().skipToNext();
        }
    }

    public void stopService(View view) {
        if (mMediaBrowser.isConnected()) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().stop();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mMediaBrowser.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (MediaControllerCompat.getMediaController(MediaPlayerActivity.this) != null) {
            MediaControllerCompat.getMediaController(MediaPlayerActivity.this).unregisterCallback(mControllerCallback);
        }
        if (mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
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

    public void shuffle(View view) {
        MediaControllerCompat.getMediaController(MediaPlayerActivity.this).getTransportControls().setShuffleMode(0);
    }
}
