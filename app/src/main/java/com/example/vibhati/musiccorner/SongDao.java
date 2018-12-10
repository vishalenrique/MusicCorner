package com.example.vibhati.musiccorner;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface SongDao {

    @Query("SELECT * FROM song_table ORDER BY artist ASC")
    LiveData<List<Song>> getAlphabetizedSongs();

    @Query("SELECT * FROM song_table WHERE id = :songName")
    LiveData<List<Song>> getSongWithId(long songName);

    @Insert
    void insert(Song song);

    @Query("DELETE FROM song_table WHERE id = :songId")
    void deleteSong(long songId);

    @Query("DELETE FROM song_table")
    void deleteAll();

}
