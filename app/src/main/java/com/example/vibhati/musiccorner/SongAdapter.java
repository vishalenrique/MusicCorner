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

import java.util.List;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{

    private Context mContext;
    private List<Song> mCursor;
    private ClickListener mClickListener;


    public interface ClickListener{
        public void onClick(int position);
    }

    public SongAdapter(Context mContext, List<Song> mCursor, ClickListener mClickListener) {
        this.mContext = mContext;
        this.mCursor = mCursor;
        this.mClickListener = mClickListener;
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
        Song song = mCursor.get(position);
        songViewHolder.titleTextView.setText(song.getTitle());
        songViewHolder.artistTextView.setText(song.getArtist());

        final int currentPosition = position;
        songViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClickListener.onClick(currentPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mCursor!=null?mCursor.size():0;
    }

    public void dataChanged(List<Song> musicCursor) {
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
