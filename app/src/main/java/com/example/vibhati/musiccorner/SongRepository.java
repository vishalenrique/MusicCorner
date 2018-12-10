package com.example.vibhati.musiccorner;

import android.app.Application;
import android.arch.lifecycle.LiveData;
import android.os.AsyncTask;

import java.util.List;

public class SongRepository {

    private static SongRepository mSongRepository;
    private SongDao mSongDao;
    private LiveData<List<Song>> mAllSongs;

    private SongRepository(Application application) {
        SongRoomDatabase mDatabase = SongRoomDatabase.getInstance(application);
        mSongDao = mDatabase.songDao();
        mAllSongs = mSongDao.getAlphabetizedSongs();
    }

    public LiveData<List<Song>> getSongs(){
        return mAllSongs;
    }

    public static SongRepository getInstance(Application application) {

        if(mSongRepository == null){
            synchronized (SongRepository.class) {
                if(mSongRepository == null)
                mSongRepository = new SongRepository(application);
            }
        }
        return mSongRepository;
    }

    public void insert(Song song) {
        new InsertAsyncTask(mSongDao).execute(song);
    }

    public void deleteSong(long id) {
        new DeleteAsyncTask(mSongDao).execute(id);
    }

    private static class DeleteAsyncTask extends AsyncTask<Long,Void,Void>{

        private SongDao mSongDao;

        DeleteAsyncTask(SongDao dao) {
            mSongDao = dao;
        }

        @Override
        protected Void doInBackground(Long... longs) {
             mSongDao.deleteSong(longs[0]);
             return null;
        }
    }

    private static class InsertAsyncTask extends AsyncTask<Song, Void, Void> {

        private SongDao mSongDao;

        InsertAsyncTask(SongDao dao) {
            mSongDao = dao;
        }

        @Override
        protected Void doInBackground(final Song... params) {
            mSongDao.insert(params[0]);
            return null;
        }
    }
}
