package com.example.vibhati.musiccorner;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class FavoriteSongsFragment extends Fragment implements SongAdapter.ClickListener {

    private RecyclerView mRecyclerView;
    private SongAdapter mAdapter;
    private static final String TAG = "FavoriteSongsFragment";
    private ArrayList<Song> songList;
    public OnSongClickListener mSongClickListener;

    @Override
    public void onClick(Song song, int position) {
        mSongClickListener.onSongClicked(song,position);
    }

    static FavoriteSongsFragment newInstance() {
        return new FavoriteSongsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_songs_main, container, false);
        mRecyclerView = rootView.findViewById(R.id.rv_main);
        mAdapter = new SongAdapter(getActivity(), null, this);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setAdapter(mAdapter);
        updateSongs();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSongClickListener = (OnSongClickListener) context;
    }

    private void updateSongs() {
        songList = MediaLibrary.getData(getActivity());
        Log.i(TAG,"songsList" + songList.size());
        mAdapter.dataChanged(songList);
    }

}
