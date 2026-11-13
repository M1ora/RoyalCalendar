package com.viktor.royalcalendar;

import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import java.io.*;
import java.time.*;
import java.util.*;

public class WidgetChecks extends Instrumentation {
  @Override
  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    start();
  }

  private int count;

  private void check(boolean condition, String message) {
    if (!condition) throw new AssertionError(message);
    count++;
  }

  @Override
  public void onStart() {
    Bundle result = new Bundle();
    try {
      LocalDate[] year = DateModel.around(LocalDate.of(2026, 1, 1));
      check(year[0].equals(LocalDate.of(2025, 12, 30)), "Previous-year neighbour");
      check(year[4].equals(LocalDate.of(2026, 1, 3)), "Next-year neighbour");
      check(
          DateModel.around(LocalDate.of(2028, 3, 1))[1].equals(LocalDate.of(2028, 2, 29)),
          "Leap day");
      check(
          DateModel.around(LocalDate.of(2026, 3, 1))[1].equals(LocalDate.of(2026, 2, 28)),
          "Non-leap February");
      check(CalendarRenderer.language(getTargetContext()).equals("en"), "English sprite labels");
      ZonedDateTime dst = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, ZoneId.of("America/New_York"));
      check(
          DateModel.nextMidnight(dst) - dst.toInstant().toEpochMilli() == 23 * 3600000L,
          "DST-safe next midnight");
      // Test the shipped composition without changing the user's live tuning.
      getTargetContext().getSharedPreferences("widget_test_defaults", 0).edit().clear().commit();
      android.content.Context defaults =
          new android.content.ContextWrapper(getTargetContext()) {
            @Override
            public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
              return getTargetContext()
                  .getSharedPreferences(
                      name.equals("placement") ? "widget_test_defaults" : name, mode);
            }
          };
      CalendarRenderer renderer = new CalendarRenderer(defaults);
      Bitmap weekend = renderer.render(LocalDate.of(2026, 11, 13), "en", 1200);
      int wrongSaturday = 0, wrongSunday = 0, blueSaturday = 0, redSunday = 0;
      for (int y = CalendarRenderer.TOP_PADDING; y < 600 + CalendarRenderer.TOP_PADDING; y++)
        for (int x = 780; x < 1200; x++) {
          int p = weekend.getPixel(x, y);
          if ((p >>> 24) < 200) continue;
          int r = android.graphics.Color.red(p),
              g = android.graphics.Color.green(p),
              b = android.graphics.Color.blue(p);
          boolean red = r > 180 && g < 90 && b < 100, blue = b > 180 && g > 130 && r < 120;
          if (x < 1000) {
            if (red) wrongSaturday++;
            if (blue) blueSaturday++;
          } else {
            if (blue) wrongSunday++;
            if (red) redSunday++;
          }
        }
      check(wrongSaturday == 0 && blueSaturday > 100, "Saturday number is blue");
      check(wrongSunday == 0 && redSunday > 100, "Sunday number is red");
      int coloredLabels = 0;
      for (int y = 370 + CalendarRenderer.TOP_PADDING; y < 440 + CalendarRenderer.TOP_PADDING; y++)
        for (int x = 780; x < 970; x++) {
          int p = weekend.getPixel(x, y);
          if ((p >>> 24) > 200 && (android.graphics.Color.red(p) != android.graphics.Color.blue(p)))
            coloredLabels++;
        }
      for (int y = 270 + CalendarRenderer.TOP_PADDING; y < 325 + CalendarRenderer.TOP_PADDING; y++)
        for (int x = 1010; x < 1200; x++) {
          int p = weekend.getPixel(x, y);
          if ((p >>> 24) > 200 && (android.graphics.Color.red(p) != android.graphics.Color.blue(p)))
            coloredLabels++;
        }
      check(coloredLabels == 0, "Weekend labels stay monochrome");
      weekend.recycle();
      File dir = new File(getTargetContext().getExternalFilesDir(null), "checks");
      dir.mkdirs();
      Bitmap blade = Bitmap.createBitmap(780, 900, Bitmap.Config.ARGB_8888);
      android.graphics.Canvas bladeCanvas = new android.graphics.Canvas(blade);
      bladeCanvas.drawColor(0xff777777);
      android.graphics.drawable.Drawable vector =
          getTargetContext().getDrawable(R.drawable.dagger_original);
      vector.setBounds(0, 0, 780, 829);
      vector.draw(bladeCanvas);
      try (FileOutputStream stream = new FileOutputStream(new File(dir, "dagger.png"))) {
        blade.compress(Bitmap.CompressFormat.PNG, 100, stream);
      }
      blade.recycle();
      for (String lang : new String[] {"en"}) {
        for (int day = 1; day <= 31; day++) {
          Bitmap b = renderer.render(LocalDate.of(2026, 12, day), lang, 600);
          if (day >= 10)
            check(
                CalendarRenderer.firstDigitIsLower(day),
                "First digit is lower than the second for " + day);
          check(b.getPixel(0, 0) == 0 && b.getPixel(599, 299) == 0, "Transparent corners");
          boolean clearEdges = true;
          for (int x = 0; x < b.getWidth(); x++)
            if ((b.getPixel(x, 0) >>> 24) > 32 || (b.getPixel(x, b.getHeight() - 1) >>> 24) > 32)
              clearEdges = false;
          for (int y = 0; y < b.getHeight(); y++)
            if ((b.getPixel(0, y) >>> 24) > 32 || (b.getPixel(b.getWidth() - 1, y) >>> 24) > 32)
              clearEdges = false;
          if (!clearEdges) {
            try (FileOutputStream stream =
                new FileOutputStream(new File(dir, "failed-date-" + day + ".png"))) {
              b.compress(Bitmap.CompressFormat.PNG, 100, stream);
            }
          }
          check(clearEdges, "Unclipped artwork for date " + day);
          int nonzero = 0;
          for (int y = 0; y < b.getHeight(); y += 3)
            for (int x = 0; x < b.getWidth(); x += 3) if ((b.getPixel(x, y) >>> 24) > 0) nonzero++;
          check(nonzero > 500, "Visible date artwork " + lang + day);
          b.recycle();
        }
        Bitmap preview = renderer.render(LocalDate.now(), lang, 1200);
        try (FileOutputStream stream =
            new FileOutputStream(new File(dir, "preview-" + lang + ".png"))) {
          preview.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        preview.recycle();
      }
      for (String weekday :
          new String[] {
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
          })
        check(
            CalendarRenderer.weekdayLettersAreOpaque(weekday),
            "Opaque weekday lettering: " + weekday);
      for (int day : new int[] {1, 8, 18, 31}) {
        Bitmap sample = renderer.render(LocalDate.of(2026, 12, day), "en", 1200);
        try (FileOutputStream stream =
            new FileOutputStream(new File(dir, "date-" + day + ".png"))) {
          sample.compress(Bitmap.CompressFormat.PNG, 100, stream);
        }
        sample.recycle();
      }
      check(
          AppWidgetManager.getInstance(getTargetContext()).getInstalledProviders().stream()
              .anyMatch(
                  p -> p.provider.getPackageName().equals(getTargetContext().getPackageName())),
          "Widget registered with Android");
      CalendarWidget.updateAll(getTargetContext());
      result.putString(
          "stream",
          "PASS: "
              + count
              + " checks. English sprites, all 31 dates, leap/year boundaries, DST, transparency,"
              + " widget registration.\n");
      finish(-1, result);
    } catch (Throwable e) {
      result.putString("stream", "FAIL: " + e + "\n");
      finish(0, result);
    }
  }
}
