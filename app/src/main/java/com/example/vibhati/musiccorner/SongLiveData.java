package com.example.vibhati.musiccorner;

import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

public class SongLiveData extends MutableLiveData<Cursor> {
    final Context context;
    public SongLiveData(Context context) {
        this.context = context;
        loadSongs();
    }

    private void loadSongs() {
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,null);
        setValue(cursor);
    }
}
