package com.ttop.cassette.loader;

import androidx.annotation.NonNull;

import com.ttop.cassette.discog.Discography;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.sort.SongSortOrder;
import com.ttop.cassette.sort.SortOrder;
import com.ttop.cassette.util.PreferenceUtil;
import com.ttop.cassette.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Karim Abou Zeid (kabouzeid)
 * @author SC (soncaokim)
 */
public class SongLoader {
    @NonNull
    public static ArrayList<Song> getAllSongs() {
        ArrayList<Song> songs = Discography.getInstance().getAllSongs();
        Collections.sort(songs, getSortOrder());
        return songs;
    }

    @NonNull
    public static ArrayList<Song> getSongs(@NonNull final String query) {
        final String strippedQuery = StringUtil.stripAccent(query.toLowerCase());

        ArrayList<Song> songs = new ArrayList<>();
        for (Song song : Discography.getInstance().getAllSongs()) {
            final String strippedTitle = StringUtil.stripAccent(song.title.toLowerCase());
            if (strippedTitle.contains(strippedQuery)) {
                songs.add(song);
            }
        }
        Collections.sort(songs, getSortOrder());
        return songs;
    }

    @NonNull
    public static Comparator<Song> getSortOrder() {
        SortOrder<Song> sortOrder = SongSortOrder.fromPreference(PreferenceUtil.getInstance().getSongSortOrder());
        return sortOrder.comparator;
    }
}
