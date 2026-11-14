package com.viktor.royalcalendar;

import android.app.Instrumentation;
import android.appwidget.AppWidgetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import java.io.*;
import java.time.*;
import java.util.*;

public class WidgetChecks extends Instrumentation {
  private boolean verifyAlarm;
  private String alarmResult = "";

  @Override
  public void onCreate(Bundle arguments) {
    super.onCreate(arguments);
    verifyAlarm =
        arguments != null && Boolean.parseBoolean(arguments.getString("verify_alarm", "false"));
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
      ZonedDateTime justBefore =
          ZonedDateTime.of(2026, 11, 14, 23, 59, 59, 0, ZoneId.of("Asia/Yekaterinburg"));
      check(
          DateModel.nextMidnight(justBefore) - justBefore.toInstant().toEpochMilli() == 1000,
          "Midnight is the next second before the day changes");
      check(
          DateModel.nextMidnight(justBefore.plusSeconds(1))
                  - justBefore.plusSeconds(1).toInstant().toEpochMilli()
              == 86400000L,
          "Midnight reschedules to the following day");
      // Test the shipped composition without changing the user's live tuning.
      getTargetContext().getSharedPreferences("widget_test_defaults", 0).edit().clear().commit();
      android.content.Context defaults =
          new android.content.ContextWrapper(getTargetContext()) {
            @Override
            public android.content.SharedPreferences getSharedPreferences(String name, int mode) {
              return getTargetContext()
                  .getSharedPreferences(
                      name.equals("placement") || name.equals("appearance")
                          ? "widget_test_defaults"
                          : name,
                      mode);
            }
          };
      CalendarRenderer renderer = new CalendarRenderer(defaults);
      for (LocalDate date :
          new LocalDate[] {
            LocalDate.of(2026, 1, 1), LocalDate.of(2028, 2, 29),
            LocalDate.of(2026, 9, 30), LocalDate.of(2026, 12, 31)
          }) {
        for (int width : new int[] {600, 1200}) {
          for (boolean opaque : new boolean[] {false, true}) {
            for (int radius : new int[] {0, 64, 160}) {
              Bitmap reload = new ReloadRenderer(defaults).render(date, width, opaque, radius);
              check(reload.getHeight() == Math.round(width * 9 / 16f), "Reload is 16:9");
              int corner = reload.getPixel(0, 0);
              check(
                  corner == (opaque && radius == 0 ? 0xff181a27 : 0),
                  "Background mode and rounding apply to corners");
              check(
                  reload.getPixel(width / 2, 10) == (opaque ? 0xff181a27 : 0),
                  "Navy background fills only the opaque mode");
              check(
                  reload.getPixel(0, width / 5) == 0xff00489f,
                  "Blue ribbon reaches the widget edge");
              if (!opaque) {
                int[] pixels = new int[reload.getWidth() * reload.getHeight()];
                reload.getPixels(
                    pixels, 0, reload.getWidth(), 0, 0, reload.getWidth(), reload.getHeight());
                boolean noNavy = true;
                for (int pixel : pixels) noNavy &= pixel != 0xff181a27 && pixel != 0xff252e48;
                check(noNavy, "Transparent mode contains no navy backing");
              }
              if (width == 1200 && date.getMonthValue() == 9 && radius == 64) {
                File reloadDir = new File(getTargetContext().getExternalFilesDir(null), "checks");
                reloadDir.mkdirs();
                try (FileOutputStream stream =
                    new FileOutputStream(
                        new File(
                            reloadDir, opaque ? "reload-opaque.png" : "reload-transparent.png"))) {
                  reload.compress(Bitmap.CompressFormat.PNG, 100, stream);
                }
              }
              reload.recycle();
            }
          }
        }
      }
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
      if (verifyAlarm) verifyExactAlarmDelivery();
      LocalDate galleryDate = LocalDate.of(2026, 11, 14);
      Bitmap royalGallery = renderer.renderWidget(galleryDate, "en", 1200);
      try (FileOutputStream stream =
          new FileOutputStream(new File(dir, "royal-release-preview.png"))) {
        royalGallery.compress(Bitmap.CompressFormat.PNG, 100, stream);
      }
      royalGallery.recycle();
      Bitmap reloadGallery = new ReloadRenderer(defaults).render(galleryDate, 1200, true, 48);
      try (FileOutputStream stream =
          new FileOutputStream(new File(dir, "reload-release-preview.png"))) {
        reloadGallery.compress(Bitmap.CompressFormat.PNG, 100, stream);
      }
      reloadGallery.recycle();
      verifyPreviewLifecycle();
      result.putString(
          "stream",
          "PASS: "
              + count
              + " checks. English sprites, all 31 dates, leap/year boundaries, DST, transparency,"
              + " widget registration. "
              + alarmResult
              + "\n");
      finish(-1, result);
    } catch (Throwable e) {
      result.putString("stream", "FAIL: " + e + "\n");
      finish(0, result);
    }
  }

  private void verifyPreviewLifecycle() {
    android.content.SharedPreferences preferences =
        getTargetContext().getSharedPreferences("appearance", 0);
    boolean hadStyle = preferences.contains("style");
    String savedStyle = preferences.getString("style", "royal");
    android.app.Activity activity =
        startActivitySync(
            new android.content.Intent(getTargetContext(), MainActivity.class)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
    try {
      waitForIdleSync();
      verifyVisiblePreview(activity);
      for (int repeat = 0; repeat < 2; repeat++) {
        for (String label : new String[] {"Persona 5 / Royal", "Persona 3 Reload"}) {
          android.app.Activity current = activity;
          runOnMainSync(
              () -> {
                android.widget.RadioButton option =
                    findStyle(current.findViewById(android.R.id.content), label);
                check(option != null, "Theme selector exists");
                option.performClick();
              });
          waitForIdleSync();
          verifyVisiblePreview(activity);
        }
        ActivityMonitor monitor = addMonitor(MainActivity.class.getName(), null, false);
        android.app.Activity current = activity;
        runOnMainSync(current::recreate);
        android.app.Activity recreated = monitor.waitForActivityWithTimeout(5000);
        removeMonitor(monitor);
        check(recreated != null, "Activity recreates successfully");
        activity = recreated;
        waitForIdleSync();
        verifyVisiblePreview(activity);
      }
    } finally {
      android.content.SharedPreferences.Editor editor = preferences.edit();
      if (hadStyle) editor.putString("style", savedStyle);
      else editor.remove("style");
      editor.commit();
      android.app.Activity current = activity;
      if (current != null) runOnMainSync(current::finish);
      waitForIdleSync();
    }
  }

  private android.widget.RadioButton findStyle(android.view.View view, String label) {
    if (view instanceof android.widget.RadioButton
        && ((android.widget.RadioButton) view).getText().toString().equals(label))
      return (android.widget.RadioButton) view;
    if (view instanceof android.view.ViewGroup) {
      android.view.ViewGroup group = (android.view.ViewGroup) view;
      for (int i = 0; i < group.getChildCount(); i++) {
        android.widget.RadioButton hit = findStyle(group.getChildAt(i), label);
        if (hit != null) return hit;
      }
    }
    return null;
  }

  private void verifyVisiblePreview(android.app.Activity activity) {
    runOnMainSync(
        () -> {
          android.widget.ImageView preview = activity.findViewById(R.id.calendar_preview);
          check(preview != null && preview.isShown(), "Preview remains visible");
          check(
              preview.getWidth() > 0 && preview.getHeight() > 0,
              "Preview retains its measured dimensions");
          check(
              Math.abs(preview.getHeight() - Math.round(preview.getWidth() * 9 / 16f)) <= 1,
              "Preview keeps its 16:9 layout");
          check(
              preview.getDrawable() instanceof android.graphics.drawable.BitmapDrawable,
              "Preview keeps its image after theme switches and recreation");
          Bitmap bitmap =
              ((android.graphics.drawable.BitmapDrawable) preview.getDrawable()).getBitmap();
          check(!bitmap.isRecycled(), "Preview bitmap remains available");
        });
  }

  private void verifyExactAlarmDelivery() throws Exception {
    android.content.Context context = getTargetContext();
    check(CalendarWidget.exactAlarmsAllowed(context), "Exact alarm access is granted");
    String action = context.getPackageName() + ".TEST_EXACT_ALARM";
    java.util.concurrent.CountDownLatch delivered = new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicLong receivedAt =
        new java.util.concurrent.atomic.AtomicLong();
    android.content.BroadcastReceiver observer =
        new android.content.BroadcastReceiver() {
          @Override
          public void onReceive(android.content.Context c, android.content.Intent i) {
            receivedAt.set(System.currentTimeMillis());
            delivered.countDown();
          }
        };
    android.content.IntentFilter filter = new android.content.IntentFilter(action);
    if (android.os.Build.VERSION.SDK_INT >= 33)
      context.registerReceiver(observer, filter, android.content.Context.RECEIVER_NOT_EXPORTED);
    else context.registerReceiver(observer, filter);
    android.app.PendingIntent operation =
        android.app.PendingIntent.getBroadcast(
            context,
            991,
            new android.content.Intent(action).setPackage(context.getPackageName()),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
                | android.app.PendingIntent.FLAG_IMMUTABLE);
    try {
      long target = System.currentTimeMillis() + 5000;
      check(
          CalendarWidget.scheduleAt(context, target, operation),
          "The real scheduler selects an exact alarm");
      check(
          delivered.await(20, java.util.concurrent.TimeUnit.SECONDS),
          "The scheduled alarm is delivered");
      long delay = receivedAt.get() - target;
      check(
          delay >= -100 && delay < 2500,
          "Exact delivery delay is below 2.5 seconds: " + delay + "ms");
      alarmResult = "Exact alarm delivery: " + delay + "ms.";
    } finally {
      context.getSystemService(android.app.AlarmManager.class).cancel(operation);
      operation.cancel();
      context.unregisterReceiver(observer);
    }
  }
}
