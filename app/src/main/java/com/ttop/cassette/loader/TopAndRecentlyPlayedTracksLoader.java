/*
* Copyright (C) 2014 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.ttop.cassette.loader;

import android.content.Context;
import android.database.Cursor;

import androidx.annotation.NonNull;

import com.ttop.cassette.discog.Discography;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.provider.HistoryStore;
import com.ttop.cassette.provider.SongPlayCountStore;
import com.ttop.cassette.provider.StoreLoader;
import com.ttop.cassette.sort.SongSortOrder;
import com.ttop.cassette.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.Collections;

public class TopAndRecentlyPlayedTracksLoader {
    @NonNull
    public static ArrayList<Song> getRecentlyPlayedTracks(@NonNull Context context) {
        final long cutoff = PreferenceUtil.getInstance().getRecentlyPlayedCutoffTimeMillis();
        if (cutoff == 0) {return new ArrayList<>();}

        HistoryStore historyStore = HistoryStore.getInstance(context);
        ArrayList<Long> songIds = historyStore.getRecentIds(cutoff);
        return StoreLoader.getSongsFromIdsAndCleanupOrphans(songIds, historyStore::removeSongIds);
    }

    @NonNull
    public static ArrayList<Song> getNotRecentlyPlayedTracks(@NonNull Context context) {
        final long cutoff = PreferenceUtil.getInstance().getNotRecentlyPlayedCutoffTimeMillis();
        if (cutoff == 0) {return new ArrayList<>();}

        HistoryStore historyStore = HistoryStore.getInstance(context);
        ArrayList<Long> songIds = new ArrayList<>();

        // Collect not played songs
        ArrayList<Long> playedSongIds = historyStore.getRecentIds(0);
        ArrayList<Song> allSongs = Discography.getInstance().getAllSongs();
        Collections.sort(allSongs, SongSortOrder.BY_DATE_ADDED);

        for (Song song : allSongs) {
            if (!playedSongIds.contains(song.id)) {
                songIds.add(song.id);
            }
        }

        // Collect not recently played songs
        ArrayList<Long> notRecentSongIds = historyStore.getRecentIds(-1 * cutoff);
        songIds.addAll(notRecentSongIds);

        return StoreLoader.getSongsFromIdsAndCleanupOrphans(songIds, historyStore::removeSongIds);
    }

    @NonNull
    public static ArrayList<Song> getTopTracks(@NonNull Context context) {
        final boolean enabled = PreferenceUtil.getInstance().maintainTopTrackPlaylist();
        if (!enabled) {return new ArrayList<>();}

        final int NUMBER_OF_TOP_TRACKS = 100;
        try (Cursor cursor = SongPlayCountStore.getInstance(context).getTopPlayedResults(NUMBER_OF_TOP_TRACKS)){
            ArrayList<Long> songIds = StoreLoader.getIdsFromCursor(cursor, SongPlayCountStore.SongPlayCountColumns.ID);

            return StoreLoader.getSongsFromIdsAndCleanupOrphans(songIds, null);
        }
    }

}
