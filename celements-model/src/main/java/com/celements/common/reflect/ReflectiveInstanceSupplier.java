package com.celements.common.reflect;

import static com.google.common.base.Preconditions.*;

import javax.validation.constraints.NotNull;

import com.google.common.base.Supplier;

public class ReflectiveInstanceSupplier<T> implements Supplier<T> {

  private final Class<T> token;

  public ReflectiveInstanceSupplier(@NotNull Class<T> token) {
    this.token = checkNotNull(token);
  }

  @Override
  public T get() {
    try {
      return token.getConstructor().newInstance();
    } catch (ReflectiveOperationException exc) {
      throw new IllegalArgumentException(token.getName(), exc);
    }
  }

}
