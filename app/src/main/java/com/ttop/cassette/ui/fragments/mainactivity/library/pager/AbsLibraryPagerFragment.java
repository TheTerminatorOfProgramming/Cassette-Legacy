package com.ttop.cassette.ui.fragments.mainactivity.library.pager;

import android.os.Bundle;

import com.ttop.cassette.ui.fragments.AbsMusicServiceFragment;
import com.ttop.cassette.ui.fragments.mainactivity.library.LibraryFragment;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class AbsLibraryPagerFragment extends AbsMusicServiceFragment {
    public LibraryFragment getLibraryFragment() {
        return (LibraryFragment) getParentFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
}
