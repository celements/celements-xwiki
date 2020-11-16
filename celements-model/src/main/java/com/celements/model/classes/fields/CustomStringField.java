package com.celements.model.classes.fields;

import static com.celements.common.MoreObjectsCel.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.Marshaller;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StringClass;

public class CustomStringField<T> extends AbstractClassField<T> implements CustomClassField<T> {

  private final Marshaller<T> marshaller;
  private final Integer size;

  public static class Builder<B extends Builder<B, T>, T> extends AbstractClassField.Builder<B, T> {

    private final Marshaller<T> marshaller;
    private Integer size;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name);
      this.marshaller = checkNotNull(marshaller);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classRef, name);
      this.marshaller = checkNotNull(marshaller);
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
  public Optional<String> serialize(T value) {
    return Optional.ofNullable(value)
        .map(marshaller::serialize)
        .filter(not(String::isEmpty));
  }

  @Override
  public Optional<T> resolve(Object obj) {
    return Optional.ofNullable(obj)
        .map(Object::toString)
        .flatMap(optToJavaUtil(marshaller::resolve));
  }

}
