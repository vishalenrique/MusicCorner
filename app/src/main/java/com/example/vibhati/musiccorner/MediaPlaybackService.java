package com.example.vibhati.musiccorner;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MediaPlaybackService extends MediaBrowserServiceCompat implements MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {

    private static final int NOTIFICATION_ID = 234;
    private static final int PENDING_INTENT_REQUEST_CODE = 32;
    public static final String ACTION_WIDGET_PLAY = "actionWidgetPlay";
    public static final String ACTION_WIDGET_PREVIOUS = "actionWidgetPrevious";
    public static final String ACTION_WIDGET_NEXT = "actionWidgetNext";
    private static final int PENDING_INTENT_FLAG = 0;
    private static final int PREF_POSITION_DEF = 0;
    private static final int PENDING_INTENT_REQUEST_CODE_NOTIFICATION = 345;
    private static final long INITIAL_DELAY_SEEKBAR_UPDATE = 0;
    private static final float PLAYBACK_SPEED = 1.0f;
    private static final float PLAYBACK_SPEED_ZERO = 0;
    private static final long CURRENT_POSITION_ZERO = 0;
    private static final int CUSTOM_STATE_SHUFFLE = -4;
    private static String channelId = "channelId";
    private static final String TAG = MediaPlaybackService.class.getSimpleName();
    private MediaPlayer mMediaPlayer;
    private ArrayList<Song> mSongList;
    private SharedPreferences mDefaultSharedPreferences;
    private boolean mIsPlaying = false;
    private String mDefaultValueInPrepare = "defaultValueInPrepare";
    private ScheduledExecutorService mExecutor;
    private Runnable mSeekBarPositionUpdateTask;
    private int mPosition;
    private AudioFocusRequest audioFocusRequest;
    public static final int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 1000;


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
        }

        @Override
        public void onPause() {
           pauseMusic();
        }

        @Override
        public void onSeekTo(long pos) {
            seekToPosition(pos);
        }

        @Override
        public void onStop() {
           stopMusic();
        }

        @Override
        public void onSkipToPrevious() {
            previousSong();
        }

        @Override
        public void onSkipToNext() {
           nextSong();
        }

        @Override
        public void onSetShuffleMode(int shuffleMode) {
            int size = mIsFavoriteMode?mFavoriteSongList.size():mSongList.size();
            Random random = new Random();
            mDefaultSharedPreferences.edit().putInt(getString(R.string.position), random.nextInt(size)).commit();
            mPlaybackStateCompatBuilder.setState(CUSTOM_STATE_SHUFFLE,mMediaPlayer.getCurrentPosition(),PLAYBACK_SPEED);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            playMusic();
        }
    };

    private void seekToPosition(long pos) {
        int newPos = (int) pos;
        mMediaPlayer.seekTo(newPos);
    }

    private PlaybackStateCompat.Builder mPlaybackStateCompatBuilder;
    private MediaMetadataCompat.Builder mMediaMetadataCompatBuilder;
    private List<Song> mFavoriteSongList;
    private boolean mIsFavoriteMode;


    @Override
    public void onCreate() {
        super.onCreate();
        mMediaSession = new MediaSessionCompat(MediaPlaybackService.this, TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        mMediaSession.setCallback(mCallback);

        mPlaybackStateCompatBuilder = new PlaybackStateCompat.Builder();
        mPlaybackStateCompatBuilder.setActions(
               PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE | PlaybackStateCompat.ACTION_SEEK_TO | PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED | PlaybackStateCompat.ACTION_PLAY |PlaybackStateCompat.ACTION_STOP | PlaybackStateCompat.ACTION_PLAY_PAUSE | PlaybackStateCompat.ACTION_PAUSE | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS | PlaybackStateCompat.ACTION_SKIP_TO_NEXT);
        setSessionToken(mMediaSession.getSessionToken());

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
            mMediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(
                    getApplicationContext(), PENDING_INTENT_REQUEST_CODE,
                    mediaButtonIntent, PENDING_INTENT_FLAG));
        }

        mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mDefaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mSongList = MediaLibrary.getInstance().getData(getApplicationContext());

        SongRepository.getInstance(getApplication()).getSongs().observeForever(new Observer<List<Song>>() {
            @Override
            public void onChanged(@Nullable List<Song> songs) {
                mFavoriteSongList = songs;
            }
        });

        mMediaMetadataCompatBuilder = new MediaMetadataCompat.Builder();

        mPosition = mDefaultSharedPreferences.getInt(getString(R.string.position), PREF_POSITION_DEF);

        Song playSong = mSongList.get(mPosition);

        SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
        editor.putString(MusicWidget.SONG_NAME,playSong.getTitle());
        editor.putString(MusicWidget.ALBUM_ART,playSong.getAlbumUri());
        editor.commit();

        mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, playSong.getTitle());
        mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playSong.getArtist());
        mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,playSong.getAlbumUri());
        mMediaMetadataCompatBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,playSong.getDuration());
        mMediaSession.setMetadata(mMediaMetadataCompatBuilder.build());
    }

    public static NotificationCompat.Builder from(Context context, MediaSessionCompat mediaSession) {
        MediaControllerCompat controller = mediaSession.getController();
        MediaMetadataCompat mediaMetadata = controller.getMetadata();
        MediaDescriptionCompat description = mediaMetadata.getDescription();

        if(description == null){
            Log.i(TAG,context.getString(R.string.metadata_unavailable_description));
            return null;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId);

        Bitmap bitmap = null;
        try {
             bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), description.getIconUri());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(bitmap == null){
            bitmap = BitmapFactory.decodeResource(context.getResources(),R.drawable.roundedsquare_music_symbol);
        }

        builder
                .setContentTitle(description.getTitle())
                .setContentText(description.getSubtitle())
                .setSubText(description.getDescription())
                .setLargeIcon(bitmap)
                .setContentIntent(PendingIntent.getActivity(context,PENDING_INTENT_REQUEST_CODE_NOTIFICATION,new Intent(context,SongsActivity.class),PENDING_INTENT_FLAG))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP))
                .setSmallIcon(R.drawable.play_icon)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimaryDark));

                builder.addAction(new NotificationCompat.Action(
                R.drawable.previous_icon, context.getString(R.string.skip_to_previous),
                MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)));

                if(controller.getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING) {
                    // Add a pause button
                builder.addAction(new NotificationCompat.Action(
                            R.drawable.pause_icon, context.getString(R.string.pause),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
                }else if(controller.getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){
                    builder.addAction(new NotificationCompat.Action(
                            R.drawable.play_icon, context.getString(R.string.play),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PLAY_PAUSE)));
                }
                builder.addAction(new NotificationCompat.Action(
                        R.drawable.next_icon, context.getString(R.string.skip_to_next),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)));
                builder.setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(1)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)));
        return builder;
    }

    void playMusic() {
        startService(new Intent(getApplicationContext(),MediaPlaybackService.class));
        prepare();
    }

    void startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if (mSeekBarPositionUpdateTask == null) {
            mSeekBarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    updateProgressCallbackTask();
                }
            };
        }
        mExecutor.scheduleAtFixedRate(
                mSeekBarPositionUpdateTask,
                INITIAL_DELAY_SEEKBAR_UPDATE,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopUpdatingCallbackWithPosition(boolean resetUIPlaybackPosition) {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
            mSeekBarPositionUpdateTask = null;
            if (resetUIPlaybackPosition ) {
                mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_STOPPED,mMediaPlayer.getCurrentPosition(),PLAYBACK_SPEED);
                mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            }
        }
    }

    private void updateProgressCallbackTask() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            int currentPosition = mMediaPlayer.getCurrentPosition();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PLAYING,currentPosition,PLAYBACK_SPEED);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        }
    }

    void pauseMusic() {
            mMediaPlayer.pause();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PAUSED, mMediaPlayer.getCurrentPosition(), PLAYBACK_SPEED_ZERO);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
            notificationManagerCompat.notify(NOTIFICATION_ID,from(getApplicationContext(),mMediaSession).build());
            stopForeground(false);

            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = defaultSharedPreferences.edit();
            editor.putBoolean(MusicWidget.isPlaying,false);
            editor.commit();

            Intent intent = new Intent(this, MusicWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getApplication(), MusicWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);
            unregisterReceiver(myNoisyAudioStreamReceiver);
    }

    void stopMusic() {
        int state = mMediaSession.getController().getPlaybackState().getState();
        if(state == PlaybackStateCompat.STATE_PLAYING){
            unregisterReceiver(myNoisyAudioStreamReceiver);
        }
        mMediaPlayer.stop();
        mMediaPlayer.reset();
        AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        stopUpdatingCallbackWithPosition(true);

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = defaultSharedPreferences.edit();
        editor.putBoolean(MusicWidget.isPlaying,false);
        editor.commit();

        Intent intent = new Intent(this, MusicWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());
        int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getApplication(), MusicWidget.class));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

        // Abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            am.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            am.abandonAudioFocus(afChangeListener);
        }
        mMediaSession.setActive(false);
        stopSelf();
        mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_STOPPED, mMediaPlayer.getCurrentPosition(), PLAYBACK_SPEED_ZERO);
        mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        stopForeground(true);

    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        // initializeProgressCallback();
        mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), PLAYBACK_SPEED);
        mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        mMediaPlayer.start();
        startUpdatingCallbackWithPosition();
    }

    void prepare() {
        if(mMediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PAUSED){
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PLAYING, mMediaPlayer.getCurrentPosition(), PLAYBACK_SPEED);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());

            startForeground(NOTIFICATION_ID, from(getApplicationContext(), mMediaSession).build());

            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = defaultSharedPreferences.edit();
            editor.putBoolean(MusicWidget.isPlaying,true);
            editor.commit();

            Intent intent = new Intent(this, MusicWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getApplication(), MusicWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);

            registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
            mMediaPlayer.start();
        }else {
            mPosition = mDefaultSharedPreferences.getInt(getString(R.string.position), PREF_POSITION_DEF);

            mIsFavoriteMode = mDefaultSharedPreferences.getBoolean(getString(R.string.isFavoriteMode), false);
            Song playSong = mIsFavoriteMode ? mFavoriteSongList.get(mPosition) : mSongList.get(mPosition);

            mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, playSong.getTitle());
            mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, playSong.getArtist());
            mMediaMetadataCompatBuilder.putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,playSong.getAlbumUri());
            mMediaMetadataCompatBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION,playSong.getDuration());
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
                Log.e(TAG, getString(R.string.error_message), e);
            }

            mMediaSession.setMetadata(mMediaMetadataCompatBuilder.build());

            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_PLAYING,CURRENT_POSITION_ZERO, PLAYBACK_SPEED);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());

            mMediaSession.setActive(true);

            startForeground(NOTIFICATION_ID, from(getApplicationContext(), mMediaSession).build());

            SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = defaultSharedPreferences.edit();
            editor.putString(MusicWidget.SONG_NAME,playSong.getTitle());
            editor.putString(MusicWidget.ALBUM_ART,playSong.getAlbumUri());
            editor.putBoolean(MusicWidget.isPlaying,true);
            editor.commit();

            Intent intent = new Intent(this, MusicWidget.class);
            intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(getApplication());
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(getApplication(), MusicWidget.class));
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            sendBroadcast(intent);

            registerReceiver(myNoisyAudioStreamReceiver, intentFilter);

            mMediaPlayer.prepareAsync();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent !=null) {
            String action = intent.getAction();
            if (!TextUtils.isEmpty(action)) {
                switch (action) {
                    case ACTION_WIDGET_PLAY:
                        if(mMediaSession.getController().getPlaybackState().getState() == PlaybackStateCompat.STATE_PLAYING)
                        mCallback.onPause();
                        else
                        mCallback.onPlay();
                        break;
                    case ACTION_WIDGET_PREVIOUS:
                        previousSong();
                        break;
                    case ACTION_WIDGET_NEXT:
                        nextSong();
                        break;
                    default:
                        break;
                }
            }
        }
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

    public void setSongs(ArrayList<Song> songList) {
        mSongList = songList;
    }

    public void previousSong() {
        if (mPosition > 0) {
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt(getString(R.string.position), --mPosition);
            editor.commit();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_PREVIOUS, CURRENT_POSITION_ZERO, PLAYBACK_SPEED_ZERO);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            playMusic();
        }

    }

    public void nextSong() {
        int size = mIsFavoriteMode?mFavoriteSongList.size():mSongList.size();
        if (mPosition < size-1) {
            SharedPreferences.Editor editor = mDefaultSharedPreferences.edit();
            editor.putInt(getString(R.string.position), ++mPosition);
            editor.commit();
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT, CURRENT_POSITION_ZERO, PLAYBACK_SPEED_ZERO);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
            playMusic();
        }
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
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        int state = mMediaSession.getController().getPlaybackState().getState();
        int size = mIsFavoriteMode?mFavoriteSongList.size():mSongList.size();
        if(mPosition == (size-1)){
            mPlaybackStateCompatBuilder.setState(PlaybackStateCompat.STATE_NONE,mp.getCurrentPosition(),PLAYBACK_SPEED);
            mMediaSession.setPlaybackState(mPlaybackStateCompatBuilder.build());
        }else if(state == PlaybackStateCompat.STATE_PLAYING){
            nextSong();
        }
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
