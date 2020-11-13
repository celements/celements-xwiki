package com.celements.logging;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.util.function.Consumer;

/**
 * a {@link Consumer} wrapper which logs the result of the {@link #delegate} execution.
 */
public class LogConsumer<T> extends Loggable<T, LogConsumer<T>> implements Consumer<T> {

  private final Consumer<T> delegate;

  LogConsumer(Consumer<T> consumer) {
    this.delegate = checkNotNull(consumer);
  }

  @Override
  public LogConsumer<T> getThis() {
    return this;
  }

  @Override
  public void accept(T t) {
    delegate.accept(t);
    log(logger, level, "{}: {}", defer(msg::get), t);
  }

}
