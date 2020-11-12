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

  @Override
  public boolean test(T t) {
    boolean ret = delegate.test(t);
    if (ret && isLevelEnabled(logger, levelMatched)) {
      log(logger, levelMatched, "{}: [{}]", defer(msg::get), t);
    } else if (!ret && isLevelEnabled(logger, levelSkipped)) {
      log(logger, levelSkipped, "{}: filtered [{}]", defer(msg::get), t);
    }
    return ret;
  }

}
