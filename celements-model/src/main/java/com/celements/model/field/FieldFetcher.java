package com.celements.model.field;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.util.Fetchable;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

public class FieldFetcher<O, T> implements Fetchable<T> {

  private final FieldGetterFunction<O, T> fieldGetterFunc;
  private final FluentIterable<O> objects;

  public FieldFetcher(@NotNull FieldAccessor<O> fieldAccessor, @NotNull FluentIterable<O> objects,
      @NotNull ClassField<T> field) {
    fieldGetterFunc = new FieldGetterFunction<>(checkNotNull(fieldAccessor), checkNotNull(field));
    this.objects = checkNotNull(objects);
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

  @Override
  public List<T> list() {
    return iter().toList();
  }

  @Override
  public FluentIterable<T> iter() {
    return objects.transform(fieldGetterFunc).filter(notNull());
  }

}
