package com.ttop.cassette.loader;

import androidx.annotation.NonNull;

import com.ttop.cassette.discog.Discography;
import com.ttop.cassette.model.Album;
import com.ttop.cassette.sort.AlbumSortOrder;
import com.ttop.cassette.sort.SortOrder;
import com.ttop.cassette.util.StringUtil;
import com.ttop.cassette.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author Karim Abou Zeid (kabouzeid)
 * @author SC (soncaokim)
 */
public class AlbumLoader {
    @NonNull
    public static ArrayList<Album> getAllAlbums() {
        ArrayList<Album> albums = Discography.getInstance().getAllAlbums();
        Collections.sort(albums, getSortOrder());
        return albums;
    }

    @NonNull
    public static ArrayList<Album> getAlbums(String query) {
        final String strippedQuery = StringUtil.stripAccent(query.toLowerCase());

        ArrayList<Album> albums = new ArrayList<>();
        for (Album album : Discography.getInstance().getAllAlbums()) {
            final String strippedAlbum = StringUtil.stripAccent(album.getTitle().toLowerCase());
            if (strippedAlbum.contains(strippedQuery)) {
                albums.add(album);
            }
        }
        Collections.sort(albums, getSortOrder());
        return albums;
    }

    @NonNull
    public static Album getAlbum(long albumId) {
        Album album = Discography.getInstance().getAlbum(albumId);
        if (album != null) {
            return album;
        } else {
            return new Album();
        }
    }

    @NonNull
    private static Comparator<Album> getSortOrder() {
        SortOrder<Album> sortOrder = AlbumSortOrder.fromPreference(PreferenceUtil.getInstance().getAlbumSortOrder());
        return sortOrder.comparator;
    }
}
