package com.celements.common.function;

import static com.google.common.base.Preconditions.*;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.Date;

import javax.annotation.concurrent.ThreadSafe;

import com.celements.common.date.DateFormat;
import com.google.common.base.Function;
import com.google.common.base.Strings;

@ThreadSafe
public final class DateFormatFunctions {

  private DateFormatFunctions() {}

  /**
   * @deprecated since 4.5, instead use {@link DateFormat#formatter(String)}
   */
  @Deprecated
  public static Function<Date, String> format(final String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    return date -> {
      try {
        return DateFormat.formatter(pattern).apply(date.toInstant());
      } catch (DateTimeException exc) {
        throw new IllegalArgumentException(exc);
      }
    };
  }

  /**
   * @deprecated since 4.5, instead use {@link DateFormat#parse(String)}
   */
  @Deprecated
  public static Function<String, Date> parse(final String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    return str -> {
      try {
        return Date.from(Instant.from(DateFormat.parser(pattern).apply(str)));
      } catch (DateTimeException exc) {
        throw new IllegalArgumentException(exc);
      }
    };
  }

}
