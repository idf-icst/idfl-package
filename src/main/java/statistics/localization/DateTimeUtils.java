package statistics.localization;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class DateTimeUtils {

  public enum SimpleTimeUnit {
    SECOND(1),
    MINUTE(60 * SECOND.seconds),
    HOUR(60 * MINUTE.seconds),
    DAY(24 * HOUR.seconds),
    WEEK(7 * DAY.seconds),
    MONTH(30 * DAY.seconds),
    YEAR(365 * DAY.seconds),
    SECONDS(1),
    MINUTES(60 * SECOND.seconds),
    HOURS(60 * MINUTE.seconds),
    DAYS(24 * HOUR.seconds),
    WEEKS(7 * DAY.seconds),
    MONTHS(30 * DAY.seconds),
    YEARS(365 * DAY.seconds);

    int seconds;
    String unit;

    SimpleTimeUnit(int constant) {
      seconds = constant;
    }

    SimpleTimeUnit(String unit) {
      this.unit = unit;
    }

    public int getConstantValue() {
      return seconds;
    }

    public String getStringValue() {
      return unit;
    }
  }

  // Unix time format in SECONDS.
  public static long getCurrentUnixTime() {
    return System.currentTimeMillis() / 1000L;
  }

  public static long getUnixTimeBefore(long amount, SimpleTimeUnit unit) {
    return getCurrentUnixTime() - amount * unit.seconds;
  }

  public static long convertTimeStringToUnixTimestamp(String timeString, String... zoneId) {
    var dateTimeZone = parse(zoneId).map(zone -> zone[0]).map(ZoneId::of)
        .orElse(ZoneId.systemDefault());
    var dateTimePattern = "yyyy-MM-dd'T'HH:mm:ss";
    var dateTimeFormatter = DateTimeFormatter.ofPattern(dateTimePattern);
      if (timeString.contains(".")) {
          timeString = timeString.substring(0, timeString.indexOf("."));
      }
      if (timeString.contains(" ")) {
          timeString = timeString.replace(" ", "T");
      }
    LocalDateTime dateTime = LocalDateTime.parse(timeString, dateTimeFormatter);
    return dateTime.atZone(dateTimeZone).toEpochSecond();
  }

  private static Optional<String[]> parse(String... args) {
    return Optional.ofNullable(args.length > 0 ? args : null);
  }

  public static String convertUnixTimeStampToReadableString(long unixTimeStamp, String... zoneId) {
    var dateTimeZone = parse(zoneId).map(zone -> zone[0]).map(ZoneId::of)
        .orElse(ZoneId.systemDefault());
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimeStamp), dateTimeZone).toString();
  }

  public static String getCurrentTimeInFormat(String formatString, String... zoneId) {
    var dateTimeZone = parse(zoneId).map(zone -> zone[0]).map(ZoneId::of)
        .orElse(ZoneId.systemDefault());
    return LocalDateTime.now(dateTimeZone).format(DateTimeFormatter.ofPattern(formatString));
  }

  public static String getTimeAfterInFormat(String formatString, long hours, String... zoneId) {
    var dateTimeZone = parse(zoneId).map(zone -> zone[0]).map(ZoneId::of)
        .orElse(ZoneId.systemDefault());
    return LocalDateTime.now(dateTimeZone).plusHours(hours)
        .format(DateTimeFormatter.ofPattern(formatString));
  }

  public static String getBeforeTimeInFormat(SimpleTimeUnit unit, long count, String formatString,
      String... zoneId) {
    var dateTimeZone = parse(zoneId).map(zone -> zone[0]).map(ZoneId::of)
        .orElse(ZoneId.systemDefault());
    return LocalDateTime.now(dateTimeZone).minusSeconds(count * unit.getConstantValue())
        .format(DateTimeFormatter.ofPattern(formatString));
  }

  public static String convertUnixTimestampToString(long unixTimeStamp, String formatString,
      String... zoneId) {
    var dateTimeZone = parse(zoneId).map(zone -> zone[0]).map(ZoneId::of)
        .orElse(ZoneId.systemDefault());
    return LocalDateTime.ofInstant(Instant.ofEpochSecond(unixTimeStamp), dateTimeZone)
        .format(DateTimeFormatter.ofPattern(formatString));
  }
}
