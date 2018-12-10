package com.example.vibhati.musiccorner;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

@Database(entities = {Song.class},version = 1)
public abstract class SongRoomDatabase extends RoomDatabase {
    public abstract SongDao songDao();
    public static SongRoomDatabase songRoomDatabase;

    public static SongRoomDatabase getInstance(final Context context){
        if(songRoomDatabase == null){
            synchronized (SongRoomDatabase.class){
                if(songRoomDatabase == null){
                    songRoomDatabase = Room.databaseBuilder(context.getApplicationContext(),
                            SongRoomDatabase.class,"song_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return songRoomDatabase;
    }
}
