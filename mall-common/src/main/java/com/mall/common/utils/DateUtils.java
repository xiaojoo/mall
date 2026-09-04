package com.mall.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 日期处理
 */
public class DateUtils {
	/** 时间格式(yyyy-MM-dd) */
	public final static String DATE_PATTERN = "yyyy-MM-dd";
	/** 时间格式(yyyy-MM-dd HH:mm:ss) */
	public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

	public static String format(Date date) {
        return format(date, DATE_PATTERN);
    }

    public static String format(Date date, String pattern) {
        if(date != null){
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }

    public static Date stringToDate(String strDate, String pattern) {
        if (StringUtils.isBlank(strDate)){
            return null;
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
        LocalDateTime ldt = LocalDateTime.parse(strDate, fmt);
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date[] getWeekStartAndEnd(int week) {
        LocalDate today = LocalDate.now();
        LocalDate date = today.plusWeeks(week);
        date = date.with(DayOfWeek.MONDAY);
        Date beginDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(date.plusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return new Date[]{beginDate, endDate};
    }

    public static Date addDateSeconds(Date date, int seconds) {
        Instant instant = date.toInstant().plusSeconds(seconds);
        return Date.from(instant);
    }

    public static Date addDateMinutes(Date date, int minutes) {
        Instant instant = date.toInstant().plus(Duration.ofMinutes(minutes));
        return Date.from(instant);
    }

    public static Date addDateHours(Date date, int hours) {
        Instant instant = date.toInstant().plus(Duration.ofHours(hours));
        return Date.from(instant);
    }

    public static Date addDateDays(Date date, int days) {
        LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusDays(days);
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date addDateWeeks(Date date, int weeks) {
        LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusWeeks(weeks);
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date addDateMonths(Date date, int months) {
        LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusMonths(months);
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    public static Date addDateYears(Date date, int years) {
        LocalDate ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().plusYears(years);
        return Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
