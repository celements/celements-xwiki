package com.celements.model.field;

import static com.google.common.base.Preconditions.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.ObjectFetcher;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

public class FieldSetter<O, T> {

  private final FieldAccessor<O> fieldAccessor;
  private final ObjectFetcher<?, O> objFetcher;
  private final ClassField<T> field;

  public FieldSetter(@NotNull FieldAccessor<O> fieldAccessor,
      @NotNull ObjectFetcher<?, O> objFetcher, @NotNull ClassField<T> field) {
    this.fieldAccessor = checkNotNull(fieldAccessor);
    this.objFetcher = checkNotNull(objFetcher);
    this.field = checkNotNull(field);
  }

  public FluentIterable<O> getObjects() {
    return objFetcher.iter();
  }

  public ClassField<T> getField() {
    return field;
  }

  public boolean first(@Nullable T value) {
    Optional<O> obj = objFetcher.first();
    if (obj.isPresent()) {
      return fieldAccessor.setValue(obj.get(), field, value);
    }
    return false;
  }

  public boolean all(@Nullable T value) {
    boolean changed = false;
    for (O obj : objFetcher.iter()) {
      changed |= fieldAccessor.setValue(obj, field, value);
    }
    return changed;
  }

}
