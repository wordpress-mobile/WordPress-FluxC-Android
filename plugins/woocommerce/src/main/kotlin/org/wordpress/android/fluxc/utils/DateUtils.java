package org.wordpress.android.fluxc.utils;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.model.SiteModel;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    private static final String DATE_FORMAT_DEFAULT = "yyyy-MM-dd";

    /**
     * Given a {@link SiteModel} and a {@link String} compatible with {@link SimpleDateFormat}
     * and a {@param dateString}
     * returns a formatted date that accounts for the site's timezone setting.
     *
     */
    public static @NonNull String getDateTimeForSite(@NonNull SiteModel site,
                                                     @NonNull String pattern,
                                                     String dateString) {
        /* If the date string is null, the current date is taken */
        Date currentDate = new Date();
        Date date = (dateString == null || dateString.isEmpty()) ? currentDate : getDateFromString(dateString);

        /*
         * Since only date is provided without the time,
         * by default the time is set to the start of the day.
         *
         * This might cause timezone issues so getting the current time
         * and setting this time to the date value
         * */
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, currentDate.getHours());
        calendar.add(Calendar.MINUTE, currentDate.getMinutes());
        calendar.add(Calendar.SECOND, currentDate.getSeconds());

        return SiteUtils.getDateTimeForSite(site, pattern, calendar.getTime());
    }

    /**
     * returns a {@link Date} instance
     * based on {@param pattern} and {@param dateString}
     */
    public static Date getDateFromString(String dateString) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_DEFAULT, Locale.ROOT);
        try {
            return dateFormat.parse(dateString);
        } catch (ParseException e) {
            return new Date();
        }
    }


    /**
     * returns a {@link String} formatted
     * based on {@param pattern} and {@param date}
     */
    public static String formatDate(String pattern,
                                    Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.ROOT);
        return dateFormat.format(date);
    }



    /**
     * Given a {@link SimpleDateFormat} instance and the {@link String} start date string,
     * returns a {@link Calendar} instance.
     * The start date time is set to 00:00:00
     */
    public static Calendar getStartDateCalendar(@NonNull Date startDate) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(startDate);
        cal1.set(Calendar.HOUR_OF_DAY, 0);
        cal1.set(Calendar.MINUTE, 0);
        cal1.set(Calendar.SECOND, 0);
        cal1.set(Calendar.MILLISECOND, 0);

        return cal1;
    }



    /**
     * Given a {@link SimpleDateFormat} instance and the {@link String} end date string,
     * returns a {@link Calendar} instance.
     * The end date time is set to 23:59:59
     */
    public static Calendar getEndDateCalendar(Date endDate) {
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(endDate);
        cal2.set(Calendar.HOUR_OF_DAY, 23);
        cal2.set(Calendar.MINUTE, 59);
        cal2.set(Calendar.SECOND, 59);
        cal2.set(Calendar.MILLISECOND, 59);
        return cal2;
    }



    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.DAYS]
     */
    public static long getQuantityInDays(@NonNull Calendar c1,
                                         @NonNull Calendar c2) {
        long millis1 = c1.getTimeInMillis();
        long millis2 = c2.getTimeInMillis();

        long diff = Math.abs(millis2 - millis1);
        return (long) Math.ceil(diff / (double) (24 * 60 * 60 * 1000));
    }

    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.WEEKS]
     */
    public static long getQuantityInWeeks(@NonNull Calendar c1,
                                          @NonNull Calendar c2) {
        /*
         * start date: if day of week is greater than 1: set to 1
         * end date: if day of week is less than 7: set to 7
         *
         * This logic is to handle half week scenarios, for instance if the
         * start date = 2019-01-25 and end date = 2019-01-28 - the difference
         * in weeks should be 2 since the dates are actually in two different weeks
         *
         * */
        if (c1.get(Calendar.DAY_OF_WEEK) > 1) c1.set(Calendar.DAY_OF_WEEK, 1);
        if (c2.get(Calendar.DAY_OF_WEEK) < 1) c2.set(Calendar.DAY_OF_WEEK, 7);

        double diffInDays = getQuantityInDays(c1, c2);
        return (long) Math.ceil(diffInDays / 7);
    }


    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.MONTHS]
     */
    public static long getQuantityInMonths(@NonNull Calendar c1,
                                           @NonNull Calendar c2) {
        /*
         * start date: if day of month is greater than 1: set to 1
         * end date: if day of month is less than the maximum day of month for that particular month:
         * set to maximum day of month for that particular month
         *
         * This is to handle scenarios where the start date such as if start date = 12/31/18 and end date = 1/1/19,
         * the default difference in months would be 1, but it should be 2 since these are two separate months
         * */
        if (c1.get(Calendar.DAY_OF_MONTH) > 1) {
            c1.set(Calendar.DAY_OF_MONTH, 1);
        }
        if (c2.get(Calendar.DAY_OF_MONTH) < c2.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            c2.set(Calendar.DAY_OF_MONTH, c2.getActualMaximum(Calendar.DAY_OF_MONTH));
        }

        long diff = 0;
        if (c2.after(c1)) {
            while (c2.after(c1)) {
                if (c2.after(c1)) {
                    diff++;
                }
                c1.add(Calendar.MONTH, 1);
            }
        }
        return Math.abs(diff);
    }


    /**
     * Given a {@link Calendar} instance for startDate and endDate,
     * returns a quantity that is calculated based on [StatsGranularity.YEARS]
     */
    public static long getQuantityInYears(@NonNull Calendar c1,
                                          @NonNull Calendar c2) {
        /*
         * start date: if day of year is greater than 1: set to 1
         * end date: if day of year is less than the maximum day of year for that particular year:
         * set to maximum day of year for that particular year
         *
         * This is to handle scenarios where the start date such as if start date = 12/31/18 and end date = 1/1/19,
         * the default difference in years would be 1, but it should be 2 since these are two separate years
         * */
        if (c1.get(Calendar.DAY_OF_YEAR) > 1) {
            c1.set(Calendar.DAY_OF_YEAR, 1);
        }
        if (c2.get(Calendar.DAY_OF_YEAR) < c2.getActualMaximum(Calendar.DAY_OF_YEAR)) {
            c2.set(Calendar.DAY_OF_YEAR, c2.getActualMaximum(Calendar.DAY_OF_YEAR));
        }

        long diff = 0;
        if (c2.after(c1)) {
            while (c2.after(c1)) {
                if (c2.after(c1)) {
                    diff++;
                }
                c1.add(Calendar.YEAR, 1);
            }
        }
        return Math.abs(diff);
    }
}
