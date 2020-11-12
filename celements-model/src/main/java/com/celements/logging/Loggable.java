package com.celements.logging;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.slf4j.Logger;

public abstract class Loggable<T, L extends Loggable<T, L>> {

  protected Logger logger;
  protected LogLevel level = LogLevel.DEBUG;
  protected LogLevel levelReduced = LogLevel.TRACE;
  protected Supplier<?> msg = () -> "";

  Loggable() {}

  public abstract L getThis();

  public L on(@Nullable Logger logger) {
    this.logger = logger;
    return getThis();
  }

  public L lvlDefault(@Nullable LogLevel level) {
    this.level = level;
    return getThis();
  }

  public L lvlReduced(@Nullable LogLevel level) {
    this.levelReduced = level;
    return getThis();
  }

  public L lvl(@Nullable LogLevel level) {
    lvlDefault(level);
    lvlReduced(((level != null) && (level.ordinal() > 0))
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

}
