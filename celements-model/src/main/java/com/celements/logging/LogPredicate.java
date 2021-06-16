package com.celements.logging;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.util.function.Predicate;

/**
 * a {@link Predicate} wrapper which logs the result of the {@link #delegate} execution.
 */
public class LogPredicate<T> extends Loggable<T, LogPredicate<T>> implements Predicate<T> {

  private final Predicate<T> delegate;

  LogPredicate(Predicate<T> predicate) {
    this.delegate = checkNotNull(predicate);
  }

  @Override
  public LogPredicate<T> getThis() {
    return this;
  }

  public LogPredicate<T> lvlPass(LogLevel level) {
    return lvlDefault(level);
  }

  public LogPredicate<T> lvlFail(LogLevel level) {
    return lvlReduced(level);
  }

  @Override
  public boolean test(T t) {
    boolean ret = delegate.test(t);
    if (ret) {
      log(logger, level, "{}: {}", defer(msg::get), t);
    } else {
      log(logger, levelReduced, "{}: filtered {}", defer(msg::get), t);
    }
    return ret;
  }

}
