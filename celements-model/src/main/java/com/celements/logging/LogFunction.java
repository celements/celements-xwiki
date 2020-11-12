package com.celements.logging;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.util.function.Function;

/**
 * a {@link Function} wrapper which logs the result of the {@link #delegate} execution.
 */
public class LogFunction<T, R> extends Loggable<T, LogFunction<T, R>> implements Function<T, R> {

  private final Function<T, R> delegate;

  LogFunction(Function<T, R> function) {
    this.delegate = checkNotNull(function);
  }

  @Override
  public LogFunction<T, R> getThis() {
    return this;
  }

  @Override
  public R apply(T t) {
    R ret = delegate.apply(t);
    log(logger, ((ret != null) ? level : levelReduced),
        "{}: [{}] -> [{}]", defer(msg::get), t, ret);
    return ret;
  }

}
