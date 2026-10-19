package com.termux.window;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * The color scheme used for the window chrome. On Android 12+ the Material You system
 * palette is used so the chrome follows the user's wallpaper theme; on older versions
 * the bundled Material Design 3 baseline dark scheme is used.
 */
final class WindowColorScheme {

    /** State layer alpha used by the MD3 ripples: on-color at 12% opacity. */
    private static final int STATE_LAYER_ALPHA = 0x1F;

    public final int surface;
    public final int surfaceContainer;
    public final int surfaceContainerHigh;
    public final int onSurface;
    public final int onSurfaceVariant;
    public final int outlineVariant;
    public final int primary;
    public final int onPrimary;

    private WindowColorScheme(int surface, int surfaceContainer, int surfaceContainerHigh,
            int onSurface, int onSurfaceVariant, int outlineVariant, int primary, int onPrimary) {
        this.surface = surface;
        this.surfaceContainer = surfaceContainer;
        this.surfaceContainerHigh = surfaceContainerHigh;
        this.onSurface = onSurface;
        this.onSurfaceVariant = onSurfaceVariant;
        this.outlineVariant = outlineVariant;
        this.primary = primary;
        this.onPrimary = onPrimary;
    }

    @NonNull
    public static WindowColorScheme load(@NonNull Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            return dynamic(context);
        return baseline(context);
    }

    /** The bundled MD3 baseline dark scheme. */
    @NonNull
    private static WindowColorScheme baseline(@NonNull Context context) {
        return new WindowColorScheme(
                context.getColor(R.color.md_theme_dark_surface),
                context.getColor(R.color.md_theme_dark_surface_container),
                context.getColor(R.color.md_theme_dark_surface_container_high),
                context.getColor(R.color.md_theme_dark_on_surface),
                context.getColor(R.color.md_theme_dark_on_surface_variant),
                context.getColor(R.color.md_theme_dark_outline_variant),
                context.getColor(R.color.md_theme_dark_primary),
                context.getColor(R.color.md_theme_dark_on_primary));
    }

    /**
     * The Material You palette exposed by the framework on Android 12+.
     * https://developer.android.com/develop/ui/views/theming/dynamic-colors
     */
    @SuppressLint("InlinedApi")
    private static WindowColorScheme dynamic(@NonNull Context context) {
        return new WindowColorScheme(
                context.getColor(android.R.color.system_neutral1_900),
                context.getColor(android.R.color.system_neutral1_800),
                context.getColor(android.R.color.system_neutral1_700),
                context.getColor(android.R.color.system_neutral1_100),
                context.getColor(android.R.color.system_neutral2_200),
                context.getColor(android.R.color.system_neutral2_700),
                context.getColor(android.R.color.system_accent1_200),
                context.getColor(android.R.color.system_accent1_800));
    }

    /** State layer color drawn over surfaces: {@link #onSurface} at state layer alpha. */
    public int stateLayer() {
        return (onSurface & 0x00FFFFFF) | (STATE_LAYER_ALPHA << 24);
    }

    /** State layer color drawn over the primary filled button. */
    public int onPrimaryStateLayer() {
        return (onPrimary & 0x00FFFFFF) | (STATE_LAYER_ALPHA << 24);
    }

    /**
     * Set the fill (and optionally the stroke) of every {@link GradientDrawable} reachable
     * through the drawable's layer/state containers.
     */
    public static void tintShape(@Nullable Drawable drawable, int fillColor, int strokeWidthPx, int strokeColor) {
        if (drawable == null) return;
        drawable = drawable.mutate();
        if (drawable instanceof GradientDrawable) {
            GradientDrawable shape = (GradientDrawable) drawable;
            shape.setColor(fillColor);
            if (strokeWidthPx > 0) shape.setStroke(strokeWidthPx, strokeColor);
        } else if (drawable instanceof LayerDrawable) {
            LayerDrawable layers = (LayerDrawable) drawable;
            for (int i = 0; i < layers.getNumberOfLayers(); i++)
                tintShape(layers.getDrawable(i), fillColor, strokeWidthPx, strokeColor);
        }
    }
}
