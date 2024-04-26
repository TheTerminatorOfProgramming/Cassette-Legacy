package com.ttop.cassette.appwidgets.base;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.kabouzeid.appthemehelper.util.MaterialValueHelper;
import com.ttop.cassette.R;
import com.ttop.cassette.discog.Discography;
import com.ttop.cassette.discog.tagging.MultiValuesTagUtil;
import com.ttop.cassette.glide.CassetteGlideExtension;
import com.ttop.cassette.glide.GlideApp;
import com.ttop.cassette.glide.CassetteSimpleTarget;
import com.ttop.cassette.glide.palette.BitmapPaletteWrapper;
import com.ttop.cassette.helper.MusicPlayerRemote;
import com.ttop.cassette.model.Song;
import com.ttop.cassette.service.MusicService;
import com.ttop.cassette.ui.activities.MainActivity;
import com.ttop.cassette.util.ImageUtil;
import com.ttop.cassette.util.MusicUtil;
import com.ttop.cassette.util.PreferenceUtil;

import java.util.ArrayList;

public abstract class BaseAppWidget extends AppWidgetProvider {
    public static final String NAME = "app_widget";

    protected Target target; // for cancellation
    protected RemoteViews appWidgetView;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(final Context context, final AppWidgetManager appWidgetManager,
                         final int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        final Intent updateIntent = new Intent(MusicService.APP_WIDGET_UPDATE);
        updateIntent.putExtra(MusicService.EXTRA_APP_WIDGET_NAME, NAME);
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        updateIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(updateIntent);
    }

    /**
     * Initialize given widgets to default state, where we launch Music on
     * default click and hide actions if service not running.
     */
    protected void defaultAppWidget(final Context context, final int[] appWidgetIds) {
        appWidgetView = new RemoteViews(context.getPackageName(), getLayout());

        if (PreferenceUtil.getInstance().getWidgetStyle()) {
            switch (PreferenceUtil.getThemeColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme())) {
                case R.color.md_black_1000:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_dark);

                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, Color.WHITE)));
                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, Color.WHITE)));
                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, Color.WHITE)));
                    break;
                case R.color.md_white_1000:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_light);

                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, Color.BLACK)));
                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, Color.BLACK)));
                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, Color.BLACK)));
                    break;
                case R.color.light_widget:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_light_trn);

                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, Color.BLACK)));
                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, Color.BLACK)));
                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, Color.BLACK)));
                    break;
                case R.color.dark_widget:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_dark_trn);

                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, Color.WHITE)));
                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, Color.WHITE)));
                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, Color.WHITE)));
                    break;
            }
        } else {
            appWidgetView.setInt(getId(), "setBackgroundResource", PreferenceUtil.getThemeColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme()));

            switch (PreferenceUtil.getThemeColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme())) {
                case R.color.md_white_1000:
                case R.color.light_widget:
                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, Color.BLACK)));
                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, Color.BLACK)));
                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, Color.BLACK)));

                    appWidgetView.setTextColor(R.id.title, Color.BLACK);
                    appWidgetView.setTextColor(R.id.text, Color.BLACK);
                    break;
                case R.color.md_black_1000:
                case R.color.dark_widget:
                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_next_white_24dp, Color.WHITE)));
                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_skip_previous_white_24dp, Color.WHITE)));
                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(context, R.drawable.ic_play_arrow_white_24dp, Color.WHITE)));

                    appWidgetView.setTextColor(R.id.title, Color.WHITE);
                    appWidgetView.setTextColor(R.id.text, Color.WHITE);
                    break;
            }
        }

        appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        appWidgetView.setImageViewResource(R.id.image, R.drawable.default_album_art);

        linkButtons(context);

        if (!MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            if (MusicPlayerRemote.isPlaying()) {
                MusicPlayerRemote.pauseSong();
                MusicPlayerRemote.resumePlaying();
            } else {
                MusicPlayerRemote.resumePlaying();
                MusicPlayerRemote.pauseSong();
            }
        }

        pushUpdate(context, appWidgetIds);
    }

    /**
     * Link up various button actions using {@link PendingIntent}.
     */
    protected void linkButtons(final Context context) {
        Intent action;
        PendingIntent pendingIntent;

        final ComponentName serviceName = new ComponentName(context, MusicService.class);

        // Home
        action = new Intent(context, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        pendingIntent = PendingIntent.getActivity(context, 0, action, 0);
        appWidgetView.setOnClickPendingIntent(R.id.image, pendingIntent);

        // Previous track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_REWIND, serviceName);
        appWidgetView.setOnClickPendingIntent(R.id.button_prev, pendingIntent);

        // Play and pause
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_TOGGLE_PAUSE, serviceName);
        appWidgetView.setOnClickPendingIntent(R.id.button_toggle_play_pause, pendingIntent);

        // Next track
        pendingIntent = buildPendingIntent(context, MusicService.ACTION_SKIP, serviceName);
        appWidgetView.setOnClickPendingIntent(R.id.button_next, pendingIntent);
    }

    /**
     * Handle a change notification coming over from
     * {@link MusicService}
     */
    public void notifyChange(final MusicService service, final String what) {
        if (hasInstances(service)) {
            if (MusicService.META_CHANGED.equals(what) || MusicService.PLAY_STATE_CHANGED.equals(what)) {
                performUpdate(service, null);
            }
        }
    }

    public void notifyThemeChange(final MusicService service) {
        performUpdate(service, null);
    }

    public void pushUpdate(final Context context, final int[] appWidgetIds) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            appWidgetManager.updateAppWidget(appWidgetIds, appWidgetView);
        } else {
            appWidgetManager.updateAppWidget(new ComponentName(context, getClass()), appWidgetView);
        }

        if (PreferenceUtil.getInstance().getWidgetStyle()) {
            switch (PreferenceUtil.getThemeColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme())) {
                case R.color.md_black_1000:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_dark);

                    break;
                case R.color.md_white_1000:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_light);

                    break;
                case R.color.light_widget:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_light_trn);

                    break;
                case R.color.dark_widget:
                    appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_dark_trn);

                    break;
            }
        } else {
            appWidgetView.setInt(getId(), "setBackgroundResource", PreferenceUtil.getThemeColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme()));
        }
    }

    /**
     * Check against {@link AppWidgetManager} if there are any instances of this
     * widget.
     */
    protected boolean hasInstances(final Context context) {
        final AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        final int[] mAppWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context,
                getClass()));
        return mAppWidgetIds.length > 0;
    }

    protected PendingIntent buildPendingIntent(Context context, final String action, final ComponentName serviceName) {
        Intent intent = new Intent(action);
        intent.setComponent(serviceName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(context, 0, intent, 0);
        } else {
            return PendingIntent.getService(context, 0, intent, 0);
        }
    }

    protected static Bitmap createRoundedBitmap(Drawable drawable, int width, int height, float tl, float tr, float bl, float br) {
        if (drawable == null) return null;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(c);

        Bitmap rounded = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(rounded);
        Paint paint = new Paint();
        paint.setShader(new BitmapShader(bitmap, BitmapShader.TileMode.CLAMP, BitmapShader.TileMode.CLAMP));
        paint.setAntiAlias(true);
        canvas.drawPath(composeRoundedRectPath(new RectF(0, 0, width, height), tl, tr, bl, br), paint);

        return rounded;
    }

    protected static Path composeRoundedRectPath(RectF rect, float tl, float tr, float bl, float br) {
        Path path = new Path();
        tl = tl < 0 ? 0 : tl;
        tr = tr < 0 ? 0 : tr;
        bl = bl < 0 ? 0 : bl;
        br = br < 0 ? 0 : br;

        path.moveTo(rect.left + tl, rect.top);
        path.lineTo(rect.right - tr, rect.top);
        path.quadTo(rect.right, rect.top, rect.right, rect.top + tr);
        path.lineTo(rect.right, rect.bottom - br);
        path.quadTo(rect.right, rect.bottom, rect.right - br, rect.bottom);
        path.lineTo(rect.left + bl, rect.bottom);
        path.quadTo(rect.left, rect.bottom, rect.left, rect.bottom - bl);
        path.lineTo(rect.left, rect.top + tl);
        path.quadTo(rect.left, rect.top, rect.left + tl, rect.top);
        path.close();

        return path;
    }

    protected Drawable getAlbumArtDrawable(final Resources resources, final Bitmap bitmap) {
        Drawable image;
        if (bitmap == null) {
            image = resources.getDrawable(R.drawable.default_album_art);
        } else {
            image = new BitmapDrawable(resources, bitmap);
        }
        return image;
    }

    protected String getSongArtistAndAlbum(final Song song) {
        return MusicUtil.getSongInfoString(song);
    }

    protected void loadAlbumCover(final MusicService service, final int[] appWidgetIds, int radius) {
        final Context appContext = service.getApplicationContext();

        service.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (target != null) {
                    GlideApp.with(appContext).clear(target);
                }
                final Song song = service.getCurrentSong();
                final boolean isPlaying = service.isPlaying();

                final int imageSize = getImageSize(service);
                if (PreferenceUtil.getInstance().getWidgetStyle()) {
                    target = GlideApp.with(appContext)
                            .asBitmapPalette()
                            .load(CassetteGlideExtension.getSongModel(song))
                            .transition(CassetteGlideExtension.getDefaultTransition())
                            .songOptions(song)
                            .apply(RequestOptions.bitmapTransform(new RoundedCorners(radius)))
                            .into(new CassetteSimpleTarget<BitmapPaletteWrapper>(imageSize, imageSize) {
                                @Override
                                public void onResourceReady(@NonNull BitmapPaletteWrapper resource, Transition<? super BitmapPaletteWrapper> glideAnimation) {
                                    Palette palette = resource.getPalette();
                                    update(resource.getBitmap(), palette.getVibrantColor(palette.getMutedColor(MaterialValueHelper.getSecondaryTextColor(appContext, true))));
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    update(null, MaterialValueHelper.getSecondaryTextColor(appContext, true));
                                }

                                private void update(@Nullable Bitmap bitmap, int color) {
                                    final int imageSize = getImageSize(service);
                                    final float cardRadius = getCardRadius(service);

                                    // Set correct drawable for pause state
                                    int playPauseRes = isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;
                                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(appContext, playPauseRes, color)));

                                    // Set prev/next button drawables
                                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(appContext, R.drawable.ic_skip_next_white_24dp, color)));
                                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(appContext, R.drawable.ic_skip_previous_white_24dp, color)));

                                    final Drawable image = getAlbumArtDrawable(appContext.getResources(), bitmap);
                                    final Bitmap roundedBitmap = createRoundedBitmap(image, imageSize, imageSize, cardRadius, 0, cardRadius, 0);
                                    appWidgetView.setImageViewBitmap(R.id.image, roundedBitmap);

                                    switch (PreferenceUtil.getThemeColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme())) {
                                        case R.color.md_black_1000:
                                            appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_dark);

                                            break;
                                        case R.color.md_white_1000:
                                            appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_light);

                                            break;
                                        case R.color.light_widget:
                                            appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_light_trn);

                                            break;
                                        case R.color.dark_widget:
                                            appWidgetView.setInt(getId(), "setBackgroundResource", R.drawable.widget_background_dark_trn);

                                            break;
                                    }

                                    pushUpdate(appContext, appWidgetIds);
                                }
                            });
                } else {
                    target = GlideApp.with(appContext)
                            .asBitmapPalette()
                            .load(CassetteGlideExtension.getSongModel(song))
                            .transition(CassetteGlideExtension.getDefaultTransition())
                            .songOptions(song)
                            .into(new CassetteSimpleTarget<BitmapPaletteWrapper>(imageSize, imageSize) {
                                @Override
                                public void onResourceReady(@NonNull BitmapPaletteWrapper resource, Transition<? super BitmapPaletteWrapper> glideAnimation) {
                                    Palette palette = resource.getPalette();
                                    update(resource.getBitmap(), palette.getVibrantColor(palette.getMutedColor(MaterialValueHelper.getSecondaryTextColor(appContext, true))));
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    update(null, MaterialValueHelper.getSecondaryTextColor(appContext, true));
                                }

                                private void update(@Nullable Bitmap bitmap, int color) {
                                    final int imageSize = getImageSize(service);
                                    final float cardRadius = getCardRadius(service);

                                    // Set correct drawable for pause state
                                    int playPauseRes = isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;
                                    appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(appContext, playPauseRes, color)));

                                    // Set prev/next button drawables
                                    appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(appContext, R.drawable.ic_skip_next_white_24dp, color)));
                                    appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(appContext, R.drawable.ic_skip_previous_white_24dp, color)));

                                    final Drawable image = getAlbumArtDrawable(appContext.getResources(), bitmap);
                                    final Bitmap roundedBitmap = createRoundedBitmap(image, imageSize, imageSize, cardRadius, 0, cardRadius, 0);
                                    appWidgetView.setImageViewBitmap(R.id.image, roundedBitmap);

                                    appWidgetView.setInt(getId(), "setBackgroundResource", PreferenceUtil.getThemeColorFromPrefValue(PreferenceUtil.getInstance().getAppTheme()));

                                    pushUpdate(appContext, appWidgetIds);
                                }
                            });
                }

            }
        });
    }

    protected void setTitlesArtwork(final MusicService service) {
        final Song song = service.getCurrentSong();

        if (TextUtils.isEmpty(song.title) && TextUtils.isEmpty(MultiValuesTagUtil.infoString(song.artistNames))) {
            appWidgetView.setViewVisibility(R.id.media_titles, View.INVISIBLE);
        } else {
            appWidgetView.setViewVisibility(R.id.media_titles, View.VISIBLE);
            appWidgetView.setTextViewText(R.id.title, song.title);
            appWidgetView.setTextViewText(R.id.text, getSongArtistAndAlbum(song));
        }
    }

    protected void setButtons(final MusicService service) {
        final boolean isPlaying = service.isPlaying();
        // Set correct drawable for pause state
        int playPauseRes = isPlaying ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;
        appWidgetView.setImageViewBitmap(R.id.button_toggle_play_pause, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, playPauseRes, MaterialValueHelper.getSecondaryTextColor(service, false))));

        // Set prev/next button drawables
        appWidgetView.setImageViewBitmap(R.id.button_next, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_next_white_24dp, MaterialValueHelper.getSecondaryTextColor(service, false))));
        appWidgetView.setImageViewBitmap(R.id.button_prev, ImageUtil.createBitmap(ImageUtil.getTintedVectorDrawable(service, R.drawable.ic_skip_previous_white_24dp, MaterialValueHelper.getSecondaryTextColor(service, false))));
    }

    public abstract int getImageSize(final MusicService service);

    public abstract float getCardRadius(final MusicService service);

    public abstract void performUpdate(final MusicService service, final int[] appWidgetIds);

    public abstract int getLayout();

    public abstract int getId();

    public void onUpdate(Context context) {

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), getClass().getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        onUpdate(context, appWidgetManager, appWidgetIds);
    }
}