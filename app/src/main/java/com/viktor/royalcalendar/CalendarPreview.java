package com.viktor.royalcalendar;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;

/** Own the preview's aspect ratio instead of relying on a wrapping parent's layout passes. */
final class CalendarPreview extends ImageView {
  CalendarPreview(Context context) {
    super(context);
    setScaleType(ScaleType.FIT_CENTER);
    setAdjustViewBounds(false);
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    int width = View.MeasureSpec.getSize(widthSpec);
    int height = Math.round(width * 9 / 16f);
    super.onMeasure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
  }
}
