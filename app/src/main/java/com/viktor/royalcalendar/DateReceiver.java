package com.viktor.royalcalendar;

import android.content.*;

public class DateReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    CalendarWidget.updateAll(c);
  }
}
