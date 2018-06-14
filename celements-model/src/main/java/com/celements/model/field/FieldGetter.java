package com.celements.model.field;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.ObjectFetcher;
import com.celements.model.util.Fetchable;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

public class FieldGetter<O, T> implements Fetchable<T> {

  private final FieldAccessor<O> fieldAccessor;
  private final ObjectFetcher<?, O> objFetcher;
  private final ClassField<T> field;

  public FieldGetter(@NotNull FieldAccessor<O> fieldAccessor,
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

  @Override
  public boolean exists() {
    return count() > 0;
  }

  @Override
  public int count() {
    return iter().size();
  }

  @Override
  public Optional<T> first() {
    return iter().first();
  }

  public @NotNull Optional<T> fromFirstObject() {
    Optional<O> obj = objFetcher.first();
    return obj.isPresent() ? fieldAccessor.getValue(obj.get(), field) : Optional.<T>absent();
  }

  @Override
  public List<T> list() {
    return iter().toList();
  }

  @Override
  public FluentIterable<T> iter() {
    return iterNullable().filter(notNull());
  }

  public @NotNull FluentIterable<T> iterNullable() {
    return objFetcher.iter().transform(new FieldGetterFunction<>(fieldAccessor, field));
  }

}
