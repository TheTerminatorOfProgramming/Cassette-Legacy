package com.ttop.cassette.model.smartplaylist;

import android.content.Context;
import android.os.Parcel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ttop.cassette.R;
import com.ttop.cassette.loader.LastAddedLoader;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.util.MusicUtil;
import com.ttop.cassette.util.PreferenceUtil;

import java.util.ArrayList;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class LastAddedPlaylist extends AbsSmartPlaylist {

    public LastAddedPlaylist(@NonNull Context context) {
        super(context.getString(R.string.last_added), R.drawable.ic_library_add_white_24dp);
    }

    @NonNull
    @Override
    public String getInfoString(@NonNull Context context) {
        String cutoff = PreferenceUtil.getInstance().getLastAddedCutoffText(context);

        return MusicUtil.buildInfoString(
                cutoff,
                super.getInfoString(context)
        );
    }

    @Nullable
    @Override
    public String getPlaylistPreference() {
        return PreferenceUtil.LAST_ADDED_CUTOFF_V2;
    }

    @NonNull
    @Override
    public ArrayList<Song> getSongs(@NonNull Context context) {
        return LastAddedLoader.getLastAddedSongs();
    }

    @Override
    public boolean isClearable() {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    protected LastAddedPlaylist(Parcel in) {
        super(in);
    }

    public static final Creator<LastAddedPlaylist> CREATOR = new Creator<LastAddedPlaylist>() {
        public LastAddedPlaylist createFromParcel(Parcel source) {
            return new LastAddedPlaylist(source);
        }

        public LastAddedPlaylist[] newArray(int size) {
            return new LastAddedPlaylist[size];
        }
    };
}
