package io.github.vejei.ratingbar;

import android.view.View;

public class ViewUtils {

    private ViewUtils() {}

    public static boolean isLayoutRtl(View view) {
        return view.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
    }
}
