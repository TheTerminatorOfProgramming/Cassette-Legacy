package com.ttop.cassette.ui.activities;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialcab.MaterialCab;
import com.h6ah4i.android.widget.advrecyclerview.utils.WrapperAdapterUtils;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.ttop.cassette.R;
import com.ttop.cassette.adapter.song.SongAdapter;
import com.ttop.cassette.databinding.ActivityGenreDetailBinding;
import com.ttop.cassette.databinding.SlidingMusicPanelLayoutBinding;
import com.ttop.cassette.helper.MusicPlayerRemote;
import com.ttop.cassette.interfaces.CabHolder;
import com.ttop.cassette.interfaces.LoaderIds;
import com.ttop.cassette.loader.GenreLoader;
import com.ttop.cassette.misc.WrappedAsyncTaskLoader;
import com.ttop.cassette.model.Genre;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.ui.activities.base.AbsSlidingMusicPanelActivity;
import com.ttop.cassette.util.PreferenceUtil;
import com.ttop.cassette.util.ViewUtil;
import com.ttop.cassette.util.CassetteColorUtil;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import java.util.ArrayList;

public class GenreDetailActivity extends AbsSlidingMusicPanelActivity implements CabHolder, LoaderManager.LoaderCallbacks<ArrayList<Song>> {

    private static final int LOADER_ID = LoaderIds.GENRE_DETAIL_ACTIVITY;

    public static final String EXTRA_GENRE = "extra_genre";

    RecyclerView recyclerView;
    Toolbar toolbar;
    TextView empty;
    TextView titleTextView;

    private Genre genre;

    private MaterialCab cab;
    private SongAdapter adapter;

    private RecyclerView.Adapter wrappedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDrawUnderStatusbar();
        toggleScreenOn();
        setImmersiveFullscreen();
        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        genre = getIntent().getExtras().getParcelable(EXTRA_GENRE);

        setUpRecyclerView();

        setUpToolBar();

        LoaderManager.getInstance(this).initLoader(LOADER_ID, null, this);
    }

    @Override
    protected View createContentView() {
        SlidingMusicPanelLayoutBinding slidingPanelBinding = createSlidingMusicPanel();
        ActivityGenreDetailBinding binding = ActivityGenreDetailBinding.inflate(
                getLayoutInflater(),
                slidingPanelBinding.contentContainer,
                true);

        recyclerView = binding.recyclerView;
        toolbar = binding.toolbar;
        empty = binding.empty;
        titleTextView = binding.title;

        return slidingPanelBinding.getRoot();
    }

    private void setUpRecyclerView() {
        ViewUtil.setUpFastScrollRecyclerViewColor(this, ((FastScrollRecyclerView) recyclerView), ThemeStore.accentColor(this));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new SongAdapter(this, new ArrayList<>(), R.layout.item_list, false, this);
        recyclerView.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkIsEmpty();
            }
        });
    }

    private void setUpToolBar() {
        toolbar.setBackgroundColor(ThemeStore.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setTitle(null);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        titleTextView.setText(genre.getName());

        titleTextView.setTextColor(MaterialValueHelper.getPrimaryTextColor(this, ColorUtil.isColorLight(ThemeStore.primaryColor(this))));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_genre_detail, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final int id = item.getItemId();
        if (id == R.id.action_shuffle_genre) {
            MusicPlayerRemote.openAndShuffleQueue(adapter.getDataSet(), true);
            return true;
        } else if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public MaterialCab openCab(final int menu, final MaterialCab.Callback callback) {
        if (cab != null && cab.isActive()) cab.finish();
        adapter.setColor(ThemeStore.primaryColor(this));
        cab = new MaterialCab(this, R.id.cab_stub)
                .setMenu(menu)
                .setCloseDrawableRes(R.drawable.ic_close_white_24dp)
                .setBackgroundColor(CassetteColorUtil.shiftBackgroundColorForLightText(ThemeStore.primaryColor(this)))
                .setPopupMenuTheme(PreferenceUtil.getInstance().getGeneralTheme())
                .start(callback);
        return cab;
    }

    @Override
    public void onBackPressed() {
        if (cab != null && cab.isActive()) cab.finish();
        else {
            recyclerView.stopScroll();
            super.onBackPressed();
        }
    }

    private void checkIsEmpty() {
        empty.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE
        );
    }

    @Override
    protected void onDestroy() {
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView = null;
        }

        if (wrappedAdapter != null) {
            WrapperAdapterUtils.releaseAll(wrappedAdapter);
            wrappedAdapter = null;
        }
        adapter = null;

        super.onDestroy();
    }

    @Override
    @NonNull
    public Loader<ArrayList<Song>> onCreateLoader(int id, Bundle args) {
        return new GenreDetailActivity.AsyncGenreSongLoader(this, genre);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<ArrayList<Song>> loader, ArrayList<Song> data) {
        if (adapter != null)
            adapter.swapDataSet(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<ArrayList<Song>> loader) {
        if (adapter != null)
            adapter.swapDataSet(new ArrayList<>());
    }

    @Override
    protected void reload() {
        LoaderManager.getInstance(this).restartLoader(LOADER_ID, null, this);
    }

    private static class AsyncGenreSongLoader extends WrappedAsyncTaskLoader<ArrayList<Song>> {
        private final Genre genre;

        public AsyncGenreSongLoader(Context context, Genre genre) {
            super(context);
            this.genre = genre;
        }

        @Override
        public ArrayList<Song> loadInBackground() {
            return GenreLoader.getSongs(genre.id);
        }
    }
}
