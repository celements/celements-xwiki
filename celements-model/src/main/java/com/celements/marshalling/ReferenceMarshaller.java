package com.celements.marshalling;

import static com.celements.model.util.ReferenceSerializationMode.*;
import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.EntityReference;

import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.celements.model.util.References;
import com.google.common.base.Optional;
import com.xpn.xwiki.web.Utils;

@Immutable
public final class ReferenceMarshaller<T extends EntityReference> extends AbstractMarshaller<T> {

  private final ReferenceSerializationMode serializationMode;
  private final EntityReference baseRef;

  public static class Builder<T extends EntityReference> {

    private final Class<T> token;
    private ReferenceSerializationMode serializationMode;
    private EntityReference baseRef;

    public Builder(@NotNull Class<T> token) {
      this.token = token;
    }

    @NotNull
    public Builder<T> serializationMode(@Nullable ReferenceSerializationMode serializationMode) {
      this.serializationMode = serializationMode;
      return this;
    }

    @NotNull
    public Builder<T> baseRef(@Nullable EntityReference baseRef) {
      this.baseRef = baseRef;
      return this;
    }

    @NotNull
    public ReferenceMarshaller<T> build() {
      return new ReferenceMarshaller<>(this);
    }

  }

  public ReferenceMarshaller(@NotNull Class<T> token) {
    this(new Builder<>(token));
  }

  private ReferenceMarshaller(Builder<T> builder) {
    super(builder.token);
    serializationMode = firstNonNull(builder.serializationMode, GLOBAL);
    baseRef = (builder.baseRef != null) ? References.cloneRef(builder.baseRef) : null;
  }

  private EntityReference getBaseRef() {
    if (baseRef != null) {
      return References.cloneRef(baseRef);
    }
    return null;
  }

  @Override
  public String serialize(T val) {
    EntityReference toSerialize = val;
    if ((baseRef != null) && (serializationMode != GLOBAL)) {
      // only the relative path to the baseRef should be serialized
      RefBuilder builder = new RefBuilder();
      EntityReference ref = val;
      while ((ref != null) && (ref.getType() != baseRef.getType())) {
        builder.with(ref.getType(), ref.getName());
        ref = ref.getParent();
      }
      if (builder.depth() > 0) {
        toSerialize = builder.buildRelative();
      }
    }
    return getModelUtils().serializeRef(toSerialize, serializationMode);
  }

  @Override
  public Optional<T> resolve(String val) {
    checkNotNull(val);
    T reference = null;
    try {
      if (getToken() == EntityReference.class) {
        reference = getToken().cast(getModelUtils().resolveRef(val, getBaseRef()));
      } else {
        reference = getModelUtils().resolveRef(val, getToken(), getBaseRef());
      }
    } catch (IllegalArgumentException exc) {
      LOGGER.info("failed to resolve '{}' for '{}'", val, getToken(), exc);
    }
    return Optional.fromNullable(reference);
  }

  protected static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  protected static ModelContext getContext() {
    return Utils.getComponent(ModelContext.class);
  }

}
