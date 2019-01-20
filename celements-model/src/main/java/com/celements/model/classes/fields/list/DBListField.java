package com.celements.model.classes.fields.list;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.celements.marshalling.DefaultMarshaller;

@Immutable
public final class DBListField extends CustomDBListField<String> {

  public final static class Builder extends CustomDBListField.Builder<Builder, String> {

    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name, new DefaultMarshaller());
    }

    @Override
    public Builder getThis() {
      return this;
    }

    /**
     * @deprecated since 3.15, deprecated override needed for backwards compatibility with older
     *             version to avoid NoSuchMethodError
     */
    @Deprecated
    @Override
    public Builder sql(@Nullable String val) {
      return super.sql(val);
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
