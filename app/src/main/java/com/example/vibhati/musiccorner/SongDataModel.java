package com.example.vibhati.musiccorner;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.MutableLiveData;
import android.database.Cursor;
import android.support.annotation.NonNull;

public class SongDataModel extends AndroidViewModel {
    SongLiveData songLiveData;

    public SongLiveData getSongLiveData() {
        return songLiveData;
    }

    public SongDataModel(@NonNull Application application) {
        super(application);
        songLiveData = new SongLiveData(application);
    }
}
