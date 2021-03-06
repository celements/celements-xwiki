package com.celements.model.classes.fields.ref;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.EntityReference;

@Immutable
public final class EntityReferenceField extends ReferenceField<EntityReference> {

  public static class Builder extends ReferenceField.Builder<Builder, EntityReference> {

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
    public EntityReferenceField build() {
      return new EntityReferenceField(getThis());
    }

  }

  protected EntityReferenceField(@NotNull Builder builder) {
    super(builder);
  }

  @Override
  public Class<EntityReference> getType() {
    return EntityReference.class;
  }

}
