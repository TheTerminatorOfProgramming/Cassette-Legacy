package com.ttop.cassette.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.ttop.cassette.R;
import com.ttop.cassette.discog.Discography;
import com.ttop.cassette.discog.tagging.MultiValuesTagUtil;
import com.ttop.cassette.helper.MusicPlayerRemote;
import com.ttop.cassette.loader.PlaylistLoader;
import com.ttop.cassette.model.Album;
import com.ttop.cassette.model.Artist;
import com.ttop.cassette.model.Genre;
import com.ttop.cassette.model.Playlist;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.model.lyrics.AbsSynchronizedLyrics;
import com.ttop.cassette.service.MusicService;

import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MusicUtil {
    public static Uri getMediaStoreAlbumCoverUri(long albumId) {
        final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

        return ContentUris.withAppendedId(sArtworkUri, albumId);
    }

    public static Uri getSongFileUri(long songId) {
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId);
    }

    @NonNull
    public static Intent createShareSongFileIntent(@NonNull final Song song, Context context) {
        try {
            return new Intent()
                    .setAction(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName(), new File(song.data)))
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("audio/*");
        } catch (IllegalArgumentException e) {
            // TODO the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
            e.printStackTrace();
            Toast.makeText(context, "Could not share this file, I'm aware of the issue.", Toast.LENGTH_SHORT).show();
            return new Intent();
        }
    }

    @NonNull
    public static String getArtistInfoString(@NonNull final Context context, @NonNull final Artist artist) {
        int albumCount = artist.getAlbumCount();
        int songCount = artist.getSongCount();

        return MusicUtil.buildInfoString(
            MusicUtil.getAlbumCountString(context, albumCount),
            MusicUtil.getSongCountString(context, songCount)
        );
    }

    @NonNull
    public static String getAlbumInfoString(@NonNull final Context context, @NonNull final Album album) {
        int songCount = album.getSongCount();

        return MusicUtil.buildInfoString(
            album.getArtistName(),
            MusicUtil.getSongCountString(context, songCount)
        );
    }

    @NonNull
    public static String getSongInfoString(@NonNull final Song song) {
        return MusicUtil.buildInfoString(
                PreferenceUtil.getInstance().showSongNumber() ? MusicUtil.getTrackNumberInfoString(song) : null,
                MultiValuesTagUtil.infoString(song.artistNames),
                song.albumName
        );
    }

    @NonNull
    public static String getGenreInfoString(@NonNull final Context context, @NonNull final Genre genre) {
        int songCount = genre.songCount;
        return MusicUtil.getSongCountString(context, songCount);
    }

    @NonNull
    public static String getPlaylistInfoString(@NonNull final Context context, @NonNull List<Song> songs) {
        final long duration = getTotalDuration(songs);

        return MusicUtil.buildInfoString(
            MusicUtil.getSongCountString(context, songs.size()),
            MusicUtil.getReadableDurationString(duration)
        );
    }

    @NonNull
    public static String getSongCountString(@NonNull final Context context, int songCount) {
        final String songString = songCount == 1 ? context.getResources().getString(R.string.song) : context.getResources().getString(R.string.songs);
        return songCount + " " + songString;
    }

    @NonNull
    public static String getAlbumCountString(@NonNull final Context context, int albumCount) {
        final String albumString = albumCount == 1 ? context.getResources().getString(R.string.album) : context.getResources().getString(R.string.albums);
        return albumCount + " " + albumString;
    }

    @NonNull
    public static String getYearString(int year) {
        return year > 0 ? String.valueOf(year) : "-";
    }

    public static long getTotalDuration(@NonNull List<Song> songs) {
        long duration = 0;
        for (int i = 0; i < songs.size(); i++) {
            duration += songs.get(i).duration;
        }
        return duration;
    }

    public static String getReadableDurationString(long songDurationMillis) {
        long minutes = (songDurationMillis / 1000) / 60;
        long seconds = (songDurationMillis / 1000) % 60;
        if (minutes < 60) {
            return String.format(Locale.getDefault(), "%01d:%02d", minutes, seconds);
        } else {
            long hours = minutes / 60;
            minutes = minutes % 60;
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        }
    }

    /**
     * Build a concatenated string from the provided arguments
     * The intended purpose is to show extra annotations
     * to a music library item.
     * Ex: for a given album --> buildInfoString(album.artist, album.songCount)
     */
    @NonNull
    public static String buildInfoString(final String... values)
    {
        return MusicUtil.buildInfoString("  •  ", values);
    }

    @NonNull
    public static String buildInfoString(@NonNull final String separator, @NonNull final String[] values)
    {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (TextUtils.isEmpty(value)) continue;
            if (result.length() > 0) result.append(separator);
            result.append(value);
        }
        return result.toString();
    }

    @NonNull
    public static String getTrackNumberInfoString(@NonNull final Song song) {
        String result = "";
        if (song.discNumber > 0) {
            result = song.discNumber + "-";
        }
        if (song.trackNumber > 0) {
            result += String.valueOf(song.trackNumber);
        }
        else if (result.isEmpty()) {
            result = "-";
        }
        return result;
    }

    public static void insertAlbumArt(@NonNull Context context, long albumId, String path, @NonNull final String mimeType) {
        ContentResolver contentResolver = context.getContentResolver();

        Uri artworkUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(artworkUri, albumId), null, null);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.AlbumColumns.ALBUM_ID, albumId);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.DATA, path);

        contentResolver.insert(artworkUri, values);
    }

    public static void deleteAlbumArt(@NonNull Context context, long albumId) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri localUri = Uri.parse("content://media/external/audio/albumart");
        contentResolver.delete(ContentUris.withAppendedId(localUri, albumId), null, null);
    }

    @NonNull
    public static File createAlbumArtFile() {
        return new File(createAlbumArtDir(), String.valueOf(System.currentTimeMillis()));
    }

    @NonNull
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static File createAlbumArtDir() {
        File albumArtDir = new File(Environment.getExternalStorageDirectory(), "/albumthumbs/");
        if (!albumArtDir.exists()) {
            albumArtDir.mkdirs();
            try {
                new File(albumArtDir, ".nomedia").createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return albumArtDir;
    }

    public static void deleteTracks(@NonNull final Activity activity, @NonNull final List<Song> songs, @Nullable final List<Uri> safUris, @Nullable final Runnable callback) {
        final int songCount = songs.size();
        final Discography discography = Discography.getInstance();

        try {
            // Step 1: Remove selected tracks from the current playlist
            MusicPlayerRemote.removeFromQueue(songs);

            // Step 2: Remove selected tracks from the database
            final StringBuilder selection = new StringBuilder();
            selection.append(BaseColumns._ID + " IN (");
            for (int i = 0; i < songCount - 1; i++) {
                selection.append(songs.get(i).id);
                selection.append(",");
            }
            // The last element of a batch
            selection.append(songs.get(songCount - 1).id);
            selection.append(")");

            activity.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    selection.toString(), null);

            // Step 3: Remove files from card - Android Q takes care of this if the element is remove via MediaStore
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                for (int i = 0; i < songCount; i++) {
                    final Uri safUri = safUris == null || safUris.size() <= i ? null : safUris.get(i);
                    SAFUtil.delete(activity, songs.get(i).data, safUri);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        activity.getContentResolver().notifyChange(Uri.parse("content://media"), null);

        activity.runOnUiThread(() -> {
            Toast.makeText(activity, activity.getString(R.string.deleted_x_songs, songCount), Toast.LENGTH_SHORT).show();
            if (callback != null) {
                callback.run();
            }
        });
    }

    public static boolean isFavoritePlaylist(@NonNull final Context context, @NonNull final Playlist playlist) {
        return playlist.name != null && isFavoritePlaylist(context, playlist.name);
    }

    public static boolean isFavoritePlaylist(@NonNull final Context context, @NonNull final String playlistName) {
        return playlistName.equals(context.getString(R.string.favorites));
    }

    public static Playlist getFavoritesPlaylist(@NonNull final Context context) {
        return PlaylistLoader.getPlaylist(context, context.getString(R.string.favorites));
    }

    private static Playlist getOrCreateFavoritesPlaylist(@NonNull final Context context) {
        return PlaylistLoader.getPlaylist(context, PlaylistsUtil.createPlaylist(context, context.getString(R.string.favorites)));
    }

    public static Playlist getOrCreateSkippedPlaylist(@NonNull final Context context) {
        return PlaylistLoader.getPlaylist(context, PlaylistsUtil.createPlaylist(context, context.getString(R.string.skipped_songs)));
    }

    public static boolean isFavorite(@NonNull final Context context, @NonNull final Song song) {
        return PlaylistsUtil.doesPlaylistContain(context, getFavoritesPlaylist(context).id, song.id);
    }

    public static void toggleFavorite(@NonNull final Context context, @NonNull final Song song) {
        if (isFavorite(context, song)) {
            PlaylistsUtil.removeFromPlaylist(context, song, getFavoritesPlaylist(context).id);
        } else {
            PlaylistsUtil.addToPlaylist(context, song, getOrCreateFavoritesPlaylist(context).id, false);
        }

        context.sendBroadcast(new Intent(MusicService.FAVORITE_STATE_CHANGED));
    }

    public static boolean isArtistNameUnknown(@Nullable String artistName) {
        return isNameUnknown(artistName, Artist.UNKNOWN_ARTIST_DISPLAY_NAME);
    }

    public static boolean isAlbumNameUnknown(@Nullable String albumName) {
        return isNameUnknown(albumName, Album.UNKNOWN_ALBUM_DISPLAY_NAME);
    }

    public static boolean isGenreNameUnknown(@Nullable String genreName) {
        return isNameUnknown(genreName, Genre.UNKNOWN_GENRE_DISPLAY_NAME);
    }

    private static boolean isNameUnknown(@Nullable String name, @NonNull final String defaultDisplayName) {
        if ((name == null) || (name.length() == 0)) return true;
        if (name.equals(defaultDisplayName)) return true;
        name = name.trim().toLowerCase();
        return (name.equals("unknown") || name.equals("<unknown>"));
    }

    @NonNull
    public static String getSectionName(@Nullable String musicMediaTitle) {
        if ((musicMediaTitle == null) || (musicMediaTitle.length() == 0)) return "";
        musicMediaTitle = musicMediaTitle.trim().toLowerCase();
        if (musicMediaTitle.startsWith("the ")) {
            musicMediaTitle = musicMediaTitle.substring(4);
        } else if (musicMediaTitle.startsWith("a ")) {
            musicMediaTitle = musicMediaTitle.substring(2);
        }
        if (musicMediaTitle.isEmpty()) return "";
        return String.valueOf(musicMediaTitle.charAt(0)).toUpperCase();
    }

    public static int indexOfSongInList(List<Song> songs, long songId) {
        for (int i = 0; i < songs.size(); i++) {
            if (songs.get(i).id == songId) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public static String getLyrics(Song song) {
        String lyrics = null;

        File file = new File(song.data);

        try {
            lyrics = AudioFileIO.read(file).getTagOrCreateDefault().getFirst(FieldKey.LYRICS);
        } catch (@NonNull Exception | NoSuchMethodError | VerifyError e) {
            e.printStackTrace();
        }

        if (lyrics == null || lyrics.trim().isEmpty() || !AbsSynchronizedLyrics.isSynchronized(lyrics)) {
            File dir = file.getAbsoluteFile().getParentFile();

            if (dir != null && dir.exists() && dir.isDirectory()) {
                String format = ".*%s.*\\.(lrc|txt)";
                String filename = Pattern.quote(FileUtil.stripExtension(file.getName()));
                String songTitle = Pattern.quote(song.title);

                final ArrayList<Pattern> patterns = new ArrayList<>();
                patterns.add(Pattern.compile(String.format(format, filename), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
                patterns.add(Pattern.compile(String.format(format, songTitle), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));

                File[] files = dir.listFiles(f -> {
                    for (Pattern pattern : patterns) {
                        if (pattern.matcher(f.getName()).matches()) return true;
                    }
                    return false;
                });

                if (files != null && files.length > 0) {
                    for (File f : files) {
                        try {
                            String newLyrics = FileUtil.read(f);
                            if (newLyrics != null && !newLyrics.trim().isEmpty()) {
                                if (AbsSynchronizedLyrics.isSynchronized(newLyrics)) {
                                    return newLyrics;
                                }
                                lyrics = newLyrics;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return lyrics;
    }
}
