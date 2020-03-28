package com.celements.model.classes.fields.ref;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ClassReference;

@Immutable
public final class AttachmentReferenceField extends ReferenceField<AttachmentReference> {

  public static class Builder extends ReferenceField.Builder<Builder, AttachmentReference> {

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
    public AttachmentReferenceField build() {
      return new AttachmentReferenceField(getThis());
    }

  }

  protected AttachmentReferenceField(@NotNull Builder builder) {
    super(builder);
  }

  @Override
  public Class<AttachmentReference> getType() {
    return AttachmentReference.class;
  }

}
