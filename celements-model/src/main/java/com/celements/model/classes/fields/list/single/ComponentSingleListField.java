package com.celements.model.classes.fields.list.single;

import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.ComponentMarshaller;
import com.xpn.xwiki.web.Utils;

@Immutable
public final class ComponentSingleListField<T> extends CustomSingleListField<T> {

  public static class Builder<T> extends CustomSingleListField.Builder<Builder<T>, T> {

    public Builder(@NotNull ClassReference classRef, @NotNull String name, @NotNull Class<T> role) {
      super(classRef, name, new ComponentMarshaller<>(role));
    }

    @Override
    public Builder<T> getThis() {
      return this;
    }

    @Override
    public ComponentSingleListField<T> build() {
      return new ComponentSingleListField<>(getThis());
    }

  }

  protected ComponentSingleListField(@NotNull Builder<T> builder) {
    super(builder);
  }

  @Override
  public List<T> getValues() {
    // override to lookup components lazily, otherwise breaks static definitions of ClassFields
    return Utils.getComponentList(getMarshaller().getToken());
  }

}
