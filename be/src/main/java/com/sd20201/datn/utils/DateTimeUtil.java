package com.sd20201.datn.utils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Calendar;
import java.util.Date;

public class DateTimeUtil {

    public static Long convertDateToTimeStampSecond(Date startDate) {
        if (startDate != null) {
            return startDate.getTime() / 1000;
        }
        return null;
    }

    public static Date addMinutes(Date date, int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    public static Long getCurrentTimeStampSecond() {
        return System.currentTimeMillis() / 1000;
    }

    public static Long getCurrentTimeMillisecondsStamp() {return System.currentTimeMillis();}

    public static Long getMillisecondsThisWeek() {
        LocalDate today = LocalDate.now();

        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();

        ZonedDateTime zonedStartOfWeek = startOfWeekDateTime.atZone(ZoneId.systemDefault());

        Instant timestamp = zonedStartOfWeek.toInstant();

        return timestamp.toEpochMilli();
    }

    public static Long getTodayMilliseconds() {
        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay = today.atStartOfDay();

        ZonedDateTime zonedDateTime = startOfDay.atZone(ZoneId.systemDefault());

        Instant timestamp = zonedDateTime.toInstant();

        return timestamp.toEpochMilli();
    }

}
