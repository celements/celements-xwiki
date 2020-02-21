package com.celements.model.classes.fields.ref;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

@Immutable
public final class ClassReferenceField extends ReferenceField<ClassReference> {

  public static class Builder extends ReferenceField.Builder<Builder, ClassReference> {

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
    public ClassReferenceField build() {
      return new ClassReferenceField(getThis());
    }

  }

  protected ClassReferenceField(@NotNull Builder builder) {
    super(builder);
  }

  @Override
  public Class<ClassReference> getType() {
    return ClassReference.class;
  }

}
