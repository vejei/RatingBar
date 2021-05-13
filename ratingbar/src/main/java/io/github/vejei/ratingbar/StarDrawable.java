package io.github.vejei.ratingbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.Dimension;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

class StarDrawable extends Drawable {
    private final @Dimension int size;
    private StarAppearance appearance;
    private ColorStateList tintList;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private final float radius;
    private float innerRadius;

    public StarDrawable(int size) {
        this.size = size;

        this.radius = size / 2f;
        this.innerRadius = radius / 2.5f;

        paint.setStrokeJoin(Paint.Join.MITER);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        canvas.save();
        canvas.translate(getBounds().left + radius, getBounds().top + radius);

        path.moveTo((float) (Math.cos(18f / 180f * Math.PI) * radius),
                (float) -Math.sin(18f / 180f * Math.PI) * radius);
        path.lineTo((float) (Math.cos(54 / 180f * Math.PI) * innerRadius),
                (float) (-Math.sin(54 / 180f * Math.PI) * innerRadius));
        for (int i = 1; i < 5; i++) {
            path.lineTo((float) (Math.cos((18f + i * 72f) / 180f * Math.PI) * radius),
                    (float) -Math.sin((18f + i * 72f) / 180f * Math.PI) * radius);
            path.lineTo((float) (Math.cos((54 + i * 72) / 180f * Math.PI) * innerRadius),
                    (float) (-Math.sin((54 + i * 72) / 180f * Math.PI) * innerRadius));
        }
        path.close();
        canvas.drawPath(path, paint);
        canvas.restore();
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        paint.setColorFilter(colorFilter);
        invalidateSelf();
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    public void setTintList(@Nullable ColorStateList tintList) {
        if (this.tintList != tintList) {
            this.tintList = tintList;
            if (tintList != null) {
                paint.setColor(tintList.getDefaultColor());
                invalidateSelf();
            }
        }
    }

    @Override
    protected boolean onStateChange(int[] state) {
        int color = tintList.getColorForState(state, -1);
        if (color != paint.getColor()) {
            paint.setColor(color);
            invalidateSelf();
        }
        return true;
    }

    @Override
    public boolean isStateful() {
        return (tintList != null) && tintList.isStateful();
    }

    @Override
    public int getIntrinsicWidth() {
        return (size != 0) ? size : -1;
    }

    @Override
    public int getIntrinsicHeight() {
        return (size != 0) ? size : -1;
    }

    public void applyAppearance(Context context, @StyleRes int appearanceRes) {
        if (context == null) {
            return;
        }
        appearance = new StarAppearance();
        TypedArray a = context.obtainStyledAttributes(appearanceRes, R.styleable.StarAppearance);

        appearance.somatotype = a.getInt(R.styleable.StarAppearance_star_somatotype, 0);
        appearance.hollowed = a.getBoolean(R.styleable.StarAppearance_star_hollowed, false);
        appearance.outlineWidth = a.getDimensionPixelSize(
                R.styleable.StarAppearance_star_outline_width, 0);
        appearance.cornerRounded = a.getBoolean(R.styleable.StarAppearance_star_corner_rounded,
                false);
        appearance.cornerRadius = a.getDimensionPixelSize(
                R.styleable.StarAppearance_star_corner_radius, 0);

        a.recycle();

        applyAppearanceInternal();
    }

    public void applyAppearance(StarAppearance appearance) {
        this.appearance = appearance;
        applyAppearanceInternal();
    }

    StarAppearance getAppearance() {
        return this.appearance;
    }

    private void applyAppearanceInternal() {
        switch (appearance.somatotype) {
            case RatingBar.SOMATOTYPE_THIN:
                this.innerRadius = radius / 2.5f;
                break;
            case RatingBar.SOMATOTYPE_FAT:
                this.innerRadius = radius / 2f;
                break;
        }

        if (appearance.hollowed) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(appearance.outlineWidth);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setStrokeWidth(0);
        }

        if (appearance.cornerRounded) {
            paint.setPathEffect(new CornerPathEffect(appearance.cornerRadius));
        } else {
            paint.setPathEffect(null);
        }
    }

    static class StarAppearance {
        @RatingBar.StarSomatotype int somatotype;
        boolean hollowed;
        @Dimension int outlineWidth;
        boolean cornerRounded;
        int cornerRadius;
    }
}
