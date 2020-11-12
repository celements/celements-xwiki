package com.celements.logging;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;

public final class LogUtils {

  private LogUtils() {}

  public static void log(Logger logger, LogLevel level, String msg) {
    log(logger, level, () -> msg);
  }

  public static void log(Logger logger, LogLevel level, Supplier<?> msg) {
    if ((logger != null) && (level != null)) {
      switch (level) {
        case TRACE:
          logger.trace("{}", defer(msg));
          break;
        case DEBUG:
          logger.debug("{}", defer(msg));
          break;
        case INFO:
          logger.info("{}", defer(msg));
          break;
        case WARN:
          logger.warn("{}", defer(msg));
          break;
        case ERROR:
          logger.error("{}", defer(msg));
          break;
      }
    }
  }

  public static void log(Logger logger, LogLevel level, String msg, Throwable throwable) {
    log(logger, level, () -> msg, throwable);
  }

  public static void log(Logger logger, LogLevel level, Supplier<?> msg, Throwable throwable) {
    if ((logger != null) && (level != null)) {
      switch (level) {
        case TRACE:
          logger.trace("{}", defer(msg), throwable);
          break;
        case DEBUG:
          logger.debug("{}", defer(msg), throwable);
          break;
        case INFO:
          logger.info("{}", defer(msg), throwable);
          break;
        case WARN:
          logger.warn("{}", defer(msg), throwable);
          break;
        case ERROR:
          logger.error("{}", defer(msg), throwable);
          break;
      }
    }
  }

  public static void log(Logger logger, LogLevel level, String msg, Object... args) {
    if ((logger != null) && (level != null)) {
      switch (level) {
        case TRACE:
          logger.trace(msg, args);
          break;
        case DEBUG:
          logger.debug(msg, args);
          break;
        case INFO:
          logger.info(msg, args);
          break;
        case WARN:
          logger.warn(msg, args);
          break;
        case ERROR:
          logger.error(msg, args);
          break;
      }
    }
  }

  public static boolean isLevelEnabled(Logger logger, LogLevel level) {
    boolean ret = false;
    if ((logger != null) && (level != null)) {
      switch (level) {
        case TRACE:
          ret = logger.isTraceEnabled();
          break;
        case DEBUG:
          ret = logger.isDebugEnabled();
          break;
        case INFO:
          ret = logger.isInfoEnabled();
          break;
        case WARN:
          ret = logger.isWarnEnabled();
          break;
        case ERROR:
          ret = logger.isErrorEnabled();
          break;
      }
    }
    return ret;
  }

  /**
   * Defers given supplier call to {@link #toString()}. This allows lazy evaluation with lambda
   * expressions for slf4j 1.x loggers.
   * Usage: {@code LOGGER.info(message, defer(() -> expensiveCall()))}.
   *
   * @see https://jira.qos.ch/browse/SLF4J-371
   */
  public static <T> Supplier<String> defer(final Supplier<T> supplier) {
    return new Supplier<String>() {

      @Override
      public String get() {
        return Objects.toString(supplier.get());
      }

      @Override
      public String toString() {
        return this.get();
      }
    };
  }

  /**
   * Simplifies logging with lambda expressions. Logs object filtering.
   *
   * @deprecated use {@link #log(Predicate)} with {@link LogPredicate} setters
   */
  @Deprecated
  public static <T> LogPredicate<T> log(Predicate<T> predicate, Logger logger,
      LogLevel levelMatched, LogLevel levelSkipped, String msg) {
    return log(predicate).on(logger).lvl(levelMatched).lvlReduced(levelSkipped).msg(msg);
  }

  /**
   * Simplifies logging with lambda expressions. Logs object filtering. Skipped objects are logged
   * one level below given level.
   *
   * @deprecated use {@link #log(Predicate)} with {@link LogPredicate} setters
   */
  @Deprecated
  public static <T> LogPredicate<T> log(Predicate<T> predicate, Logger logger,
      LogLevel level, String msg) {
    return log(predicate).on(logger).lvl(level).msg(msg);
  }

  /**
   * Simplifies logging with lambda expressions. Logs present {@link Optional} in a stream.
   */
  public static <T> LogPredicate<Optional<T>> logIfPresent(Logger logger,
      LogLevel level, String msg) {
    return log(Optional<T>::isPresent).on(logger).lvl(level).msg(msg);
  }

  /**
   * Simplifies logging with lambda expressions in {@code filter} methods. Usage sample:
   *
   * <pre>
   * stream.filter(log(predicate).on(LOGGER).lvl(INFO).msg(() -> expensiveMsgCalc()))
   * </pre>
   *
   * @see LogPredicate
   */
  public static <T> LogPredicate<T> log(Predicate<T> predicate) {
    return new LogPredicate<>(predicate);
  }

  /**
   * Simplifies logging with lambda expressions. Logs object mapping.
   *
   * @deprecated use {@link #log(Function)} with {@link LogFunction} setters
   */
  @Deprecated
  public static <T, R> LogFunction<T, R> log(Function<T, R> function, Logger logger,
      LogLevel level, String msg) {
    return log(function).on(logger).lvl(level).msg(msg);
  }

  /**
   * Simplifies logging with lambda expressions in {@code map} methods. Usage sample:
   *
   * <pre>
   * stream.map(log(function).on(LOGGER).lvl(INFO).msg(() -> expensiveMsgCalc()))
   * </pre>
   *
   * @see LogFunction
   */
  public static <T, R> LogFunction<T, R> log(Function<T, R> function) {
    return new LogFunction<>(function);
  }

}
