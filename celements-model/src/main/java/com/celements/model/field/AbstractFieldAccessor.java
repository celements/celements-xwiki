package com.celements.model.field;

import com.celements.model.classes.fields.ClassField;

public abstract class AbstractFieldAccessor<T> implements FieldAccessor<T> {

  @Override
  @Deprecated
  public <V> com.google.common.base.Optional<V> getValue(T instance, ClassField<V> field) {
    return com.google.common.base.Optional.fromJavaUtil(get(instance, field));
  }

  @Override
  @Deprecated
  public <V> boolean setValue(T instance, ClassField<V> field, V value) {
    return set(instance, field, value);
  }

}
