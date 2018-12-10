package com.example.vibhati.musiccorner;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MediaPlayerActivity extends AppCompatActivity {

    private static final String EXTRA_SONG = "extraSong";
    private SongViewModel mSongViewModel;
    private boolean isFavorite;
    private Button mButton;
    private Song mSong;
    private ArrayList<Song> mSongList;
    private List<Song> mFavoriteSongList;
    private static final String TAG = "MediaPlayerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_player);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mButton = findViewById(R.id.favorite_media_player);

        mSongList = MediaLibrary.getData(this);
        mSong = mSongList.get(PreferenceManager.getDefaultSharedPreferences(this).getInt("position", 0));

        Log.i(TAG,"from general"+mSong.toString());

        mSongViewModel = ViewModelProviders.of(this).get(SongViewModel.class);
        mSongViewModel.getAllSongs().observe(this, new Observer<List<Song>>() {
            @Override
            public void onChanged(@Nullable List<Song> songs) {
                Log.i(TAG,"list size: " + songs.size());
                mFavoriteSongList = songs;
                isFavorite = mFavoriteSongList.contains(mSong);

                for(Song song:songs)
                    Log.i(TAG,"from list"+song.toString());
                updateState();
            }
        });

        if(mFavoriteSongList == null){
            Log.i(TAG,"empty");
        }
        if(mSong == null){
            Log.i(TAG,"empty song");
        }

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isFavorite){
                    mSongViewModel.insert(mSong);

                }else{
                    mSongViewModel.deleteSong(mSong.getId());
                }
                isFavorite = !isFavorite;
                updateState();
            }
        });

    }

    private void updateState() {
        if(isFavorite){
            mButton.setText("unlike");
        }else{
            mButton.setText("like");
        }
    }
}
