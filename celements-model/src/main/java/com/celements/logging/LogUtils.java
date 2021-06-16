package com.celements.logging;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.helpers.MessageFormatter;

public final class LogUtils {

  private LogUtils() {}

  public static void log(Logger logger, LogLevel level, Object msg) {
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

  public static void log(Logger logger, LogLevel level, Object msg, Throwable throwable) {
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

  public static void log(Logger logger, LogLevel level, Object msg, Object... args) {
    if ((logger != null) && (level != null)) {
      String msgStr = Objects.toString(msg);
      switch (level) {
        case TRACE:
          logger.trace(msgStr, args);
          break;
        case DEBUG:
          logger.debug(msgStr, args);
          break;
        case INFO:
          logger.info(msgStr, args);
          break;
        case WARN:
          logger.warn(msgStr, args);
          break;
        case ERROR:
          logger.error(msgStr, args);
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
      LogLevel level, Object msg) {
    return log(Optional<T>::isPresent).on(logger).lvl(level).msg(msg);
  }

  /**
   * Simplifies logging with lambda expressions in e.g. {@code filter} methods. Usage sample:
   *
   * <pre>
   * stream.filter(log(predicate).info(LOGGER).msg(() -> expensiveMsgCalc()))
   * </pre>
   *
   * @see LogPredicate
   */
  public static <T> LogPredicate<T> log(Predicate<T> predicate) {
    return new LogPredicate<>(predicate);
  }

  /**
   * helper if argument type inference is failing
   *
   * @see #log(Predicate)
   */
  public static <T> LogPredicate<T> logP(Predicate<T> predicate) {
    return log(predicate);
  }

  /**
   * Simplifies logging with lambda expressions. Logs object mapping.
   *
   * @deprecated use {@link #log(Function)} with {@link LogFunction} setters
   */
  @Deprecated
  public static <T, R> LogFunction<T, R> log(Function<T, R> function, Logger logger,
      LogLevel level, Object msg) {
    return log(function).on(logger).lvl(level).msg(msg);
  }

  /**
   * Simplifies logging with lambda expressions in {@code map} methods. Usage sample:
   *
   * <pre>
   * stream.map(log(function).info(LOGGER).msg(() -> expensiveMsgCalc()))
   * </pre>
   *
   * @see LogFunction
   */
  public static <T, R> LogFunction<T, R> log(Function<T, R> function) {
    return new LogFunction<>(function);
  }

  /**
   * helper if argument type inference is failing
   *
   * @see #log(Function)
   */
  public static <T, R> LogFunction<T, R> logF(Function<T, R> function) {
    return log(function);
  }

  /**
   * Simplifies logging with lambda expressions in e.g. {@code forEach} methods. Usage sample:
   *
   * <pre>
   * stream.forEach(log(consumer).info(LOGGER).msg(() -> expensiveMsgCalc()))
   * </pre>
   *
   * @see LogConsumer
   */
  public static <T> LogConsumer<T> log(Consumer<T> consumer) {
    return new LogConsumer<>(consumer);
  }

  /**
   * helper if argument type inference is failing
   *
   * @see #log(Consumer)
   */
  public static <T> LogConsumer<T> logC(Consumer<T> consumer) {
    return log(consumer);
  }

  /**
   * Simplifies logging with lambda expressions in e.g. {@code orElseGet} methods. Usage sample:
   *
   * <pre>
   * optional.orElseGet(log(supplier).info(LOGGER).msg(() -> expensiveMsgCalc()))
   * </pre>
   *
   * @see LogSupplier
   */
  public static <T> LogSupplier<T> log(Supplier<T> supplier) {
    return new LogSupplier<>(supplier);
  }

  /**
   * helper if argument type inference is failing
   *
   * @see #log(Supplier)
   */
  public static <T> LogSupplier<T> logS(Supplier<T> supplier) {
    return log(supplier);
  }

  public static <T> Supplier<String> format(final Supplier<T> supplier, final Object... args) {
    return format(supplier.get(), args);
  }

  public static <T> Supplier<String> format(final T msg, final Object... args) {
    return defer(() -> MessageFormatter.arrayFormat(Objects.toString(msg), args));
  }

}
