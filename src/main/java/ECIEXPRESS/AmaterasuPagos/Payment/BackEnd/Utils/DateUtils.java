package ECIEXPRESS.AmaterasuPagos.Payment.BackEnd.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public final class DateUtils {

    private DateUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static final String ISO_DATE_FORMAT = "yyyy-MM-dd";
    public static final String ISO_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DISPLAY_DATE_FORMAT = "dd/MM/yyyy";
    public static final String DISPLAY_DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";
    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    public static String formatDate(Date date, String format) {
        SimpleDateFormat formatter = new SimpleDateFormat(format);
        return formatter.format(date);
    }

    public static Date parseDate(String dateString, String format) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            return formatter.parse(dateString);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Error parsing date: " + dateString + " (format=" + format + ")", e);
        }
    }

    public static String formatLocalDateTime(LocalDateTime dateTime, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return dateTime.format(formatter);
    }

    public static LocalDateTime parseLocalDateTime(String dateString, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.parse(dateString, formatter);
    }

    public static String toIsoString(LocalDateTime dateTime) {
        return formatLocalDateTime(dateTime, ISO_DATETIME_FORMAT);
    }

    public static LocalDateTime fromIsoString(String dateString) {
        return parseLocalDateTime(dateString, ISO_DATETIME_FORMAT);
    }

    public static String toDisplayString(LocalDateTime dateTime) {
        return formatLocalDateTime(dateTime, DISPLAY_DATETIME_FORMAT);
    }

    public static String currentDateTimeAsString(String format) {
        return formatLocalDateTime(LocalDateTime.now(), format);
    }

    public static String currentIsoDateTime() {
        return toIsoString(LocalDateTime.now());
    }
}
