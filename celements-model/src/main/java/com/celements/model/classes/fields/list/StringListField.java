package com.celements.model.classes.fields.list;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.DefaultMarshaller;

@Immutable
public class StringListField extends CustomListField<String> {

  public static class Builder<B extends Builder<B>> extends CustomListField.Builder<B, String> {

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name, new DefaultMarshaller());
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name, new DefaultMarshaller());
    }

    @Override
    public StringListField build() {
      return new StringListField(this);
    }

  }

  protected StringListField(@NotNull Builder<?> builder) {
    super(builder);
  }

}
