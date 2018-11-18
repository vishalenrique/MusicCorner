package com.example.vibhati.musiccorner;

import android.content.Context;
import android.media.session.MediaSession;
import android.support.annotation.NonNull;
import android.widget.MediaController;

public class MusicController extends MediaController {

    public MusicController(Context context) {
        super(context);
    }

    @Override
    public void hide() {
    }
}
