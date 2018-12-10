package com.example.vibhati.musiccorner;

import android.app.Application;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;

import java.util.List;

public class SongViewModel extends AndroidViewModel {

    private LiveData<List<Song>> mAllSongs;
    private SongRepository mRepository;

    public SongViewModel(@NonNull Application application) {
        super(application);
        mRepository = SongRepository.getInstance(application);
        mAllSongs = mRepository.getSongs();
    }

    public LiveData<List<Song>> getAllSongs() {
        return mAllSongs;
    }

    public void insert(Song song){
        mRepository.insert(song);
    }

    public void deleteSong(long id){
        mRepository.deleteSong(id);
    }
}
