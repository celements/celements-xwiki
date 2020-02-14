package com.celements.model.classes.fields.list.single;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.DefaultMarshaller;

@Immutable
public final class DBSingleListField extends CustomDBSingleListField<String> {

  public final static class Builder extends CustomDBSingleListField.Builder<Builder, String> {

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name, new DefaultMarshaller());
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name, new DefaultMarshaller());
    }

    @Override
    public Builder getThis() {
      return this;
    }

    @Override
    public DBSingleListField build() {
      return new DBSingleListField(getThis());
    }

  }

  protected DBSingleListField(@NotNull Builder builder) {
    super(builder);
  }

}
