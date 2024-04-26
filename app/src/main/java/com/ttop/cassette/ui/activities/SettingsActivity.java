package com.ttop.cassette.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.TwoStatePreference;

import com.afollestad.materialdialogs.color.ColorChooserDialog;
import com.kabouzeid.appthemehelper.ThemeStore;
import com.kabouzeid.appthemehelper.common.prefs.supportv7.ATEColorPreference;
import com.kabouzeid.appthemehelper.common.prefs.supportv7.ATEPreferenceFragmentCompat;
import com.kabouzeid.appthemehelper.util.ColorUtil;
import com.ttop.cassette.R;
import com.ttop.cassette.appshortcuts.DynamicShortcutManager;
import com.ttop.cassette.appwidgets.AppWidgetBig;
import com.ttop.cassette.appwidgets.AppWidgetClassic;
import com.ttop.cassette.appwidgets.AppWidgetFull;
import com.ttop.cassette.appwidgets.AppWidgetMini;
import com.ttop.cassette.databinding.ActivityPreferencesBinding;
import com.ttop.cassette.helper.MusicPlayerRemote;
import com.ttop.cassette.preferences.BlacklistPreference;
import com.ttop.cassette.preferences.BlacklistPreferenceDialog;
import com.ttop.cassette.preferences.LibraryPreference;
import com.ttop.cassette.preferences.LibraryPreferenceDialog;
import com.ttop.cassette.preferences.NowPlayingScreenPreference;
import com.ttop.cassette.preferences.NowPlayingScreenPreferenceDialog;
import com.ttop.cassette.preferences.SmartPlaylistPreference;
import com.ttop.cassette.preferences.SmartPlaylistPreferenceDialog;
import com.ttop.cassette.preferences.PreAmpPreference;
import com.ttop.cassette.preferences.PreAmpPreferenceDialog;
import com.ttop.cassette.service.MusicService;
import com.ttop.cassette.ui.activities.base.AbsBaseActivity;
import com.ttop.cassette.ui.activities.intro.AppIntroActivity;
import com.ttop.cassette.util.ImageTheme.ThemeStyleUtil;
import com.ttop.cassette.util.FileUtil;
import com.ttop.cassette.util.NavigationUtil;
import com.ttop.cassette.util.PreferenceUtil;
import com.ttop.cassette.util.Util;

import java.io.File;

public class SettingsActivity extends AbsBaseActivity implements ColorChooserDialog.ColorCallback {

    Toolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityPreferencesBinding binding = ActivityPreferencesBinding.inflate(LayoutInflater.from(this));
        toolbar = binding.toolbar;
        setContentView(binding.getRoot());

        setDrawUnderStatusbar();
        toggleScreenOn();
        setImmersiveFullscreen();
        setStatusbarColorAuto();
        setNavigationbarColorAuto();
        setTaskDescriptionColorAuto();

        toolbar.setBackgroundColor(ThemeStore.primaryColor(this));
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.content_frame, new SettingsFragment()).commit();
        } else {
            SettingsFragment frag = (SettingsFragment) getSupportFragmentManager().findFragmentById(R.id.content_frame);
            if (frag != null) frag.invalidateSettings();
        }
    }

    @Override
    public void onColorSelection(@NonNull ColorChooserDialog dialog, @ColorInt int selectedColor) {
        final int title = dialog.getTitle();
        int finalAccent = 0;
        int finalPrimary = 0;
        com.ttop.cassette.util.ColorUtil colorUtil = new com.ttop.cassette.util.ColorUtil();

        if (title == R.string.pref_title_primary_color) {

            PreferenceUtil.getInstance().setOriginalPrimaryColor(selectedColor);

            if (PreferenceUtil.getInstance().getDesaturate()){
                finalPrimary = colorUtil.desaturateColor(selectedColor, 0.4f);
            }else{
                finalPrimary = selectedColor;
            }

            ThemeStore.editTheme(this)
                    .primaryColor(finalPrimary)
                    .commit();
        } else if (title == R.string.pref_title_accent_color) {
            PreferenceUtil.getInstance().setOriginalAccentColor(selectedColor);

            if (PreferenceUtil.getInstance().getDesaturate()){
                finalAccent = colorUtil.desaturateColor(selectedColor, 0.4f);
            }else{
                finalAccent = selectedColor;
            }
            ThemeStore.editTheme(this)
                    .accentColor(finalAccent)
                    .commit();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            new DynamicShortcutManager(this).updateDynamicShortcuts();
        }
        recreate();
    }

    @Override
    public void onColorChooserDismissed(@NonNull ColorChooserDialog dialog) {
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends ATEPreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {
        MusicService musicService = MusicPlayerRemote.musicService;
        final AppWidgetClassic appWidgetClassic = AppWidgetClassic.getInstance();
        final AppWidgetBig appWidgetBig = AppWidgetBig.getInstance();
        final AppWidgetFull appWidgetFull = AppWidgetFull.getInstance();
        final AppWidgetMini appWidgetMini = AppWidgetMini.getInstance();

        private static void setSummary(@NonNull Preference preference) {
            setSummary(preference, PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), ""));
        }

        private static void setSummary(Preference preference, @NonNull Object value) {
            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } else {
                preference.setSummary(stringValue);
            }
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.pref_theme);
            addPreferencesFromResource(R.xml.pref_colors);
            addPreferencesFromResource(R.xml.pref_library);
            addPreferencesFromResource(R.xml.pref_now_playing_screen);
            addPreferencesFromResource(R.xml.pref_mini_player);
            addPreferencesFromResource(R.xml.pref_notification);
            addPreferencesFromResource(R.xml.pref_widget);
            addPreferencesFromResource(R.xml.pref_images);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                addPreferencesFromResource(R.xml.pref_lockscreen);
            }
            addPreferencesFromResource(R.xml.pref_audio);
            addPreferencesFromResource(R.xml.pref_playlists);
            addPreferencesFromResource(R.xml.pref_labs);
            addPreferencesFromResource(R.xml.pref_about);

            // set summary for whitelist, in order to indicate start directory
            final String strSummaryWhitelist = getString(R.string.pref_summary_whitelist);
            final File startDirectory = PreferenceUtil.getInstance().getStartDirectory();
            final String startPath = FileUtil.safeGetCanonicalPath(startDirectory);
            findPreference(PreferenceUtil.WHITELIST_ENABLED).setSummary(strSummaryWhitelist+startPath);

        }

        @Nullable
        @Override
        public DialogFragment onCreatePreferenceDialog(Preference preference) {
            if (preference instanceof NowPlayingScreenPreference) {
                return NowPlayingScreenPreferenceDialog.newInstance();
            } else if (preference instanceof BlacklistPreference) {
                return BlacklistPreferenceDialog.newInstance();
            } else if (preference instanceof LibraryPreference) {
                return LibraryPreferenceDialog.newInstance();
            } else if (preference instanceof PreAmpPreference) {
                return PreAmpPreferenceDialog.newInstance();
            } else if (preference instanceof SmartPlaylistPreference) {
                return SmartPlaylistPreferenceDialog.newInstance(preference.getKey());
            }
            return super.onCreatePreferenceDialog(preference);
        }

        @Override
        public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            getListView().setPadding(0, 0, 0, 0);
            invalidateSettings();
            PreferenceUtil.getInstance().registerOnSharedPreferenceChangedListener(this);
        }

        @Override
        public void onDestroyView() {
            super.onDestroyView();
            PreferenceUtil.getInstance().unregisterOnSharedPreferenceChangedListener(this);
        }

        private void invalidateSettings() {

            final TwoStatePreference keep_current_state = findPreference("keep_current_state");
            keep_current_state.setChecked(PreferenceUtil.getInstance().getCurrentState());
            keep_current_state.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setCurrentState((Boolean) newValue);
                return true;
            });

            final TwoStatePreference animate = findPreference("animate_playing_song_icon");
            animate.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setShouldRecreate(true);
                return true;
            });

            final TwoStatePreference auto_playback = findPreference("headset_playback");
            auto_playback.setChecked(PreferenceUtil.getInstance().getHeadsetPlayback());
            auto_playback.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setHeadsetPlayback((Boolean) newValue);
                return true;
            });

            final TwoStatePreference immersive_mode = findPreference("immersive_mode");
            immersive_mode.setChecked(PreferenceUtil.getInstance().getImmersiveMode());
            immersive_mode.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setImmersiveMode((Boolean) newValue);
                PreferenceUtil.getInstance().setShouldRecreate(true);

                setImmersiveFullscreen();
                return true;
            });

            final TwoStatePreference screen_on = findPreference("screen_on");
            screen_on.setChecked(PreferenceUtil.getInstance().getScreenOn());
            screen_on.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setScreenOn((Boolean) newValue);
                PreferenceUtil.getInstance().setShouldRecreate(true);

                toggleScreenOn();
                return true;
            });

            final TwoStatePreference circle_progress = findPreference("circle_progress");
            circle_progress.setChecked(PreferenceUtil.getInstance().getCircleProgress());
            circle_progress.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setCircleProgress((Boolean) newValue);
                PreferenceUtil.getInstance().setShouldRecreate(true);
                return true;
            });

            final TwoStatePreference expand_panel = findPreference("show_now_playing");
            expand_panel.setChecked(PreferenceUtil.getInstance().getExpandPanel());
            expand_panel.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setExpandPanel((Boolean) newValue);
                return true;
            });

            final TwoStatePreference extra_controls = findPreference("extra_controls");
            if (Util.isTablet(getContext())){
                extra_controls.setEnabled(false);
                PreferenceUtil.getInstance().setShouldRecreate(true);
            }else {
                extra_controls.setEnabled(true);
                extra_controls.setChecked(PreferenceUtil.getInstance().getExtraControls());
                extra_controls.setOnPreferenceChangeListener((preference, newValue) -> {
                    PreferenceUtil.getInstance().setExtraControls((Boolean) newValue);
                    PreferenceUtil.getInstance().setShouldRecreate(true);
                    return true;
                });
            }

            final TwoStatePreference desaurate = findPreference("desaturate_colors");
            desaurate.setChecked(PreferenceUtil.getInstance().getDesaturate());
            desaurate.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setDesaturate((Boolean) newValue);
                if ((Boolean) newValue) {
                    com.ttop.cassette.util.ColorUtil colorutil = new com.ttop.cassette.util.ColorUtil();

                    ThemeStore.editTheme(getActivity())
                            .accentColor(colorutil.desaturateColor(ThemeStore.accentColor(getActivity()), 0.4f))
                            .primaryColor(colorutil.desaturateColor(ThemeStore.primaryColor(getActivity()), 0.4f))
                            .commit();
                }else {

                    ThemeStore.editTheme(getActivity())
                            .accentColor(PreferenceUtil.getInstance().getOriginalAccentColor())
                            .primaryColor(PreferenceUtil.getInstance().getOriginalPrimaryColor())
                            .commit();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                        new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();
                }
                getActivity().recreate();
                return true;
            });

            final TwoStatePreference empty_queue = findPreference("empty_queue_action");
            empty_queue.setChecked(PreferenceUtil.getInstance().getEmptyQueueAction());
            empty_queue.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setEmptyQueueAction((Boolean) newValue);
                return true;
            });

            final TwoStatePreference pause_zero = findPreference("pause_on_zero");
            pause_zero.setChecked(PreferenceUtil.getInstance().getPauseZero());
            pause_zero.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setPauseZero((Boolean) newValue);
                return true;
            });

            final TwoStatePreference widgetStyle = findPreference("widget_style");
            widgetStyle.setChecked(PreferenceUtil.getInstance().getWidgetStyle());
            if (PreferenceUtil.getInstance().getWidgetStyle()){
                widgetStyle.setSummary("Rounded");
            }else{
                widgetStyle.setSummary("Classic");
            }
            widgetStyle.setOnPreferenceChangeListener((preference, o) -> {

                PreferenceUtil.getInstance().setWidgetStyle((Boolean) o);

                if (PreferenceUtil.getInstance().getWidgetStyle()){
                    widgetStyle.setSummary("Rounded");
                }else{
                    widgetStyle.setSummary("Classic");
                }

                appWidgetClassic.notifyThemeChange(musicService);
                appWidgetBig.notifyThemeChange(musicService);
                appWidgetFull.notifyThemeChange(musicService);
                appWidgetMini.notifyThemeChange(musicService);
                return true;
            });

            final TwoStatePreference widget_override = findPreference("widget_override");
            widget_override.setChecked(PreferenceUtil.getInstance().getWidgetOverride());
            widget_override.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setWidgetOverride((Boolean) newValue);

                appWidgetClassic.notifyThemeChange(musicService);
                appWidgetBig.notifyThemeChange(musicService);
                appWidgetFull.notifyThemeChange(musicService);
                appWidgetMini.notifyThemeChange(musicService);

                return true;
            });

            final Preference intro = findPreference("intro");
            intro.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getContext(), AppIntroActivity.class));
                return false;
            });

            final Preference app_info = findPreference("app_info");
            app_info.setOnPreferenceClickListener(preference -> {
                Intent i = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.addCategory(Intent.CATEGORY_DEFAULT);
                i.setData(Uri.parse("package:" + getContext().getPackageName()));
                startActivity(i);
                return false;
            });

            final Preference about = findPreference("about");
            about.setOnPreferenceClickListener(preference -> {
                new Handler().postDelayed(() -> startActivity(new Intent(getActivity(), AboutActivity.class)), 200);
                return false;
            });

            final TwoStatePreference next_prev = findPreference("extra_player_controls");
            next_prev.setChecked(PreferenceUtil.getInstance().getExtraPlayerControls());
            next_prev.setOnPreferenceChangeListener((preference, newValue) -> {
                PreferenceUtil.getInstance().setExtraPlayerControls((Boolean) newValue);
                PreferenceUtil.getInstance().setShouldRecreate(true);
                return true;
            });

            final Preference generalTheme = findPreference(PreferenceUtil.GENERAL_THEME);
            setSummary(generalTheme);
            generalTheme.setOnPreferenceChangeListener((preference, o) -> {
                String themeName = (String) o;

                setSummary(generalTheme, o);

                PreferenceUtil.getInstance().setAppTheme(themeName);

                appWidgetClassic.notifyThemeChange(musicService);
                appWidgetBig.notifyThemeChange(musicService);
                appWidgetFull.notifyThemeChange(musicService);
                appWidgetMini.notifyThemeChange(musicService);

                if (getActivity() != null) {
                    ThemeStore.markChanged(getActivity());
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    // Set the new theme so that updateAppShortcuts can pull it
                    getActivity().setTheme(PreferenceUtil.getThemeResFromPrefValue(themeName));

                    new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();
                }

                getActivity().recreate();
                return true;
            });

            final Preference themeStyle = findPreference("theme_style");
            themeStyle.setOnPreferenceChangeListener((preference, o) -> {
                ThemeStyleUtil.updateInstance(PreferenceUtil.getThemeStyleFromPrefValue((String) o));
                if (getActivity() != null) {
                    ThemeStore.markChanged(getActivity());
                }

                return true;
            });

            final Preference autoDownloadImagesPolicy = findPreference(PreferenceUtil.AUTO_DOWNLOAD_IMAGES_POLICY);
            if (autoDownloadImagesPolicy != null) {
                setSummary(autoDownloadImagesPolicy);
                autoDownloadImagesPolicy.setOnPreferenceChangeListener((preference, o) -> {
                    setSummary(autoDownloadImagesPolicy, o);
                    return true;
                });
            }

            final ATEColorPreference primaryColorPref = findPreference("primary_color");
            if (getActivity() != null && primaryColorPref != null) {
                final int primaryColor = ThemeStore.primaryColor(getActivity());
                primaryColorPref.setColor(primaryColor, ColorUtil.darkenColor(primaryColor));
                primaryColorPref.setOnPreferenceClickListener(preference -> {
                    new ColorChooserDialog.Builder(getActivity(), R.string.pref_title_primary_color)
                            .accentMode(false)
                            .allowUserColorInput(true)
                            .allowUserColorInputAlpha(false)
                            .preselect(primaryColor)
                            .show(getActivity());
                    return true;
                });
            }

            final ATEColorPreference accentColorPref = findPreference("accent_color");
            if (getActivity() != null && accentColorPref != null) {
                final int accentColor = ThemeStore.accentColor(getActivity());
                accentColorPref.setColor(accentColor, ColorUtil.darkenColor(accentColor));
                accentColorPref.setOnPreferenceClickListener(preference -> {
                    new ColorChooserDialog.Builder(getActivity(), R.string.pref_title_accent_color)
                            .accentMode(true)
                            .allowUserColorInput(true)
                            .allowUserColorInputAlpha(false)
                            .preselect(accentColor)
                            .show(getActivity());
                    return true;
                });
            }
            TwoStatePreference colorNavBar = findPreference("should_color_navigation_bar");
            if (colorNavBar != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    colorNavBar.setVisible(false);
                } else {
                    colorNavBar.setChecked(ThemeStore.coloredNavigationBar(getActivity()));
                    colorNavBar.setOnPreferenceChangeListener((preference, newValue) -> {
                        ThemeStore.editTheme(getActivity())
                                .coloredNavigationBar((Boolean) newValue)
                                .commit();
                        getActivity().recreate();
                        return true;
                    });
                }
            }

            final TwoStatePreference classicNotification = findPreference(PreferenceUtil.CLASSIC_NOTIFICATION);
            if (classicNotification != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    classicNotification.setVisible(false);
                } else {
                    classicNotification.setChecked(PreferenceUtil.getInstance().classicNotification());
                    classicNotification.setOnPreferenceChangeListener((preference, newValue) -> {
                        // Save preference
                        PreferenceUtil.getInstance().setClassicNotification((Boolean) newValue);
                        return true;
                    });
                }
            }

            final TwoStatePreference coloredNotification = findPreference(PreferenceUtil.COLORED_NOTIFICATION);
            if (coloredNotification != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    coloredNotification.setEnabled(PreferenceUtil.getInstance().classicNotification());
                } else {
                    coloredNotification.setChecked(PreferenceUtil.getInstance().coloredNotification());
                    coloredNotification.setOnPreferenceChangeListener((preference, newValue) -> {
                        // Save preference
                        PreferenceUtil.getInstance().setColoredNotification((Boolean) newValue);
                        return true;
                    });
                }
            }

            final TwoStatePreference colorAppShortcuts = findPreference("should_color_app_shortcuts");
            if (colorAppShortcuts != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) {
                    colorAppShortcuts.setVisible(false);
                } else {
                    colorAppShortcuts.setChecked(PreferenceUtil.getInstance().coloredAppShortcuts());
                    colorAppShortcuts.setOnPreferenceChangeListener((preference, newValue) -> {
                        // Save preference
                        PreferenceUtil.getInstance().setColoredAppShortcuts((Boolean) newValue);

                        // Update app shortcuts
                        new DynamicShortcutManager(getActivity()).updateDynamicShortcuts();

                        return true;
                    });
                }
            }

            final Preference equalizer = findPreference("equalizer");
            if (equalizer != null) {
                if (!hasEqualizer()) {
                    equalizer.setEnabled(false);
                    equalizer.setSummary(getResources().getString(R.string.no_equalizer));
                }
                equalizer.setOnPreferenceClickListener(preference -> {
                    NavigationUtil.openEqualizer(getActivity());
                    return true;
                });
            }

            if (PreferenceUtil.getInstance().getReplayGainSourceMode() == PreferenceUtil.RG_SOURCE_MODE_NONE) {
                Preference pref = findPreference("replaygain_preamp");
                if (pref != null) {
                    pref.setEnabled(false);
                    pref.setSummary(getResources().getString(R.string.pref_rg_disabled));
                }
            }

            updateNowPlayingScreenSummary();
            updatePlaylistsSummary();
        }

        private boolean hasEqualizer() {
            final Intent effects = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            if (getActivity() != null) {
                PackageManager pm = getActivity().getPackageManager();
                ResolveInfo ri = pm.resolveActivity(effects, 0);
                return ri != null;
            }
            return false;
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            switch (key) {
                case PreferenceUtil.NOW_PLAYING_SCREEN_ID:
                    updateNowPlayingScreenSummary();
                    break;
                case PreferenceUtil.CLASSIC_NOTIFICATION:
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        findPreference("colored_notification").setEnabled(sharedPreferences.getBoolean(key, false));
                    }
                    break;
                case PreferenceUtil.RG_SOURCE_MODE_V2:
                    Preference pref = findPreference("replaygain_preamp");
                    if (pref != null) {
                        if (!sharedPreferences.getString(key, "none").equals("none")) {
                            pref.setEnabled(true);
                            pref.setSummary(R.string.pref_summary_rg_preamp);
                        } else {
                            pref.setEnabled(false);
                            pref.setSummary(getResources().getString(R.string.pref_rg_disabled));
                        }
                    }
                    break;
                case PreferenceUtil.WHITELIST_ENABLED:
                    getContext().sendBroadcast(new Intent(MusicService.MEDIA_STORE_CHANGED));
                    break;
                case PreferenceUtil.RECENTLY_PLAYED_CUTOFF_V2:
                case PreferenceUtil.NOT_RECENTLY_PLAYED_CUTOFF_V2:
                case PreferenceUtil.LAST_ADDED_CUTOFF_V2:
                    updatePlaylistsSummary();
                    break;
            }
        }

        private void updateNowPlayingScreenSummary() {
            findPreference(PreferenceUtil.NOW_PLAYING_SCREEN_ID).setSummary(PreferenceUtil.getInstance().getNowPlayingScreen().titleRes);
        }

        private void updatePlaylistsSummary() {
            final Context context = getContext();
            final PreferenceUtil preferenceUtil = PreferenceUtil.getInstance();

            findPreference(PreferenceUtil.RECENTLY_PLAYED_CUTOFF_V2)
                    .setSummary(preferenceUtil.getRecentlyPlayedCutoffText(context));
            findPreference(PreferenceUtil.NOT_RECENTLY_PLAYED_CUTOFF_V2)
                    .setSummary(preferenceUtil.getNotRecentlyPlayedCutoffText(context));
            findPreference(PreferenceUtil.LAST_ADDED_CUTOFF_V2)
                    .setSummary(preferenceUtil.getLastAddedCutoffText(context));
        }

        public void setImmersiveFullscreen() {
            int flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);


            if (PreferenceUtil.getInstance().getImmersiveMode()) {
                getActivity().getWindow().getDecorView().setSystemUiVisibility(flags);
            }else {
                getActivity().getWindow().getDecorView().setSystemUiVisibility(0);
                getActivity().getWindow().setStatusBarColor(ThemeStore.primaryColor(getActivity()));
            }
        }

        public void toggleScreenOn() {
            if (PreferenceUtil.getInstance().getScreenOn()) {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }
}
