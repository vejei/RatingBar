package io.github.vejei.ratingbar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Dimension;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.customview.view.AbsSavedState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatisticsView extends View {
    private static final Pattern DATA_PATTERN = Pattern.compile("^[(\\d+(?:\\.\\d+)?),\\s]+$");

    @IntDef(value = {DEFAULT_TYPEFACE, SANS, SERIF, MONOSPACE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface XMLTypefaceAttr{}
    private static final int DEFAULT_TYPEFACE = -1;
    private static final int SANS = 1;
    private static final int SERIF = 2;
    private static final int MONOSPACE = 3;

    private Drawable starDrawableFromUser;
    private int starSize;
    private int starMargin;
    private ColorStateList starTintList;
    private @StyleRes int starAppearanceRes;

    private int rowCount;
    private int rowMargin;
    private int columnMargin;
    private boolean digitalColumnDisabled;
    private int digitalColumnTextAppearance;
    private int percentageBarColor;
    private int percentageBarTrackColor;
    private float percentageBarWidth;
    private float percentageBarHeight;
    private boolean percentageBarCornerRounded;
    private float percentageBarCornerRadius;
    private String statisticsDataText;

    private TextAppearance digitalTextAppearance;

    private float[] data;
    private String[] dataText;

    private float halfViewHeight;

    private float starColumnWidth;
    private float starColumnHeight;
    private final float barColumnWidth;
    private final float barColumnHeight;

    private float starColumnStart;
    private float starColumnTop;
    private float barColumnStart;
    private float barColumnEnd;
    private float digitalColumnStart;
    private int topLayerBarSign = 1;

    private final Rect textBounds = new Rect();

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Drawable starDrawable;

    public StatisticsView(Context context) {
        this(context, null);
    }

    public StatisticsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.awesome_statistics_view_style);
    }

    public StatisticsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.StatisticsView);
    }

    public StatisticsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr,
                          int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.StatisticsView,
                defStyleAttr, defStyleRes);
        int totalRowMargin;

        starDrawableFromUser = a.getDrawable(R.styleable.StatisticsView_star);
        starSize = a.getDimensionPixelSize(R.styleable.StatisticsView_star_size, 0);
        starMargin = a.getDimensionPixelSize(R.styleable.StatisticsView_star_margin, 0);
        starTintList = a.getColorStateList(R.styleable.StatisticsView_star_tint);

        if (starDrawableFromUser == null) {
            starAppearanceRes = a.getResourceId(R.styleable.StatisticsView_star_appearance,
                    R.style.StatisticsViewStarAppearance);
        }
        createStarDrawable();
        applyStarDrawableTint();

        rowCount = a.getInteger(R.styleable.StatisticsView_row_count, 0);
        if (rowCount < 0) {
            rowCount = 0;
        }

        data = new float[rowCount];
        dataText = new String[rowCount];

        rowMargin = a.getDimensionPixelSize(R.styleable.StatisticsView_row_margin, 0);
        totalRowMargin = rowMargin * (rowCount - 1);

        // Computing the width and height of star column
        if (rowCount > 0) {
            starColumnWidth = starSize * rowCount + starMargin * (rowCount - 1);
            starColumnHeight = starSize * rowCount + totalRowMargin;
        }

        columnMargin = a.getDimensionPixelSize(R.styleable.StatisticsView_column_margin, 0);
        digitalColumnDisabled = a.getBoolean(R.styleable.StatisticsView_column_digital_disabled,
                false);
        if (!digitalColumnDisabled) {
            digitalColumnTextAppearance = a.getResourceId(
                    R.styleable.StatisticsView_column_digital_text_appearance,
                    R.style.StatisticsViewDigitalColumnTextAppearance);
            readTextAppearance();
            applyTextAppearance();
        }
        percentageBarColor = a.getColor(R.styleable.StatisticsView_percentage_bar_color,
                -1);
        percentageBarTrackColor = a.getColor(R.styleable.StatisticsView_percentage_bar_track_color,
                -1);
        if (a.hasValue(R.styleable.StatisticsView_percentage_bar_width)) {
            percentageBarWidth = a.getDimensionPixelSize(
                    R.styleable.StatisticsView_percentage_bar_width, 0);
        } else {
            percentageBarWidth = starColumnWidth * 2.3f;
        }

        if (a.hasValue(R.styleable.StatisticsView_percentage_bar_height)) {
            percentageBarHeight = a.getDimensionPixelSize(
                    R.styleable.StatisticsView_percentage_bar_height, 0);
        } else {
            percentageBarHeight = Math.round(starSize * 0.6f);
        }
        if (percentageBarHeight > starSize) {
            percentageBarHeight = starSize;
        }

        barColumnWidth = percentageBarWidth;
        barColumnHeight = percentageBarHeight * rowCount + totalRowMargin;

        percentageBarCornerRounded = a.getBoolean(
                R.styleable.StatisticsView_percentage_bar_corner_rounded, true);

        if (percentageBarCornerRounded) {
            if (a.hasValue(R.styleable.StatisticsView_percentage_bar_corner_radius)) {
                percentageBarCornerRadius = a.getDimensionPixelSize(
                        R.styleable.StatisticsView_percentage_bar_corner_radius, 0);
            } else {
                percentageBarCornerRadius = percentageBarHeight / 2f;
            }
        }

        statisticsDataText = a.getString(R.styleable.StatisticsView_statistics_data);
        if (statisticsDataText != null) {
            float[] resultData = parseStatisticsData();
            if (resultData != null) {
                feed(resultData);
            }
        }

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width, height;
        float margin;
        float maxTextWidth = 0;
        int totalTextHeight = 0;
        float digitalColumnWidth, digitalColumnHeight;

        if (!digitalColumnDisabled) {
            float maxColumnHeight;

            margin = columnMargin * 2;
            for (String item : dataText) {
                if (item == null) {
                    continue;
                }
                textBounds.setEmpty();
                textPaint.getTextBounds(item, 0, item.length(), textBounds);

                maxTextWidth = Math.max(textBounds.width(), maxTextWidth);
                totalTextHeight += textBounds.height();
            }
            digitalColumnWidth = maxTextWidth;
            digitalColumnHeight = totalTextHeight + rowMargin * (rowCount - 1);

            maxColumnHeight = Math.max(Math.max(starColumnHeight, barColumnHeight),
                    digitalColumnHeight);

            width = Math.round(starColumnWidth + barColumnWidth + margin + digitalColumnWidth);
            height = Math.round(maxColumnHeight);
        } else {
            margin = columnMargin;
            width = Math.round(starColumnWidth + barColumnWidth + margin);
            height = Math.round(Math.max(starColumnHeight, barColumnHeight));
        }

        halfViewHeight = height / 2f;
        width += getPaddingLeft() + getPaddingRight();
        height += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(resolveSize(width , widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();

        if (ViewUtils.isLayoutRtl(this)) {
            textPaint.setTextAlign(Paint.Align.RIGHT);
            topLayerBarSign = -1;

            starColumnStart = w - paddingRight;
            barColumnStart = starColumnStart - starColumnWidth - columnMargin;
            barColumnEnd = barColumnStart - barColumnWidth;
            digitalColumnStart = barColumnEnd - columnMargin;
        } else {
            textPaint.setTextAlign(Paint.Align.LEFT);
            topLayerBarSign = 1;

            starColumnStart = paddingLeft;
            barColumnStart = starColumnStart + starColumnWidth + columnMargin;
            barColumnEnd = barColumnStart + barColumnWidth;
            digitalColumnStart = barColumnEnd + columnMargin;
        }

        starColumnTop = getPaddingTop();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(0, Math.abs(starColumnHeight / 2f - halfViewHeight));

        canvas.save();
        if (ViewUtils.isLayoutRtl(this)) {
            canvas.translate(starColumnStart - starColumnWidth, starColumnTop);
        } else {
            canvas.translate(starColumnStart, starColumnTop);
        }

        // Draw the star column.
        for (int i = 0; i < rowCount; i++) {
            canvas.save();
            int starNumber = rowCount - i;

            // Move to the position of the next row in the vertical direction.
            if (ViewUtils.isLayoutRtl(this)) {
                canvas.translate(0, (starSize + rowMargin) * i);
            } else {
                canvas.translate(i * (starSize + starMargin), (starSize + rowMargin) * i);
            }

            // Draw a row of stars.
            for (int j = 1; j <= starNumber; j++) {
                starDrawable.draw(canvas);

                if (j < starNumber) {
                    canvas.translate(starSize + starMargin, 0);
                } else {
                    canvas.translate(starSize, 0);
                }
            }
            canvas.restore();
        }
        canvas.restore();

        canvas.save();
        canvas.translate(0, starColumnTop);

        // Draw the percentage bar.
        for (int i = 1; i <= rowCount; i++) {
            float centerY = (i * starSize) - starSize / 2f + (i - 1) * rowMargin;
            float top = centerY - percentageBarHeight / 2f;
            float bottom = centerY + percentageBarHeight / 2f;

            // Draw the track, that is the bottom layer.
            paint.setColor(percentageBarTrackColor);
            canvas.drawRoundRect(barColumnStart, top, barColumnEnd, bottom,
                    percentageBarCornerRadius, percentageBarCornerRadius, paint);
            // Draw the top layer.
            paint.setColor(percentageBarColor);

            canvas.save();
            canvas.clipRect(barColumnStart, top,
                    barColumnStart + percentageBarWidth * data[i - 1] * topLayerBarSign, bottom);
            canvas.drawRoundRect(barColumnStart, top, barColumnEnd, bottom,
                    percentageBarCornerRadius, percentageBarCornerRadius, paint);
            canvas.restore();
        }

        // Draw digital column texts.
        if (!digitalColumnDisabled && (data.length > 0)) {
            for (int i = 1; i <= rowCount; i++) {
                if (dataText[i - 1] == null) {
                    continue;
                }
                float centerY;
                float exactCenterY;

                textBounds.setEmpty();

                textPaint.getTextBounds(dataText[i - 1], 0, dataText[i - 1].length(), textBounds);
                centerY = (i * starSize) - starSize / 2f + (i - 1) * rowMargin;
                exactCenterY = textBounds.exactCenterY();

                canvas.drawText(dataText[i - 1], digitalColumnStart, centerY - exactCenterY,
                        textPaint);
            }
        }
        canvas.restore();

        canvas.restore();
    }

    private float[] parseStatisticsData() {
        if (statisticsDataText != null) {
            Matcher matcher = DATA_PATTERN.matcher(statisticsDataText);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("The statistics data text must match the pattern: "
                        + "a, b, c, d, e");
            }
            statisticsDataText = statisticsDataText.replace("\\s+", "");
            String[] numberText = statisticsDataText.split(",");
            float[] resultData = new float[numberText.length];
            for (int i = 0; i < numberText.length; i++) {
                resultData[i] = Float.parseFloat(numberText[i]);
            }

            return resultData;
        }
        return null;
    }

    private void createStarDrawable() {
        if (starDrawableFromUser != null) {
            starDrawable = starDrawableFromUser.getConstantState().newDrawable();
        } else {
            starDrawable = new StarDrawable(starSize);
            ((StarDrawable) starDrawable).applyAppearance(getContext(), starAppearanceRes);
        }
        starDrawable.setCallback(this);
        starDrawable.setBounds(0, 0, starSize, starSize);
    }

    private void applyStarDrawableTint() {
        starDrawable.setTintList(starTintList);
    }

    private void readTextAppearance() {
        @SuppressLint("CustomViewStyleable") TypedArray a = getContext()
                .obtainStyledAttributes(digitalColumnTextAppearance,
                        androidx.appcompat.R.styleable.TextAppearance);

        digitalTextAppearance = new TextAppearance();

        digitalTextAppearance.textSize = a.getDimensionPixelSize(
                androidx.appcompat.R.styleable.TextAppearance_android_textSize, 0);
        digitalTextAppearance.textColor = a.getColorStateList(
                androidx.appcompat.R.styleable.TextAppearance_android_textColor).getDefaultColor();
        digitalTextAppearance.textStyle = a.getInt(
                androidx.appcompat.R.styleable.TextAppearance_android_textStyle, -1);
        digitalTextAppearance.typefaceIndex = a.getInt(
                androidx.appcompat.R.styleable.TextAppearance_android_typeface, -1);

        a.recycle();
    }

    private void applyTextAppearance() {
        if (digitalTextAppearance.textSize != textPaint.getTextSize()) {
            textPaint.setTextSize(digitalTextAppearance.textSize);
        }
        if (digitalTextAppearance.textColor != textPaint.getColor()) {
            textPaint.setColor(digitalTextAppearance.textColor);
        }

        Typeface typeface;
        switch (digitalTextAppearance.typefaceIndex) {
            case SANS:
                typeface = Typeface.SANS_SERIF;
                break;
            case SERIF:
                typeface = Typeface.SERIF;
                break;
            case MONOSPACE:
                typeface = Typeface.MONOSPACE;
                break;
            case DEFAULT_TYPEFACE:
            default:
                typeface = null;
                break;
        }
        if (digitalTextAppearance.textStyle > 0) {
            if (typeface == null) {
                typeface = Typeface.defaultFromStyle(digitalTextAppearance.textStyle);
            } else {
                typeface = Typeface.create(typeface, digitalTextAppearance.textStyle);
            }

            textPaint.setTypeface(typeface);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = (typeface != null) ? typeface.getStyle() : 0;
            int need = digitalTextAppearance.textStyle & ~typefaceStyle;
            textPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            textPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            textPaint.setFakeBoldText(false);
            textPaint.setTextSkewX(0);
            textPaint.setTypeface(typeface);
        }
    }

    public Drawable getStarDrawable() {
        return starDrawable.getConstantState().newDrawable();
    }

    public void setStarDrawable(Drawable drawable) {
        if (starDrawableFromUser != drawable) {
            starDrawableFromUser = drawable;
            createStarDrawable();
            applyStarDrawableTint();
        }
    }

    public int getStarSize() {
        return starSize;
    }

    public void setStarSize(@Dimension int starSize) {
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
        if (this.starTintList != starTintList) {
            this.starTintList = starTintList;
            applyStarDrawableTint();
            invalidate();
        }
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
        requestLayout();
    }

    public int getRowMargin() {
        return rowMargin;
    }

    public void setRowMargin(int rowMargin) {
        this.rowMargin = rowMargin;
        requestLayout();
    }

    public int getColumnMargin() {
        return columnMargin;
    }

    public void setColumnMargin(int columnMargin) {
        this.columnMargin = columnMargin;
        requestLayout();
    }

    public boolean isDigitalColumnDisabled() {
        return digitalColumnDisabled;
    }

    public void setDigitalColumnDisabled(boolean digitalColumnDisabled) {
        this.digitalColumnDisabled = digitalColumnDisabled;
        requestLayout();
    }

    public int getDigitalColumnTextAppearance() {
        return digitalColumnTextAppearance;
    }

    public void setDigitalColumnTextAppearance(int digitalColumnTextAppearance) {
        this.digitalColumnTextAppearance = digitalColumnTextAppearance;
    }

    public int getPercentageBarColor() {
        return percentageBarColor;
    }

    public void setPercentageBarColor(int percentageBarColor) {
        this.percentageBarColor = percentageBarColor;
    }

    public int getPercentageBarTrackColor() {
        return percentageBarTrackColor;
    }

    public void setPercentageBarTrackColor(int percentageBarTrackColor) {
        this.percentageBarTrackColor = percentageBarTrackColor;
    }

    public float getPercentageBarWidth() {
        return percentageBarWidth;
    }

    public void setPercentageBarWidth(int percentageBarWidth) {
        this.percentageBarWidth = percentageBarWidth;
    }

    public float getPercentageBarHeight() {
        return percentageBarHeight;
    }

    public void setPercentageBarHeight(int percentageBarHeight) {
        this.percentageBarHeight = percentageBarHeight;
    }

    public boolean isPercentageBarCornerRounded() {
        return percentageBarCornerRounded;
    }

    public void setPercentageBarCornerRounded(boolean percentageBarCornerRounded) {
        this.percentageBarCornerRounded = percentageBarCornerRounded;
    }

    public float getPercentageBarCornerRadius() {
        return percentageBarCornerRadius;
    }

    public void setPercentageBarCornerRadius(int percentageBarCornerRadius) {
        this.percentageBarCornerRadius = percentageBarCornerRadius;
    }

    public void feed(float[] data) {
        int length;

        this.data = new float[rowCount];
        this.dataText = new String[rowCount];

        length = Math.min(this.data.length, data.length);
        System.arraycopy(data, 0, this.data, 0, length);
        for (int i = 0; i < this.data.length; i++) {
            dataText[i] = String.format(Locale.US, "%.1f%%", this.data[i] * 100);
        }
        requestLayout();
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.rowCount = rowCount;
        savedState.data = new float[rowCount];
        System.arraycopy(this.data, 0, savedState.data, 0, this.data.length);
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
        feed(savedState.data);
    }

    static class SavedState extends AbsSavedState {
        int rowCount;
        float[] data;

        protected SavedState(@NonNull Parcelable superState) {
            super(superState);
        }

        public SavedState(@NonNull Parcel source, @Nullable ClassLoader loader) {
            super(source, loader);
            rowCount = source.readInt();
            data = new float[rowCount];
            source.readFloatArray(data);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(rowCount);
            dest.writeFloatArray(data);
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

    private static class TextAppearance {
        int textSize;
        int textColor;
        @XMLTypefaceAttr int textStyle;
        int typefaceIndex;
    }
}
