package com.example.vibhati.musiccorner;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.List;

public class MediaPlaybackService extends MediaBrowserServiceCompat {
    private MediaSessionCompat mMediaSession;
    private PlaybackStateCompat.Builder mStateBuilder;
    private String TAG = "MEDIA_SESSION_TAG";

    ////////////////////////

    private IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    // Defined elsewhere...
    private AudioManager.OnAudioFocusChangeListener afChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {

        }
    };
    private BecomingNoisyReceiver myNoisyAudioStreamReceiver = new BecomingNoisyReceiver();
    private Notification myPlayerNotification;
   // private MediaBrowserServiceCompat service;
    private MediaPlayer player;

    private AudioFocusRequest audioFocusRequest;

    //callback should be used to handle

    private String channelId = "channelId";
    private int id = 3;
    MediaSessionCompat.Callback callback = new MediaSessionCompat.Callback() {

        //2
        @Override
        public void onPlay() {

            //service should start here startService()
            //also put the service in the foreground


            ////// for the notification /////////////////

            // Given a media session and its context (usually the component containing the session)
            // Create a NotificationCompat.Builder

             // Get the session's metadata
            MediaControllerCompat controller = mMediaSession.getController();
            MediaMetadataCompat mediaMetadata = controller.getMetadata();
            MediaDescriptionCompat description = mediaMetadata.getDescription();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(MediaPlaybackService.this, channelId);

            builder
                    // Add the metadata for the currently playing track
                    .setContentTitle(description.getTitle())
                    .setContentText(description.getSubtitle())
                    .setSubText(description.getDescription())
                    .setLargeIcon(description.getIconBitmap())

                    // Enable launching the player by clicking the notification
                    .setContentIntent(controller.getSessionActivity())

                    // Stop the service when the notification is swiped away
                    .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(MediaPlaybackService.this,
                            PlaybackStateCompat.ACTION_STOP))

                    // Make the transport controls visible on the lockscreen
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

                    // Add an app icon and set its accent color
                    // Be careful about the color
                    .setSmallIcon(R.drawable.play)
                    .setColor(ContextCompat.getColor(MediaPlaybackService.this, R.color.colorPrimaryDark))

                    // Add a pause button
                    .addAction(new NotificationCompat.Action(
                            R.drawable.pause, getString(R.string.pause),
                            MediaButtonReceiver.buildMediaButtonPendingIntent(MediaPlaybackService.this,
                                    PlaybackStateCompat.ACTION_PLAY_PAUSE)))

                    // Take advantage of MediaStyle features
                    .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mMediaSession.getSessionToken())
                            .setShowActionsInCompactView(0)

                            // Add a cancel button
                            .setShowCancelButton(true)
                            .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(MediaPlaybackService.this,
                                    PlaybackStateCompat.ACTION_STOP)));

            myPlayerNotification = builder.build();

             // Display the notification and place the service in the foreground
//            startForeground(id, builder.build());

            ////// for the notification /////////////////

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
            }else {
                result = am.requestAudioFocus(afChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // Start the service
                startService(new Intent(getApplicationContext(), MediaBrowserServiceCompat.class));
                // Set the session active  (and update metadata and state)
                mMediaSession.setActive(true);
                // start the player (custom call)
                player.start();
                // Register BECOME_NOISY BroadcastReceiver
                registerReceiver(myNoisyAudioStreamReceiver, intentFilter);
                // Put the service in the foreground, post notification

//                service.startForeground(id, myPlayerNotification);
                startForeground(id, myPlayerNotification);
            }
        }

        @Override
        public void onStop() {

            //service should stop here stopService() or stopSelf()

            AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            // Abandon audio focus
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                am.abandonAudioFocusRequest(audioFocusRequest);
            }else{
                am.abandonAudioFocus(afChangeListener);
            }
            unregisterReceiver(myNoisyAudioStreamReceiver);
            // Stop the service
//            service.stopSelf();
            stopSelf();
            // Set the session inactive  (and update metadata and state)
            mMediaSession.setActive(false);
            // stop the player (custom call)
            player.stop();
            // Take the service out of the foreground
//            service.stopForeground(false);
            stopForeground(false);
        }

        @Override
        public void onPause() {
            AudioManager am = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
            // Update metadata and state
            // pause the player (custom call)
            player.pause();
            // unregister BECOME_NOISY BroadcastReceiver
            unregisterReceiver(myNoisyAudioStreamReceiver);
            // Take the service out of the foreground, retain the notification
//            service.stopForeground(false);
            stopForeground(false);
        }
    };

    ////////////////////////

    @Override
    public void onCreate() {
        super.onCreate();
        mMediaSession = new MediaSessionCompat(MediaPlaybackService.this, TAG);

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                        MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //custome action

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        mStateBuilder = new PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY | PlaybackStateCompat.ACTION_PLAY_PAUSE);
        mMediaSession.setPlaybackState(mStateBuilder.build());

        // MySessionCallback() has methods that handle callbacks from a media controller
        mMediaSession.setCallback(callback);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setClass(this, MediaButtonReceiver.class);
            mMediaSession.setMediaButtonReceiver(PendingIntent.getBroadcast(
                    this,
                    0,
                    mediaButtonIntent,
                    0));
        }

        setSessionToken(mMediaSession.getSessionToken());

        player = new MediaPlayer();
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String s, int i, @Nullable Bundle bundle) {
//        // (Optional) Control the level of access for the specified package name.
//        // You'll need to write your own logic to do this.
//        if (allowBrowsing(clientPackageName, clientUid)) {
//            // Returns a root ID that clients can use with onLoadChildren() to retrieve
//            // the content hierarchy.
//            return new BrowserRoot(MY_MEDIA_ROOT_ID, null);
//        } else {
//            // Clients can connect, but this BrowserRoot is an empty hierachy
//            // so onLoadChildren returns nothing. This disables the ability to browse for content.
//            return new BrowserRoot(MY_EMPTY_MEDIA_ROOT_ID, null);
//        }
        if (TextUtils.equals(s, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }
        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String s, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
//            //  Browsing not allowed
//            if (TextUtils.equals(MY_EMPTY_MEDIA_ROOT_ID, parentMediaId)) {
//                result.sendResult(null);
//                return;
//            }
//
//            // Assume for example that the music catalog is already loaded/cached.
//
//            List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
//
//            // Check if this is the root menu:
//            if (MY_MEDIA_ROOT_ID.equals(parentMediaId)) {
//                // Build the MediaItem objects for the top level,
//                // and put them in the mediaItems list...
//            } else {
//                // Examine the passed parentMediaId to see which submenu we're at,
//                // and put the children of that menu in the mediaItems list...
//            }
//            result.sendResult(mediaItems);
        result.sendResult(null);
    }

    public class BecomingNoisyReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        MediaButtonReceiver.handleIntent(mMediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }
}
