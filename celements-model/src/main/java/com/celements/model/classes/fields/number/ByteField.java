package com.celements.model.classes.fields.number;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

@Immutable
public final class ByteField extends NumberField<Byte> {

  public static class Builder extends NumberField.Builder<Builder, Byte> {

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name);
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public ByteField build() {
      return new ByteField(getThis());
    }

  }

  protected ByteField(@NotNull Builder builder) {
    super(builder);
  }

  @Override
  public Class<Byte> getType() {
    return Byte.class;
  }

}
