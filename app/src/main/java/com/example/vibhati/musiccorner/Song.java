package com.example.vibhati.musiccorner;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

@Entity(tableName = "song_table")
public class Song implements Parcelable {

    @PrimaryKey
    @NonNull
    private long id;

    private String title;

    private String artist;

    @ColumnInfo(name = "album_id")
    private long albumId;

    public Song(@NonNull long id, String title, String artist, long albumId) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.albumId = albumId;
    }

    public long getAlbumId() {
        return albumId;
    }

    public void setAlbumId(long albumId) {
        this.albumId = albumId;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Song) {
            Song song = (Song) obj;
            return this.id == song.getId();
        }
        return super.equals(obj);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    @Override
    public String toString() {
        return title + " - " + artist;
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.title);
        dest.writeString(this.artist);
        dest.writeLong(this.albumId);
    }

    protected Song(Parcel in) {
        this.id = in.readLong();
        this.title = in.readString();
        this.artist = in.readString();
        this.albumId = in.readLong();
    }

    public static final Creator<Song> CREATOR = new Creator<Song>() {
        @Override
        public Song createFromParcel(Parcel source) {
            return new Song(source);
        }

        @Override
        public Song[] newArray(int size) {
            return new Song[size];
        }
    };
}
