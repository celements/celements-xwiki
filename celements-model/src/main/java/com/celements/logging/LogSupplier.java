package com.celements.logging;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.util.function.Supplier;

/**
 * a {@link Supplier} wrapper which logs the result of the {@link #delegate} execution.
 */
public class LogSupplier<T> extends Loggable<T, LogSupplier<T>> implements Supplier<T> {

  private final Supplier<T> delegate;

  LogSupplier(Supplier<T> supplier) {
    this.delegate = checkNotNull(supplier);
  }

  @Override
  public LogSupplier<T> getThis() {
    return this;
  }

  @Override
  public T get() {
    T t = delegate.get();
    log(logger, level, "{}: {}", defer(msg::get), t);
    return t;
  }

}
