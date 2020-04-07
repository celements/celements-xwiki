package com.celements.common.date;

import static java.time.format.DateTimeFormatter.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import javax.validation.constraints.NotNull;

import com.celements.configuration.ConfigSourceUtils;

public final class DateUtil {

  public static final Instant INSTANT_MIN = ISO_INSTANT.parse("0001-01-01T00:00:00Z",
      Instant::from);
  public static final Instant INSTANT_MAX = ISO_INSTANT.parse("9999-12-31T00:00:00Z",
      Instant::from);

  public static final DateTimeFormatter FORMAT_DE_DATE = DateTimeFormatter
      .ofPattern("dd.MM.yyyy");
  public static final DateTimeFormatter FORMAT_DE_TIME = DateTimeFormatter
      .ofPattern("dd.MM.yyyy HH:mm");
  public static final DateTimeFormatter FORMAT_DE_TIME_S = DateTimeFormatter
      .ofPattern("dd.MM.yyyy HH:mm:ss");

  private DateUtil() {}

  @NotNull
  public static ZoneId getDefaultZone() {
    try {
      return ConfigSourceUtils.getStringProperty("celements.time.zone").toJavaUtil()
          .map(ZoneId::of).orElseGet(ZoneId::systemDefault);
    } catch (DateTimeException exc) {
      return ZoneId.systemDefault();
    }
  }

  /**
   * @throws DateTimeException
   *           if no conversion to ZonedDateTime is possible
   */
  @NotNull
  public static ZonedDateTime atZone(@NotNull TemporalAccessor temporal) {
    return atZone(temporal, getDefaultZone());
  }

  /**
   * @throws DateTimeException
   *           if no conversion to ZonedDateTime is possible
   */
  @NotNull
  public static ZonedDateTime atZone(@NotNull TemporalAccessor temporal, @NotNull ZoneId zone) {
    if (temporal instanceof ZonedDateTime) {
      return (ZonedDateTime) temporal;
    } else if (temporal instanceof Instant) {
      return ((Instant) temporal).atZone(zone);
    } else if (temporal instanceof LocalDateTime) {
      return ((LocalDateTime) temporal).atZone(zone);
    } else if (temporal instanceof LocalDate) {
      return atZone(((LocalDate) temporal).atStartOfDay(), zone);
    } else if (temporal instanceof YearMonth) {
      return atZone(((YearMonth) temporal).atDay(1), zone);
    } else if (temporal instanceof Year) {
      return atZone(((Year) temporal).atMonth(1), zone);
    } else {
      return ZonedDateTime.from(temporal);
    }
  }

}
