package com.ttop.cassette.ui.fragments.mainactivity.library.pager;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.ttop.cassette.R;
import com.ttop.cassette.adapter.GenreAdapter;
import com.ttop.cassette.interfaces.LoaderIds;
import com.ttop.cassette.loader.GenreLoader;
import com.ttop.cassette.misc.WrappedAsyncTaskLoader;
import com.ttop.cassette.model.Genre;

import java.util.ArrayList;

public class GenresFragment extends AbsLibraryPagerRecyclerViewFragment<GenreAdapter, LinearLayoutManager> implements LoaderManager.LoaderCallbacks<ArrayList<Genre>> {

    private static final int LOADER_ID = LoaderIds.GENRES_FRAGMENT;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @NonNull
    @Override
    protected LinearLayoutManager createLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @NonNull
    @Override
    protected GenreAdapter createAdapter() {
        ArrayList<Genre> dataSet = getAdapter() == null ? new ArrayList<>() : getAdapter().getDataSet();
        return new GenreAdapter(getLibraryFragment().getMainActivity(), dataSet);
    }

    @Override
    protected int getEmptyMessage() {
        return R.string.no_genres;
    }

    @Override
    @NonNull
    public Loader<ArrayList<Genre>> onCreateLoader(int id, Bundle args) {
        return new GenresFragment.AsyncGenreLoader(getActivity());
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<Genre>> loader, ArrayList<Genre> data) {
        getAdapter().swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<Genre>> loader) {
        getAdapter().swapDataSet(new ArrayList<>());
    }

    private static class AsyncGenreLoader extends WrappedAsyncTaskLoader<ArrayList<Genre>> {
        public AsyncGenreLoader(Context context) {
            super(context);
        }

        @Override
        public ArrayList<Genre> loadInBackground() {
            return GenreLoader.getAllGenres();
        }
    }

    @Override
    public void reload() {
        getLoaderManager().restartLoader(LOADER_ID, null, this);
    }
}
