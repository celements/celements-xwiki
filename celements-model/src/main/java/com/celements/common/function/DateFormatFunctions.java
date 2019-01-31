package com.celements.common.function;

import static com.google.common.base.Preconditions.*;

import java.lang.ref.SoftReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;

import javax.annotation.concurrent.ThreadSafe;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@ThreadSafe
public final class DateFormatFunctions {

  private DateFormatFunctions() {
  }

  /**
   * {@link DateFormat} isn't thread safe and its initialization is expensive, hence we use
   * {@link ThreadLocal} combined with a {@link LoadingCache}. The cache instances leak in a
   * thread pool. To contain the memory impact we use a max size and {@link SoftReference} values.
   */
  private static final ThreadLocal<LoadingCache<String, DateFormat>> DATE_FORMAT_CACHE = new ThreadLocal<LoadingCache<String, DateFormat>>() {

    @Override
    protected LoadingCache<String, DateFormat> initialValue() {
      return CacheBuilder.newBuilder().maximumSize(100).softValues().build(DF_LOADER);
    }
  };

  private static final CacheLoader<String, DateFormat> DF_LOADER = new CacheLoader<String, DateFormat>() {

    @Override
    public DateFormat load(String pattern) throws Exception {
      return new SimpleDateFormat(pattern);
    }
  };

  public static Function<Date, String> format(final String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    return new Function<Date, String>() {

      @Override
      public String apply(Date date) {
        return getDateFormatter(pattern).format(date);
      }
    };
  }

  public static Function<String, Date> parse(final String pattern) {
    checkArgument(!Strings.isNullOrEmpty(pattern));
    return new Function<String, Date>() {

      @Override
      public Date apply(String str) {
        try {
          return getDateFormatter(pattern).parse(str);
        } catch (ParseException exc) {
          throw new IllegalArgumentException(exc);
        }
      }
    };
  }

  private static DateFormat getDateFormatter(String pattern) {
    try {
      return DATE_FORMAT_CACHE.get().get(pattern);
    } catch (ExecutionException exc) {
      throw new IllegalArgumentException("Illegal pattern: " + pattern, exc);
    }
  }

}
