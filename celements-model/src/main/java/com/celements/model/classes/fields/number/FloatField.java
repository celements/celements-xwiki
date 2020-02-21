package com.celements.model.classes.fields.number;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

@Immutable
public final class FloatField extends NumberField<Float> {

  public static class Builder extends NumberField.Builder<Builder, Float> {

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
    public FloatField build() {
      return new FloatField(getThis());
    }

  }

  protected FloatField(@NotNull Builder builder) {
    super(builder);
  }

  @Override
  public Class<Float> getType() {
    return Float.class;
  }

}
