package com.celements.common.date;

import static com.google.common.base.Preconditions.*;

import java.lang.ref.SoftReference;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalQuery;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

@ThreadSafe
public final class DateFormat {

  private DateFormat() {}

  private static final CacheLoader<String, DateTimeFormatter> FORMATTER_LOADER = new CacheLoader<String, DateTimeFormatter>() {

    @Override
    public DateTimeFormatter load(String pattern) throws Exception {
      return DateTimeFormatter.ofPattern(pattern);
    }
  };

  /**
   * {@link DateTimeFormatter} initialization is expensive, hence we use a {@link LoadingCache}.
   * To contain the memory impact we use a max size and {@link SoftReference} values.
   */
  private static final LoadingCache<String, DateTimeFormatter> FORMATTER_CACHE = CacheBuilder
      .newBuilder().maximumSize(100).softValues().build(FORMATTER_LOADER);

  /**
   * @throws DateTimeException
   *           on illegal pattern
   * @see DateTimeFormatter#ofPattern(String)
   */
  @NotNull
  public static DateTimeFormatter ofPattern(@NotEmpty String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    try {
      return FORMATTER_CACHE.get(pattern);
    } catch (ExecutionException | UncheckedExecutionException exc) {
      throw new DateTimeException("Illegal pattern: " + pattern, exc);
    }
  }

  /**
   * @throws DateTimeException
   *           by {@link Function#apply(Object)}
   */
  @NotNull
  public static Function<Temporal, String> formatter(@NotEmpty final String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    return temporal -> ofPattern(pattern).format(DateUtil.atZone(temporal));
  }

  /**
   * @throws DateTimeException
   *           by {@link Function#apply(Object)}
   */
  @NotNull
  public static Function<Temporal, String> formatter(@NotEmpty final String pattern,
      @NotNull final Locale locale) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    checkNotNull(locale);
    return temporal -> ofPattern(pattern).withLocale(locale).format(DateUtil.atZone(temporal));
  }



  /**
   * @throws DateTimeException
   *           by {@link Function#apply(Object)}
   */
  @NotNull
  public static Function<String, ZonedDateTime> parser(@NotEmpty final String pattern) {
    return parser(pattern, DateUtil.getDefaultZone());
  }

  /**
   * @throws DateTimeException
   *           by {@link Function#apply(Object)}
   */
  @NotNull
  public static Function<String, ZonedDateTime> parser(@NotEmpty final String pattern,
      @NotNull final ZoneId zone) {
    return parser(pattern, zone, ZonedDateTime::from, LocalDateTime::from, LocalDate::from,
        LocalTime::from, YearMonth::from, Year::from);
  }

  /**
   * @throws DateTimeException
   *           by {@link Function#apply(Object)}
   */
  @NotNull
  public static Function<String, ZonedDateTime> parser(@NotEmpty final String pattern,
      @NotNull final ZoneId zone, final TemporalQuery<?>... queries) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    checkNotNull(zone);
    return text -> DateUtil.atZone(ofPattern(pattern).parseBest(text, queries), zone);
  }

}
