package com.attendance.utility;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.format.TextStyle;
import java.util.Locale;

public class DateHelper {

    public static String formatWeek(String isoWeek) {

        String[] parts = isoWeek.split("-");
        int year = Integer.parseInt(parts[0]);
        int week = Integer.parseInt(parts[1]);

        LocalDate start = LocalDate
                .ofYearDay(year, 1)
                .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                .with(DayOfWeek.MONDAY);

        LocalDate end = start.plusDays(6);

        String startMonth = start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String endMonth = end.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);

        return startMonth + " " + start.getDayOfMonth()
                + " - "
                + endMonth + " " + end.getDayOfMonth();
    }

    public static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY ||
                date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
