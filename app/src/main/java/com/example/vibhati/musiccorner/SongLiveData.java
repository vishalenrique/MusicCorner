package com.example.vibhati.musiccorner;

import android.annotation.SuppressLint;
import android.arch.lifecycle.MutableLiveData;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SongLiveData extends MutableLiveData<List<Song>> {
    final Context context;

    public SongLiveData(Context context) {
        this.context = context;
        loadSongs();
    }


    private void loadSongs() {
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        List<Song> songList = null;
        if (cursor != null && cursor.moveToFirst()) {
            int titleColumn = cursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = cursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = cursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            songList = new ArrayList<>();
            do {
                long thisId = cursor.getLong(idColumn);
                String thisTitle = cursor.getString(titleColumn);
                String thisArtist = cursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (cursor.moveToNext());

            Collections.sort(songList, new Comparator<Song>() {
                public int compare(Song a, Song b) {
                    return a.getTitle().compareToIgnoreCase(b.getTitle());
                }
            });
        }
        if (cursor != null) {
            cursor.close();
        }
        setValue(songList);
    }
}
