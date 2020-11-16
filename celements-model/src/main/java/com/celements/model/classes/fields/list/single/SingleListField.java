package com.celements.model.classes.fields.list.single;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.Marshaller;
import com.celements.model.classes.fields.list.AbstractListField;
import com.google.common.collect.ImmutableList;

public abstract class SingleListField<T> extends AbstractListField<T, T> {

  public abstract static class Builder<B extends Builder<B, T>, T> extends
      AbstractListField.Builder<B, T, T> {

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name, marshaller);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classRef, name, marshaller);
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
  public Optional<String> serialize(T value) {
    return Optional.ofNullable(value)
        .map(ImmutableList::of)
        .flatMap(this::serializeInternal);
  }

  @Override
  public Optional<T> resolve(Object obj) {
    return resolveInternal(obj).findFirst();
  }

}
