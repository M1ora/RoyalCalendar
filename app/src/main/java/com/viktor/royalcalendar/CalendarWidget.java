package com.viktor.royalcalendar;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CalendarWidget extends AppWidgetProvider {
  public static void updateAll(Context context) {
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    int[] ids = manager.getAppWidgetIds(new ComponentName(context, CalendarWidget.class));
    if (ids.length == 0) {
      cancel(context);
      return;
    }
    CalendarRenderer renderer = new CalendarRenderer(context);
    LocalDate today = LocalDate.now();
    String lang = CalendarRenderer.language(context);
    for (int id : ids) {
      Bundle options = manager.getAppWidgetOptions(id);
      int dp = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 400);
      int width = Math.max(600, Math.min(1200, dp * 3));
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
      views.setImageViewBitmap(R.id.calendar_image, renderer.render(today, lang, width));
      views.setContentDescription(
          R.id.calendar_image,
          today.format(
              DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.forLanguageTag(lang))));
      Intent open = new Intent(context, MainActivity.class);
      views.setOnClickPendingIntent(
          R.id.calendar_image,
          PendingIntent.getActivity(
              context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
      manager.updateAppWidget(id, views);
    }
    schedule(context);
  }

  static PendingIntent alarm(Context c) {
    return PendingIntent.getBroadcast(
        c,
        1,
        new Intent(c, DateReceiver.class).setAction("com.viktor.royalcalendar.MIDNIGHT"),
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  static void schedule(Context c) {
    c.getSystemService(AlarmManager.class)
        .setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, DateModel.nextMidnight(ZonedDateTime.now()), alarm(c));
  }

  static void cancel(Context c) {
    c.getSystemService(AlarmManager.class).cancel(alarm(c));
  }

  @Override
  public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
    updateAll(c);
  }

  @Override
  public void onAppWidgetOptionsChanged(Context c, AppWidgetManager m, int id, Bundle b) {
    updateAll(c);
  }

  @Override
  public void onEnabled(Context c) {
    updateAll(c);
  }

  @Override
  public void onDisabled(Context c) {
    cancel(c);
  }
}
