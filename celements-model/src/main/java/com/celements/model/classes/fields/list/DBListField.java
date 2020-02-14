package com.celements.model.classes.fields.list;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.DefaultMarshaller;

@Immutable
public final class DBListField extends CustomDBListField<String> {

  public final static class Builder extends CustomDBListField.Builder<Builder, String> {

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
    public DBListField build() {
      return new DBListField(getThis());
    }

  }

  protected DBListField(@NotNull Builder builder) {
    super(builder);
  }

}
