package com.celements.common.reflect;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.Constructor;

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
      Constructor<? extends T> constructor = token.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (ReflectiveOperationException exc) {
      throw new IllegalArgumentException(token.getName(), exc);
    }
  }

}
