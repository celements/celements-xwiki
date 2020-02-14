package com.celements.model.classes.fields.list;

import java.util.Arrays;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.EnumMarshaller;

@Immutable
public class EnumListField<E extends Enum<E>> extends CustomListField<E> {

  public static class Builder<E extends Enum<E>> extends CustomListField.Builder<Builder<E>, E> {

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name, @NotNull Class<E> enumType) {
      this(classDefName, name, new EnumMarshaller<>(enumType));
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull Class<E> enumType) {
      this(classRef, name, new EnumMarshaller<>(enumType));
    }

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull EnumMarshaller<E> marshaller) {
      super(classDefName, name, marshaller);
      values(Arrays.asList(marshaller.getToken().getEnumConstants()));
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull EnumMarshaller<E> marshaller) {
      super(classRef, name, marshaller);
      values(Arrays.asList(marshaller.getToken().getEnumConstants()));
    }

    @Override
    public Builder<E> getThis() {
      return this;
    }

    @Override
    public EnumListField<E> build() {
      return new EnumListField<>(getThis());
    }

  }

  protected EnumListField(@NotNull Builder<E> builder) {
    super(builder);
  }

}
