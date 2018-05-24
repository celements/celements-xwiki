package com.celements.model.classes.fields;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.marshalling.Marshaller;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class CustomStringField<T> extends AbstractClassField<T> implements CustomClassField<T> {

  private final Marshaller<T> marshaller;
  private final Integer size;

  public static class Builder<B extends Builder<B, T>, T> extends AbstractClassField.Builder<B, T> {

    private final Marshaller<T> marshaller;
    private Integer size;

    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name);
      this.marshaller = marshaller;
    }

    @Override
    @SuppressWarnings("unchecked")
    public B getThis() {
      return (B) this;
    }

    public B size(@Nullable Integer val) {
      size = val;
      return getThis();
    }

    @Override
    public CustomStringField<T> build() {
      return new CustomStringField<>(getThis());
    }

  }

  protected CustomStringField(@NotNull Builder<?, T> builder) {
    super(builder);
    this.size = builder.size;
    this.marshaller = builder.marshaller;
  }

  @Override
  public Class<T> getType() {
    return marshaller.getToken();
  }

  public Integer getSize() {
    return size;
  }

  @Override
  protected PropertyClass getPropertyClass() {
    StringClass element = new StringClass();
    if (size != null) {
      element.setSize(size);
    }
    return element;
  }

  @Override
  public Object serialize(T value) {
    Object ret = null;
    if (value != null) {
      ret = marshaller.serialize(value);
    }
    return ret;
  }

  @Override
  public T resolve(Object obj) {
    T ret = null;
    if (obj != null) {
      ret = marshaller.resolve(obj.toString()).orNull();
    }
    return ret;
  }

}
