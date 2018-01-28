package com.celements.model.classes.fields.list.single;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.celements.marshalling.DefaultMarshaller;

@Immutable
public final class StringSingleListField extends CustomSingleListField<String> {

  public static class Builder extends CustomSingleListField.Builder<Builder, String> {

    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name, new DefaultMarshaller());
    }

    @Override
    public StringSingleListField build() {
      return new StringSingleListField(this);
    }

  }

  protected StringSingleListField(@NotNull Builder builder) {
    super(builder);
  }

}
