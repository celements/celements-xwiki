package com.celements.logging;

import static com.google.common.base.Predicates.*;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Loggable<T, L extends Loggable<T, L>> {

  private Logger logger;
  protected LogLevel levelMatched = LogLevel.DEBUG;
  protected LogLevel levelSkipped = LogLevel.TRACE;
  protected Supplier<?> msg = () -> "";

  Loggable() {}

  public abstract L getThis();

  public L on(@Nullable Logger logger) {
    this.logger = logger;
    return getThis();
  }

  public L lvlMatched(@Nullable LogLevel level) {
    this.levelMatched = level;
    return getThis();
  }

  public L lvlSkipped(@Nullable LogLevel level) {
    this.levelSkipped = level;
    return getThis();
  }

  public L lvl(@Nullable LogLevel level) {
    lvlMatched(level);
    lvlSkipped(((level != null) && (level.ordinal() > 0))
        ? LogLevel.values()[level.ordinal() - 1]
        : null);
    return getThis();
  }

  public L msg(@Nullable Supplier<?> msg) {
    this.msg = (msg != null) ? msg : () -> "";
    return getThis();
  }

  public L msg(@Nullable Object msg) {
    return msg(() -> msg);
  }

  protected Logger getLogger() {
    return Optional.ofNullable(logger)
        .orElseGet(() -> Stream.of(Thread.currentThread().getStackTrace())
            .skip(2)
            .map(StackTraceElement::getClassName)
            .filter(not(this.getClass().getName()::equals))
            .map(this::getClassForName)
            .map(LoggerFactory::getLogger)
            .findFirst().orElseThrow(IllegalArgumentException::new));
  }

  private Class<?> getClassForName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

}
