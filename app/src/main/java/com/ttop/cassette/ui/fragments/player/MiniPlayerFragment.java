package com.ttop.cassette.ui.fragments.player;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.util.ATHUtil;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.ttop.cassette.R;
import com.ttop.cassette.databinding.FragmentMiniPlayerBinding;
import com.ttop.cassette.databinding.FragmentMiniPlayerButtonsBinding;
import com.ttop.cassette.helper.MusicPlayerRemote;
import com.ttop.cassette.helper.MusicProgressViewUpdateHelper;
import com.ttop.cassette.helper.PlayPauseButtonOnClickHandler;
import com.ttop.cassette.ui.fragments.AbsMusicServiceFragment;
import com.ttop.cassette.util.PreferenceUtil;
import com.ttop.cassette.util.Util;
import com.ttop.cassette.views.PlayPauseDrawable;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

/**
 * @author Karim Abou Zeid (kabouzeid)
 */
public class MiniPlayerFragment extends AbsMusicServiceFragment implements MusicProgressViewUpdateHelper.Callback {
    TextView miniPlayerTitle;
    ImageView miniPlayerPlayPauseButton;
    ImageView miniPlayerNextButton;
    ImageView miniPlayerPrevButton;
    ImageView close;
    MaterialProgressBar progressBar;
    CircularProgressBar circle;

    private PlayPauseDrawable miniPlayerPlayPauseDrawable;

    private MusicProgressViewUpdateHelper progressViewUpdateHelper;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressViewUpdateHelper = new MusicProgressViewUpdateHelper(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        if (PreferenceUtil.getInstance().getExtraControls())
        {
            FragmentMiniPlayerButtonsBinding binding1 = FragmentMiniPlayerButtonsBinding.inflate(inflater, container, false);
            miniPlayerTitle = binding1.miniPlayerTitle;
            miniPlayerPlayPauseButton = binding1.miniPlayerPlayPauseButton;
            miniPlayerNextButton = binding1.miniPlayerPrev;
            miniPlayerPrevButton = binding1.miniPlayerNext;
            close = binding1.close;
            progressBar = binding1.progressBar;
            circle = binding1.circularProgressBar;
            return binding1.getRoot();
        }else{
            if (Util.isTablet(getContext())){
                FragmentMiniPlayerButtonsBinding binding1 = FragmentMiniPlayerButtonsBinding.inflate(inflater, container, false);
                miniPlayerTitle = binding1.miniPlayerTitle;
                miniPlayerPlayPauseButton = binding1.miniPlayerPlayPauseButton;
                miniPlayerNextButton = binding1.miniPlayerPrev;
                miniPlayerPrevButton = binding1.miniPlayerNext;
                close = binding1.close;
                progressBar = binding1.progressBar;
                circle = binding1.circularProgressBar;
                return binding1.getRoot();
            }else{
                FragmentMiniPlayerBinding binding = FragmentMiniPlayerBinding.inflate(inflater, container, false);
                miniPlayerTitle = binding.miniPlayerTitle;
                miniPlayerPlayPauseButton = binding.miniPlayerPlayPauseButton;
                progressBar = binding.progressBar;
                close = binding.close;
                circle = binding.circularProgressBar;
                return binding.getRoot();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (PreferenceUtil.getInstance().getCircleProgress()){
            progressBar.setVisibility(View.GONE);
        }else{
            circle.setVisibility(View.GONE);
        }

        view.setOnTouchListener(new FlingPlayBackController(getActivity()));
        setUpMiniPlayer();

        miniPlayerTitle.setSelected(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void setUpMiniPlayer() {
        setUpPlayPauseButton();
        setUpCloseButton();
        progressBar.setSupportProgressTintList(ColorStateList.valueOf(ThemeStore.accentColor(getActivity())));
        circle.setProgressBarColor(ThemeStore.accentColor(getContext()));
        if (Util.isTablet(getContext())) {
            setUpPrevNextButton();
        }else {
            if (PreferenceUtil.getInstance().getExtraControls()) {
                setUpPrevNextButton();
            }
        }
    }

    private void setUpPlayPauseButton() {
        miniPlayerPlayPauseDrawable = new PlayPauseDrawable(getActivity());
        miniPlayerPlayPauseButton.setImageDrawable(miniPlayerPlayPauseDrawable);
        miniPlayerPlayPauseButton.setColorFilter(ATHUtil.resolveColor(getActivity(), R.attr.iconColor, ThemeStore.textColorSecondary(getActivity())), PorterDuff.Mode.SRC_IN);
        miniPlayerPlayPauseButton.setOnClickListener(new PlayPauseButtonOnClickHandler());
    }

    private void setUpPrevNextButton() {
        //prev
        miniPlayerPrevButton.setImageResource(R.drawable.ic_skip_previous_white_24dp);
        //miniPlayerPrevButton.setColorFilter(ATHUtil.resolveColor(getActivity(), R.attr.iconColor, ThemeStore.textColorSecondary(getActivity())), PorterDuff.Mode.SRC_IN);
        miniPlayerPrevButton.setOnClickListener(view -> {
            if (MusicPlayerRemote.getSongProgressMillis() > 5000) {
                MusicPlayerRemote.seekTo(0);
            } else {
                if (MusicPlayerRemote.getPosition() > 0){
                    if (MusicPlayerRemote.isPlaying()) {
                        MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() - 1, true);
                    }
                    else{
                        MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() - 1, false);
                    }
                }else{
                    MusicPlayerRemote.seekTo(0);
                }
            }
        });

        //next
        miniPlayerNextButton.setImageResource(R.drawable.ic_skip_next_white_24dp);
        //miniPlayerNextButton.setColorFilter(ATHUtil.resolveColor(getActivity(), R.attr.iconColor, ThemeStore.textColorSecondary(getActivity())), PorterDuff.Mode.SRC_IN);
        miniPlayerNextButton.setOnClickListener(view -> {
            if (MusicPlayerRemote.isPlaying()){
                MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() + 1, true);
            }else{
                MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() + 1, false);
            }
        });
    }

    public void setUpCloseButton(){
        close.setOnClickListener(v -> {
            MusicPlayerRemote.clearQueue();
        });
    }

    private void updateSongTitle() {
        miniPlayerTitle.setText(MusicPlayerRemote.getCurrentSong().title);
    }

    @Override
    public void onServiceConnected() {
        updateSongTitle();
        updatePlayPauseDrawableState(false);
    }

    @Override
    public void onPlayingMetaChanged() {
        updateSongTitle();
    }

    @Override
    public void onPlayStateChanged() {
        updatePlayPauseDrawableState(true);
    }

    @Override
    public void onUpdateProgressViews(int progress, int total) {
        progressBar.setMax(total);
        progressBar.setProgress(progress);

        circle.setProgressMax(total);
        circle.setProgress(progress);
    }

    @Override
    public void onResume() {
        super.onResume();
        progressViewUpdateHelper.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        progressViewUpdateHelper.stop();
    }

    private static class FlingPlayBackController implements View.OnTouchListener {

        GestureDetector flingPlayBackController;

        public FlingPlayBackController(Context context) {
            flingPlayBackController = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    if (Math.abs(velocityX) > Math.abs(velocityY)) {
                        if (velocityX < 0) {
                            if (MusicPlayerRemote.isPlaying()){
                                MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() + 1, true);
                            }else{
                                MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() + 1, false);
                            }
                            return true;
                        } else if (velocityX > 0) {
                            if (MusicPlayerRemote.getSongProgressMillis() > 5000) {
                                MusicPlayerRemote.seekTo(0);
                            } else {
                                if (MusicPlayerRemote.getPosition() > 0){
                                    if (MusicPlayerRemote.isPlaying()) {
                                        MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() - 1, true);
                                    }
                                    else{
                                        MusicPlayerRemote.playSongAt(MusicPlayerRemote.getPosition() - 1, false);
                                    }
                                }else{
                                    MusicPlayerRemote.seekTo(0);
                                }
                            }
                            return true;
                        }
                    }
                    return false;
                }
            });
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            return flingPlayBackController.onTouchEvent(event);
        }
    }

    protected void updatePlayPauseDrawableState(boolean animate) {
        if (MusicPlayerRemote.isPlaying()) {
            miniPlayerPlayPauseDrawable.setPause(animate);
        } else {
            miniPlayerPlayPauseDrawable.setPlay(animate);
        }
    }
}