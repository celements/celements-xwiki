package com.celements.model.classes.fields.ref;

import static com.celements.common.MoreObjectsCel.*;
import static com.google.common.base.Predicates.*;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.marshalling.ReferenceMarshaller;
import com.celements.model.classes.fields.AbstractClassField;
import com.celements.model.classes.fields.CustomClassField;
import com.celements.model.util.ReferenceSerializationMode;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StringClass;

public abstract class ReferenceField<T extends EntityReference> extends AbstractClassField<T>
    implements CustomClassField<T> {

  private final Integer size;
  private final ReferenceMarshaller<T> marshaller;

  public abstract static class Builder<B extends Builder<B, T>, T extends EntityReference> extends
      AbstractClassField.Builder<B, T> {

    private ReferenceSerializationMode serializationMode;
    private EntityReference baseRef;
    private Integer size;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name);
    }

    public B refSerializationMode(@Nullable ReferenceSerializationMode serializationMode) {
      this.serializationMode = serializationMode;
      return getThis();
    }

    public B baseRef(EntityReference baseRef) {
      this.baseRef = baseRef;
      return getThis();
    }

    public B size(@Nullable Integer val) {
      size = val;
      return getThis();
    }

  }

  protected ReferenceField(@NotNull Builder<?, T> builder) {
    super(builder);
    this.size = builder.size;
    marshaller = new ReferenceMarshaller.Builder<>(getType()).serializationMode(
        builder.serializationMode).baseRef(builder.baseRef).build();
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
