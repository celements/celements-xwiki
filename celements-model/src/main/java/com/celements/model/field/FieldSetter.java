package com.celements.model.field;

import static com.google.common.base.Preconditions.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

public class FieldSetter<O, T> {

  private final FieldAccessor<O> fieldAccessor;
  private final FluentIterable<O> objects;
  private final ClassField<T> field;

  public FieldSetter(@NotNull FieldAccessor<O> fieldAccessor, @NotNull FluentIterable<O> objects,
      @NotNull ClassField<T> field) {
    this.fieldAccessor = checkNotNull(fieldAccessor);
    this.objects = checkNotNull(objects);
    this.field = checkNotNull(field);
  }

  public boolean setFirst(@Nullable T value) {
    Optional<O> obj = objects.first();
    if (obj.isPresent()) {
      return fieldAccessor.setValue(obj.get(), field, value);
    }
    return false;
  }

  public boolean setAll(@Nullable T value) {
    boolean changed = false;
    for (O obj : objects) {
      changed |= fieldAccessor.setValue(obj, field, value);
    }
    return changed;
  }

}
