package com.example.vibhati.musiccorner;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{

    private Context mContext;
    private Cursor mCursor;

    public SongAdapter(Context mContext, Cursor mCursor) {
        this.mContext = mContext;
        this.mCursor = mCursor;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.list_item, viewGroup, false);

        SongViewHolder songViewHolder = new SongViewHolder(view);
        return songViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder songViewHolder, int position) {
        mCursor.moveToPosition(position);
        songViewHolder.titleTextView.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
        songViewHolder.artistTextView.setText(mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
    }

    @Override
    public int getItemCount() {
        return mCursor!=null?mCursor.getCount():0;
    }

    public void dataChanged(Cursor musicCursor) {
            mCursor = musicCursor;
            notifyDataSetChanged();
    }

    class SongViewHolder extends RecyclerView.ViewHolder{

       ImageView imageView;
       TextView titleTextView;
       TextView artistTextView;

       public SongViewHolder(@NonNull View itemView) {
           super(itemView);
           titleTextView = itemView.findViewById(R.id.tv_main_title);
           artistTextView = itemView.findViewById(R.id.tv_main_artist);
       }
   }
}
