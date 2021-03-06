package com.celements.model.classes.fields.ref;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;

@Immutable
public final class DocumentReferenceField extends ReferenceField<DocumentReference> {

  public static class Builder extends ReferenceField.Builder<Builder, DocumentReference> {

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
    public DocumentReferenceField build() {
      return new DocumentReferenceField(getThis());
    }

  }

  protected DocumentReferenceField(@NotNull Builder builder) {
    super(builder);
  }

  @Override
  public Class<DocumentReference> getType() {
    return DocumentReference.class;
  }

}
