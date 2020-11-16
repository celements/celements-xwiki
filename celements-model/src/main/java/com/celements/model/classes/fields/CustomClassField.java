package com.celements.model.classes.fields;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public interface CustomClassField<T> extends ClassField<T> {

  @NotNull
  Optional<?> serialize(@Nullable T value);

  @NotNull
  Optional<T> resolve(@Nullable Object obj);

}
