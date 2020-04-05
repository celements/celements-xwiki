package com.celements.common.date;

import static java.time.format.DateTimeFormatter.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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

}
