package com.celements.model.classes.fields.list.single;

import javax.validation.constraints.NotNull;

import com.celements.marshalling.Marshaller;
import com.celements.model.classes.fields.list.AbstractListField;
import com.google.common.collect.ImmutableList;

public abstract class SingleListField<T> extends AbstractListField<T, T> {

  public abstract static class Builder<B extends Builder<B, T>, T> extends
      AbstractListField.Builder<B, T, T> {

    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name, marshaller);
    }

  }

  protected SingleListField(@NotNull Builder<?, T> builder) {
    super(builder);
  }

  @Override
  public Class<T> getType() {
    return getMarshaller().getToken();
  }

  @Override
  public Object serialize(T value) {
    return serializeInternal(value != null ? ImmutableList.of(value) : ImmutableList.<T>of());
  }

  @Override
  public T resolve(Object obj) {
    return resolveInternal(obj).first().orNull();
  }

}
