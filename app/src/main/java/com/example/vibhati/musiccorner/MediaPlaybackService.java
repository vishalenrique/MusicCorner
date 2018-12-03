package com.example.vibhati.musiccorner;

import android.app.PendingIntent;
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

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener {

    private static final int NOTIFICATION_ID = 234;
    private static final int PENDING_INTENT_REQUEST_CODE = 32;
    private static String channelId = "channelId";
    private static final String TAG = "MediaPlaybackService";
    private MediaPlayer mMediaPlayer;
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
            }
        }
    };

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
    private MediaSessionCompat mMediaSession;
    private MediaSessionCompat.Callback mCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onPlay() {
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
            } else {
                result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                playMusic();
            }
            Log.i(TAG, "onPlay called");
        }

        @Override
        public void onPause() {
           pauseMusic();
            Log.i(TAG, "onPause called");
        }

        @Override
        public void onStop() {
           stopMusic();
            Log.i(TAG, "onStop called");
        }

        @Override
        public void onSkipToPrevious() {
            previousSong();
            Log.i(TAG, "onSkipToPrevious called");

        }

        @Override
        public void onSkipToNext() {
           nextSong();
            Log.i(TAG, "onSkipToNext called");
        }
    };
    private PlaybackStateCompat.Builder mPlaybackStateCompatBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataCompatBuilder;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate called: ");
        mMediaSession = new MediaSessionCompat(MediaPlaybackService.this, TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSession.setCallback(mCallback);

        mPlaybackStateCompatBuilder = new PlaybackStateCompat.Builder();
        mPlaybackStateCompatBuilder.setActions(
                PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED | PlaybackStateCompat.ACTION_PLAY |PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);

        setSessionToken(mMediaSession.getSessionToken());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
            mMediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(
                    getApplicationContext(), PENDING_INTENT_REQUEST_CODE,
                    mediaButtonIntent,
                    0));
        }

        mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mSongList = MediaLibrary.getData(getApplicationContext());

        mMediaMetadataCompatBuilder = new MediaMetadataCompat.Builder();

        mPosition = mDefaultSharedPreferences.getInt("position", 0);

        Song playSong = mSongList.get(mPosition);

        mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, playSong.getTitle());
        mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playSong.getArtist());
        mMediaSession.setMetadata(mMediaMetadataCompatBuilder.build());

        Log.i(TAG, "onCreate called: end ");
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mMediaPlayer.start();
    }

    public static NotificationCompat.Builder from(Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        if(description == null){
            Log.i(TAG," MediaDescriptionCompat: null");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);

        builder
                .setContentTitle(description.getTitle())
//                .setContentText(description.getSubtitle())
//                .setSubText(description.getDescription())
//                .setLargeIcon(description.getIconBitmap())
                .setContentIntent(PendingIntent.getActivity(context,345,new Intent(context,SongsActivity.class),0))
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
        startService(new Intent(getApplicationContext(),MediaPlaybackService.class));
        prepare();
    }

    void pauseMusic() {
            mMediaPlayer.pause();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), 0);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            stopForeground(false);
            unregisterReceiver(myNoisyAudioStreamReceiver);
    }

    void stopMusic() {
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        // Abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            am.abandonAudioFocus(afChangeListener);
        }
        mMediaSession.setActive(false);
        stopSelf();
        mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_STOPPED, mMediaPlayer.getCurrentPosition(), 0);
        mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
//        unregisterReceiver(myNoisyAudioStreamReceiver);
        stopForeground(true);

    }

    void prepare() {
        if(mMediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), 1.0f);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());

            startForeground(NOTIFICATION_ID, from(getApplicationContext(), mMediaSession).build());

            registerReceiver(myNoisyAudioStreamReceiver, intentFilter);

            mMediaPlayer.start();
        }else {
            mPosition = mDefaultSharedPreferences.getInt("position", 0);

            Song playSong = mSongList.get(mPosition);

            mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, playSong.getTitle());
            mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playSong.getArtist());
            //get id
            long currSong = playSong.getId();
            //set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            try {
                mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
            } catch (Exception e) {
                Log.e("MUSIC SERVICE", "Error setting data source", e);
            }

            mMediaSession.setMetadata(mMediaMetadataCompatBuilder.build());

            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), 1.0f);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());

            mMediaSession.setActive(true);

            startForeground(NOTIFICATION_ID, from(getApplicationContext(), mMediaSession).build());

            registerReceiver(myNoisyAudioStreamReceiver, intentFilter);

            mMediaPlayer.prepareAsync();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand called: " + String.valueOf(startId));
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

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
        Log.i(TAG, "releaseResources called: ");
    }

    public void setSongs(ArrayList<Song> songList) {
        mSongList = songList;
    }

    public void previousSong() {
        if (mPosition > 0) {
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt("position", --mPosition);
            editor.commit();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, mMediaPlayer.getCurrentPosition(), 0);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            playMusic();
        }

    }

    public void nextSong() {
        if (mPosition < (mSongList.size()-1)) {
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt("position", ++mPosition);
            editor.commit();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, mMediaPlayer.getCurrentPosition(), 0);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            playMusic();
        }
    }

    @Override
    public void unbindService(ServiceConnection conn) {
        super.unbindService(conn);
        Log.i(TAG, "unbindService called: ");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        if (mMediaSession != null) {
            mMediaSession.release();
            mMediaSession = null;
        }
        Log.i(TAG, "onDestroy called: ");

    }

    public void showLogs() {
        Log.i(TAG, "showLogs called: ");
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
