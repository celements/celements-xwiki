package com.celements.logging;

import java.util.Objects;
import java.util.function.Supplier;

import org.slf4j.Logger;

public final class LogUtils {

  private LogUtils() {}

  public static void log(Logger logger, LogLevel LogLevel, String msg) {
    if ((logger != null) && (LogLevel != null)) {
      switch (LogLevel) {
        case TRACE:
          logger.trace(msg);
          break;
        case DEBUG:
          logger.debug(msg);
          break;
        case INFO:
          logger.info(msg);
          break;
        case WARN:
          logger.warn(msg);
          break;
        case ERROR:
          logger.error(msg);
          break;
      }
    }
  }

  public static void log(Logger logger, LogLevel LogLevel, String msg, Throwable throwable) {
    if ((logger != null) && (LogLevel != null)) {
      switch (LogLevel) {
        case TRACE:
          logger.trace(msg, throwable);
          break;
        case DEBUG:
          logger.debug(msg, throwable);
          break;
        case INFO:
          logger.info(msg, throwable);
          break;
        case WARN:
          logger.warn(msg, throwable);
          break;
        case ERROR:
          logger.error(msg, throwable);
          break;
      }
    }
  }

  public static void log(Logger logger, LogLevel LogLevel, String msg, Object... args) {
    if ((logger != null) && (LogLevel != null)) {
      switch (LogLevel) {
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

  public static boolean isLevelEnabled(Logger logger, LogLevel LogLevel) {
    boolean ret = false;
    if ((logger != null) && (LogLevel != null)) {
      switch (LogLevel) {
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
   * Defers given supplier call to {@link #toString()}. This allows lazy evaluation of lambda
   * expressions for Loggers (slf4j 1.x). Won't be needed anymore once slf4j 2+ is released.
   * Usage: {@code LOGGER.info(message, defer(() -> expensiveCall()))}.
   */
  public static <T> LogSupplier defer(Supplier<T> supplier) {
    return new LogSupplier() {

      @Override
      public String get() {
        return Objects.toString(supplier.get());
      }
    };
  }

  public static abstract class LogSupplier implements Supplier<String> {

    @Override
    public final String toString() {
      return this.get();
    }
  }

}
