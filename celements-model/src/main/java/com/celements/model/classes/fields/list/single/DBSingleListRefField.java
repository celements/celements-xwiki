package com.celements.model.classes.fields.list.single;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.marshalling.ReferenceMarshaller;

@Immutable
public final class DBSingleListRefField<T extends EntityReference> extends
    CustomDBSingleListField<T> {

  public final static class Builder<T extends EntityReference> extends
      CustomDBSingleListField.Builder<Builder<T>, T> {

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
    public DBSingleListRefField<T> build() {
      return new DBSingleListRefField<>(getThis());
    }

  }

  protected DBSingleListRefField(@NotNull Builder<T> builder) {
    super(builder);
  }

}
