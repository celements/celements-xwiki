package com.celements.common.date;

import static com.google.common.base.Preconditions.*;

import java.lang.ref.SoftReference;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.ThreadSafe;
import javax.validation.constraints.NotEmpty;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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
  public static DateTimeFormatter ofPattern(String pattern) {
    try {
      return FORMATTER_CACHE.get(pattern);
    } catch (ExecutionException exc) {
      throw new DateTimeException("Illegal pattern: " + pattern, exc);
    }
  }

  /**
   * @throws DateTimeException
   *           by {@link Function#apply(Object)}
   */
  public static Function<TemporalAccessor, String> formatter(@NotEmpty final String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    return temporal -> ofPattern(pattern).format(Instant.from(temporal));
  }

  /**
   * @throws DateTimeException
   *           by {@link Function#apply(Object)}
   */
  public static Function<String, TemporalAccessor> parser(@NotEmpty final String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    return text -> ofPattern(pattern).parse(text);
  }

}
