package com.viktor.royalcalendar;

import java.time.LocalDate;
import java.time.ZonedDateTime;

public final class DateModel {
  public static LocalDate[] around(LocalDate today) {
    LocalDate[] result = new LocalDate[5];
    for (int i = 0; i < 5; i++) result[i] = today.plusDays(i - 2);
    return result;
  }

  public static long nextMidnight(ZonedDateTime now) {
    return now.toLocalDate().plusDays(1).atStartOfDay(now.getZone()).toInstant().toEpochMilli();
  }

  private DateModel() {}
}
