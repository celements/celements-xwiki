package com.celements.model.classes.fields.list;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.marshalling.ReferenceMarshaller;

public final class DBListRefField<T extends EntityReference> extends CustomDBListField<T> {

  public final static class Builder<T extends EntityReference> extends
      CustomDBListField.Builder<Builder<T>, T> {

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull ReferenceMarshaller<T> marshaller) {
      super(classDefName, name, marshaller);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull ReferenceMarshaller<T> marshaller) {
      super(classRef, name, marshaller);
    }

    @Override
    public Builder<T> getThis() {
      return this;
    }

    @Override
    public DBListRefField<T> build() {
      return new DBListRefField<>(getThis());
    }

  }

  protected DBListRefField(@NotNull Builder<T> builder) {
    super(builder);
  }

}
