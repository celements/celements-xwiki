package com.celements.logging;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.util.function.Predicate;

import org.slf4j.Logger;

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
    Logger log = getLogger();
    if (ret && isLevelEnabled(log, levelMatched)) {
      log(log, levelMatched, "{}: [{}]", defer(msg::get), t);
    } else if (!ret && isLevelEnabled(log, levelSkipped)) {
      log(log, levelSkipped, "{}: skipped [{}]", defer(msg::get), t);
    }
    return ret;
  }

}
