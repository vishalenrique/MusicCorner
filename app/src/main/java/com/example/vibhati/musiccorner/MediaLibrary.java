package com.example.vibhati.musiccorner;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MediaLibrary {

    private static ArrayList<Song> songList;

    public static ArrayList<Song> getData(Context context){
        if(songList == null) {
            Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int titleColumn = cursor.getColumnIndex
                        (MediaStore.Audio.Media.TITLE);
                int idColumn = cursor.getColumnIndex
                        (MediaStore.Audio.Media._ID);
                int artistColumn = cursor.getColumnIndex
                        (MediaStore.Audio.Media.ARTIST);
                int albumId = cursor.getColumnIndex
                        (MediaStore.Audio.Media.ALBUM_ID);
                songList = new ArrayList<>();
                do {
                    long thisId = cursor.getLong(idColumn);
                    String thisTitle = cursor.getString(titleColumn);
                    String thisArtist = cursor.getString(artistColumn);
                    long thisAlbumId = cursor.getLong(albumId);
                    songList.add(new Song(thisId, thisTitle, thisArtist, thisAlbumId));
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
        }
        return songList;
    }
}
