package com.celements.model.classes.fields.list.single;

import java.util.Arrays;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.celements.marshalling.EnumMarshaller;

@Immutable
public final class EnumSingleListField<E extends Enum<E>> extends CustomSingleListField<E> {

  public static class Builder<E extends Enum<E>> extends
      CustomSingleListField.Builder<Builder<E>, E> {

    public Builder(@NotNull String classDefName, @NotNull String name, @NotNull Class<E> enumType) {
      this(classDefName, name, new EnumMarshaller<>(enumType));
    }

    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull EnumMarshaller<E> marshaller) {
      super(classDefName, name, marshaller);
      values(Arrays.asList(marshaller.getToken().getEnumConstants()));
    }

    @Override
    public Builder<E> getThis() {
      return this;
    }

    @Override
    public EnumSingleListField<E> build() {
      return new EnumSingleListField<>(getThis());
    }

  }

  protected EnumSingleListField(@NotNull Builder<E> builder) {
    super(builder);
  }

}
