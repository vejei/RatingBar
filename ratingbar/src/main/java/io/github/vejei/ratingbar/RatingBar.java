package io.github.vejei.ratingbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.core.content.ContextCompat;
import androidx.customview.view.AbsSavedState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class RatingBar extends View {
    @IntDef({MODE_INTERACTIVE, MODE_INDICATIVE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {}

    public static final int MODE_INTERACTIVE = 0;
    public static final int MODE_INDICATIVE = 1;

    @IntDef({SOMATOTYPE_THIN, SOMATOTYPE_FAT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface StarSomatotype {}

    public static final int SOMATOTYPE_THIN = 0;
    public static final int SOMATOTYPE_FAT = 1;

    private @Mode int mode;
    private Drawable starDrawableFromUser;
    private int starCount;
    private int starSize;
    private int starMargin;
    private ColorStateList starTintList;
    private int starAppearanceRes;
    private float stepSize;
    private float ratingStarCount;

    private float totalSteps;

    private Drawable bottomLayerStar;
    private Drawable topLayerStar;
    private Drawable layers;

    private float touchDownX;
    private boolean isDragging;
    private final float scaleTouchSlop;
    private final Rect clipBounds = new Rect();

    private float offset;

    private int right;
    private int top;
    private int bottom;

    public RatingBar(Context context) {
        this(context, null);
    }

    public RatingBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.awesome_rating_bar_style);
    }

    public RatingBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.RatingBar);
    }

    public RatingBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                     int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.RatingBar,
                defStyleAttr, defStyleRes);

        mode = a.getInt(R.styleable.RatingBar_bar_mode, 0);
        starDrawableFromUser = a.getDrawable(R.styleable.RatingBar_star);
        starCount = a.getInteger(R.styleable.RatingBar_star_count, 0);
        starSize = a.getDimensionPixelSize(R.styleable.RatingBar_star_size, 0);
        starMargin = a.getDimensionPixelSize(R.styleable.RatingBar_star_margin, 0);

        if (a.hasValue(R.styleable.RatingBar_star_tint)) {
            starTintList = a.getColorStateList(R.styleable.RatingBar_star_tint);
        } else {
            starTintList = ContextCompat.getColorStateList(context, R.color.ratingbar_bottom_layer);
        }

        if (starDrawableFromUser == null) {
            starAppearanceRes = a.getResourceId(R.styleable.RatingBar_star_appearance,
                    R.style.StarAppearance);
        }

        stepSize = a.getFloat(R.styleable.RatingBar_step_size, 0.0f);
        if (stepSize < 0) {
            stepSize = 0.5f;
        }

        ratingStarCount = a.getFloat(R.styleable.RatingBar_rating_star_count, 0);
        if (ratingStarCount < 0) {
            ratingStarCount = 0.0f;
        }

        a.recycle();

        createDrawables();
        applyDrawablesTint();

        totalSteps = computeTotalSteps();
        scaleTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        setRatingStarCount(ratingStarCount);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width, height;

        width = computeDrawablesWidth() + getPaddingLeft() + getPaddingRight();
        height = starSize + getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        if (ViewUtils.isLayoutRtl(this)) {
            right = w - getPaddingRight();
        } else {
            right = getPaddingLeft();
        }

        top = getPaddingTop();
        bottom = top + starSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < starCount; i++) {
            if (bottomLayerStar != null) {
                setDrawableBounds(bottomLayerStar, i);
                bottomLayerStar.draw(canvas);
            }
        }

        computeClipBounds(offset);

        canvas.save();
        canvas.clipRect(clipBounds);
        for (int i = 0; i < starCount; i++) {
            if (topLayerStar != null) {
                setDrawableBounds(topLayerStar, i);
                topLayerStar.draw(canvas);
            }
        }
        canvas.restore();
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || (who == bottomLayerStar) || (who == topLayerStar);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        int[] states = getDrawableState();
        if (bottomLayerStar.isStateful()) {
            bottomLayerStar.setState(states);
        }
        if (topLayerStar.isStateful()) {
            topLayerStar.setState(states);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mode == MODE_INDICATIVE) {
            return false;
        }

        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchDownX = event.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    jumpToPosition(event);
                } else {
                    if (Math.abs(event.getX() - touchDownX) > scaleTouchSlop) {
                        startDrag(event);
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (isDragging) {
                    jumpToPosition(event);
                    isDragging = false;
                    setPressed(false);
                } else {
                    isDragging = true;
                    jumpToPosition(event);
                    isDragging = false;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                setPressed(false);
                break;
        }
        return true;
    }

    private void createDrawables() {
        if (starDrawableFromUser != null) {
            if (starDrawableFromUser instanceof LayerDrawable) {
                bottomLayerStar = ((LayerDrawable) starDrawableFromUser).getDrawable(0);
                topLayerStar = ((LayerDrawable) starDrawableFromUser).getDrawable(1);
            } else {
                topLayerStar = starDrawableFromUser.getConstantState().newDrawable();
                bottomLayerStar = starDrawableFromUser.getConstantState().newDrawable();
            }
        } else {
            bottomLayerStar = new StarDrawable(starSize);
            topLayerStar = new StarDrawable(starSize);

            applyStarAppearance();
        }
        bottomLayerStar.setCallback(this);
        bottomLayerStar.setBounds(0, 0, starSize, starSize);

        topLayerStar.setCallback(this);
        topLayerStar.setBounds(0, 0, starSize, starSize);

        layers = new LayerDrawable(new Drawable[] {bottomLayerStar, topLayerStar});
        layers.setCallback(this);
    }

    private void applyStarAppearance() {
        if (starDrawableFromUser != null || bottomLayerStar == null || topLayerStar == null) {
            return;
        }
        StarDrawable.StarAppearance bottomLayerAppearance;
        StarDrawable.StarAppearance topLayerAppearance = new StarDrawable.StarAppearance();

        ((StarDrawable) bottomLayerStar).applyAppearance(getContext(), starAppearanceRes);
        bottomLayerAppearance = ((StarDrawable) bottomLayerStar).getAppearance();

        topLayerAppearance.hollowed = false;
        topLayerAppearance.somatotype = bottomLayerAppearance.somatotype;
        topLayerAppearance.outlineWidth = bottomLayerAppearance.outlineWidth;
        topLayerAppearance.cornerRounded = bottomLayerAppearance.cornerRounded;
        topLayerAppearance.cornerRadius = bottomLayerAppearance.cornerRadius;

        ((StarDrawable) topLayerStar).applyAppearance(topLayerAppearance);
    }

    private void applyDrawablesTint() {
        if (bottomLayerStar != null) {
            bottomLayerStar.setTintList(starTintList);
        }

        if (topLayerStar != null) {
            int topLayerColor;
            int[] states = new int[]{
                    android.R.attr.state_pressed, android.R.attr.state_focused,
                    android.R.attr.state_selected, android.R.attr.state_checked
            };
            topLayerColor = starTintList.getColorForState(states, -1);

            if (topLayerColor == -1) {
                final TypedValue value = new TypedValue();
                if (getContext().getTheme().resolveAttribute(android.R.attr.colorControlActivated,
                        value, true)) {
                    topLayerColor = value.data;
                }
            }

            topLayerColor = Color.argb(255, Color.red(topLayerColor),
                    Color.green(topLayerColor), Color.blue(topLayerColor));

            topLayerStar.setTint(topLayerColor);
        }
    }

    private float computeTotalSteps() {
        return starCount / stepSize;
    }

    private int computeDrawablesWidth() {
        return Math.round(starSize * starCount + starMargin * (starCount - 1));
    }

    private void setDrawableBounds(Drawable drawable, int position) {
        int drawableLeft, drawableRight;

        if (ViewUtils.isLayoutRtl(this)) {
            drawableRight = right - starSize * position - starMargin * position;
            drawableLeft = drawableRight - starSize;
        } else {
            drawableLeft = right + starSize * position + starMargin * position;
            drawableRight = drawableLeft + starSize;
        }
        drawable.setBounds(drawableLeft, top, drawableRight, bottom);
    }

    private void startDrag(MotionEvent event) {
        setPressed(true);
        isDragging = true;
        jumpToPosition(event);
    }

    private void jumpToPosition(MotionEvent event) {
        int x = Math.round(event.getX());
        int width = getWidth();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int availableWidth = width - paddingLeft - paddingRight;

        if (ViewUtils.isLayoutRtl(this)) {
            if (x > (width - paddingRight)) {
                offset = 0.0f;
            } else if (x < paddingLeft) {
                offset = 1.0f;
            } else {
                offset = (availableWidth - x + paddingLeft) / (float) availableWidth;
            }
        } else {
            if (x < paddingLeft) {
                offset = 0f;
            } else if (x > width - paddingRight) {
                offset = 1f;
            } else {
                offset = (x - paddingLeft) / (float) availableWidth;
            }
        }

        float remaining = offset * availableWidth;
        float stepDimen = starSize * starCount / totalSteps;
        float stepsPerStar = computeStepsPerStar();
        int cutCount = 0;
        int stepCount = 0;

        while (remaining > 0) {
            if (cutCount < stepsPerStar) {
                remaining -= stepDimen;
                cutCount++;
                stepCount++;
            } else {
                cutCount = 0;
                remaining -= starMargin;
            }
        }

        offset = computeStepsWidth(stepCount) / availableWidth;
    }

    private float computeStepsPerStar() {
        return 1.0f / stepSize;
    }

    private float computeStepsWidth(int totalSteps) {
        if (totalSteps <= 0) {
            return 0;
        }
        float stepsStarCount = 1f * totalSteps / computeStepsPerStar();
        int marginCount = (int) (Math.ceil(stepsStarCount) - 1);
        ratingStarCount = stepsStarCount;
        return stepsStarCount * starSize + marginCount * starMargin;
    }

    private void computeClipBounds(float offset) {
        int width = getWidth();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int left, right;
        int distanceMoved = Math.round((width - paddingLeft - paddingRight) * offset);

        if (ViewUtils.isLayoutRtl(this)) {
            right = width - getPaddingRight();
            left = right - distanceMoved;
        } else {
            left = getPaddingLeft();
            right = getPaddingLeft() + distanceMoved;
        }

        clipBounds.set(left, getPaddingTop(), right, getPaddingTop() + starSize);
    }

    @Mode
    public int getMode() {
        return mode;
    }

    public void setMode(@Mode int mode) {
        this.mode = mode;
    }

    public Drawable getStarDrawable() {
        if (starDrawableFromUser != null) {
            return starDrawableFromUser;
        } else {
            return layers;
        }
    }

    public void setStarDrawable(Drawable starDrawable) {
        if (starDrawableFromUser != starDrawable) {
            starDrawableFromUser = starDrawable;
            createDrawables();
            invalidate();
        }
    }

    public int getStarCount() {
        return starCount;
    }

    public void setStarCount(int starCount) {
        this.starCount = starCount;
        totalSteps = computeTotalSteps();
        requestLayout();
    }

    public int getStarSize() {
        return starSize;
    }

    public void setStarSize(int starSize) {
        this.starSize = starSize;
        requestLayout();
    }

    public int getStarMargin() {
        return starMargin;
    }

    public void setStarMargin(int starMargin) {
        this.starMargin = starMargin;
        requestLayout();
    }

    public ColorStateList getStarTintList() {
        return starTintList;
    }

    public void setStarTintList(ColorStateList starTintList) {
        this.starTintList = starTintList;
        applyDrawablesTint();
        invalidate();
    }

    public void setStarAppearance(@StyleRes int starAppearanceRes) {
        this.starAppearanceRes = starAppearanceRes;
        if (starDrawableFromUser == null) {
            applyStarAppearance();
        }
    }

    public float getStepSize() {
        return stepSize;
    }

    public void setStepSize(float stepSize) {
        this.stepSize = stepSize;
        totalSteps = computeTotalSteps();
        invalidate();
    }

    public float getRatingStarCount() {
        return ratingStarCount;
    }

    public void setRatingStarCount(float ratingStarCount) {
        int stepCount;
        float stepsWidth;
        int availableWidth = computeDrawablesWidth();

        this.ratingStarCount = ratingStarCount;

        stepCount = (int) Math.ceil(ratingStarCount * computeStepsPerStar());
        stepsWidth = computeStepsWidth(stepCount);
        offset = stepsWidth / availableWidth;

        invalidate();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.ratingStarCount = ratingStarCount;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        setRatingStarCount(savedState.ratingStarCount);
    }

    static class SavedState extends AbsSavedState {
        float ratingStarCount;

        public SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        public SavedState(@NonNull Parcel source, @Nullable ClassLoader loader) {
            super(source, loader);
            ratingStarCount = source.readFloat();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeFloat(ratingStarCount);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new ClassLoaderCreator<SavedState>() {
                    @Override
                    public SavedState createFromParcel(Parcel source) {
                        return new SavedState(source, null);
                    }

                    @Override
                    public SavedState[] newArray(int size) {
                        return new SavedState[size];
                    }

                    @Override
                    public SavedState createFromParcel(Parcel source, ClassLoader loader) {
                        return new SavedState(source, loader);
                    }
                };
    }
}
