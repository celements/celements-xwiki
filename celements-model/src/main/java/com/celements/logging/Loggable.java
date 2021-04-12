package com.celements.logging;

import static com.celements.logging.LogUtils.*;

import java.util.function.Supplier;

import org.slf4j.Logger;

public abstract class Loggable<T, L extends Loggable<T, L>> {

  protected Logger logger;
  protected LogLevel level = LogLevel.DEBUG;
  protected LogLevel levelReduced = LogLevel.TRACE;
  protected Supplier<?> msg = () -> "";

  Loggable() {}

  public abstract L getThis();

  public L trace(Logger logger) {
    return on(logger).lvl(LogLevel.TRACE);
  }

  public L debug(Logger logger) {
    return on(logger).lvl(LogLevel.DEBUG);
  }

  public L info(Logger logger) {
    return on(logger).lvl(LogLevel.INFO);
  }

  public L warn(Logger logger) {
    return on(logger).lvl(LogLevel.WARN);
  }

  public L error(Logger logger) {
    return on(logger).lvl(LogLevel.ERROR);
  }

  public L on(Logger logger) {
    this.logger = logger;
    return getThis();
  }

  public L lvlDefault(LogLevel level) {
    this.level = level;
    return getThis();
  }

  public L lvlReduced(LogLevel level) {
    this.levelReduced = level;
    return getThis();
  }

  public L lvl(LogLevel level) {
    lvlDefault(level);
    lvlReduced(((level != null) && (level.ordinal() > 0))
        ? LogLevel.values()[level.ordinal() - 1]
        : null);
    return getThis();
  }

  public L msg(Supplier<?> msg) {
    this.msg = (msg != null) ? msg : () -> "";
    return getThis();
  }

  public L msg(Object msg) {
    return msg(() -> msg);
  }

  public L msg(Object msg, Object... args) {
    return msg(format(msg, args));
  }

}
