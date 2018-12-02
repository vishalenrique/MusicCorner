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
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int NOTIFICATION_ID = 234;
    private static String channelId = "channelId";
    private LocalBinder mLocalBinder = new LocalBinder();
    private static final String TAG = "MediaPlaybackService";
    private static int mThreadId;
    private MediaPlayer mMediaPlayer;
    private boolean mIsReady = false;
    private ArrayList<Song> mSongList;
    private SharedPreferences mDefaultSharedPreferences;
    private boolean mIsPlaying = false;
    private String mDefaultValueInPrepare = "defaultValueInPrepare";
    ;
    private int mPosition;
    private AudioFocusRequest audioFocusRequest;
    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
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
    private MediaSessionCompat mMediaSession;
    private MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
//            AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
//            // Request audio focus for playback, this registers the afChangeListener
//            AudioAttributes attrs = null;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
//                attrs = new AudioAttributes.Builder()
//                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                        .build();
//            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
//                        .setOnAudioFocusChangeListener(afChangeListener)
//                        .setAudioAttributes(attrs)
//                        .setAcceptsDelayedFocusGain(true)
//                        .build();
//            }
//            int result = 0;
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//                result = am.requestAudioFocus(audioFocusRequest);
//            } else {
//                result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
//            }
//            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
//                playMusic();
//            }
            Log.i(TAG,"onPlay called");
        }

        @Override
        public void onPause() {
//            pauseMusic();
            Log.i(TAG,"onPause called");
        }

        @Override
        public void onStop() {
//            stopMusic();
            Log.i(TAG,"onStop called");
        }

        @Override
        public void onSkipToPrevious() {
//            previousSong();
            Log.i(TAG,"onSkipToPrevious called");

        }

        @Override
        public void onSkipToNext() {
//            nextSong();
            Log.i(TAG,"onSkipToNext called");
        }
    };
    private PlaybackStateCompat.Builder mPlaybackStateCompatBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataCompatBuilder;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate called: " + String.valueOf(mThreadId));
        //startService(new Intent(this.getApplicationContext(), MediaPlaybackService.class));

        mMediaSession = new MediaSessionCompat(MediaPlaybackService.this, TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSession.setCallback(mCallback);

        mPlaybackStateCompatBuilder = new PlaybackStateCompat.Builder();
        mPlaybackStateCompatBuilder.setActions(
                PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);

        setSessionToken(mMediaSession.getSessionToken());

        mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
       // mDefaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        mMediaMetadataCompatBuilder = new MediaMetadataCompat.Builder();
        mSongList = MediaLibrary.getData(getApplicationContext());
        Log.i(TAG, "onCreate called: end " + String.valueOf(mThreadId));
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mIsReady = true;
        playMusic();
    }

    public static NotificationCompat.Builder from(Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);

        builder
                .setContentTitle(description.getTitle())
//                .setContentText(description.getSubtitle())
//                .setSubText(description.getDescription())
//                .setLargeIcon(description.getIconBitmap())
                .setContentIntent(controller.getSessionActivity())
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setSmallIcon(R.drawable.play)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))

        // Add a pause button
                .addAction(new NotificationCompat.Action(
                            R.drawable.pause, context.getString(R.string.pause),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)))
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0)
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
        return builder;
    }

    void playMusic() {
            if (!mMediaPlayer.isPlaying()) {
                if (mIsReady) {

                    startForeground(NOTIFICATION_ID,from(getApplicationContext(),mMediaSession).build());

                    mMediaSession.setMetadata(mMediaMetadataCompatBuilder.build());

                    mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), 1.0f);
                    mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());

                    mMediaSession.setActive(true);

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

    private void updatePlayingStateToSharedPreference(boolean state) {
        SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
        editor.putBoolean("isPlaying", state);
        editor.apply();
    }

    void pauseMusic() {
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), 0);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            stopForeground(false);
            updatePlayingStateToSharedPreference(false);
            unregisterReceiver(myNoisyAudioStreamReceiver);
        }
    }

    void stopMusic() {
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        // Abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            am.abandonAudioFocus(afChangeListener);
        }
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            updatePlayingStateToSharedPreference(false);
        }
        mMediaPlayer.reset();
        mIsReady = false;
        unregisterReceiver(myNoisyAudioStreamReceiver);
        stopForeground(true);
        mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), 0);
        mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        mMediaSession.setActive(false);

    }

    void prepare() {
        mPosition = mDefaultSharedPreferences.getInt("position", -1);
        if (mPosition == -1) {
            Log.i(TAG, "Select a song to play");
        } else {
            Song playSong = mSongList.get(mPosition);

            mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, playSong.getTitle());
            mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playSong.getArtist());
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
        Log.i(TAG, "onStartCommand called: " + String.valueOf(startId));
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        Log.i(TAG, "onBind called: " + String.valueOf(mThreadId));
//        return mLocalBinder;
//    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {
        if (TextUtils.equals(s, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }

    public void releaseResources() {
        Log.i(TAG, "releaseResources called: " + String.valueOf(mThreadId));
        stopSelf(mThreadId);
    }

    public void setSongs(ArrayList<Song> songList) {
        mSongList = songList;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("key")) {
            Log.i(TAG, "onSharedPreferenceChanged called: " + sharedPreferences.getString(key, "defaultValue"));
        }
    }

    public void previousSong() {
        if (mPosition >= 0) {
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt("position", --mPosition);
            editor.commit();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, mMediaPlayer.getCurrentPosition(), 0);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            prepare();
        }

    }

    public void nextSong() {
        if (mPosition == mSongList.size()) {
        } else {
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt("position", ++mPosition);
            editor.commit();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, mMediaPlayer.getCurrentPosition(), 0);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            prepare();
        }
    }

    class LocalBinder extends Binder {
        MediaPlaybackService getService() {
            return MediaPlaybackService.this;
        }
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        Log.i(TAG, "unbindService called: " + String.valueOf(mThreadId));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        if (mMediaSession != null) {
            mMediaSession.release();
            mMediaSession = null;
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        Log.i(TAG, "onDestroy called: " + String.valueOf(mThreadId));

    }

    public void showLogs() {
        Log.i(TAG, "showLogs called: " + String.valueOf(mThreadId));
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
