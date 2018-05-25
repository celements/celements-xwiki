package com.celements.marshalling;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.EntityReference;

import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.google.common.base.Optional;
import com.xpn.xwiki.web.Utils;

@Immutable
public final class ReferenceMarshaller<T extends EntityReference> extends AbstractMarshaller<T> {

  private final ReferenceSerializationMode mode;

  public ReferenceMarshaller(@NotNull Class<T> token) {
    this(token, null);
  }

  public ReferenceMarshaller(@NotNull Class<T> token, @Nullable ReferenceSerializationMode mode) {
    super(token);
    this.mode = firstNonNull(mode, ReferenceSerializationMode.GLOBAL);
  }

  @Override
  public String serialize(T val) {
    return getModelUtils().serializeRef(val, mode);
  }

  @Override
  public Optional<T> resolve(String val) {
    checkNotNull(val);
    T reference = null;
    try {
      if (getToken() == EntityReference.class) {
        reference = getToken().cast(getModelUtils().resolveRef(val));
      } else {
        reference = getModelUtils().resolveRef(val, getToken());
      }
    } catch (IllegalArgumentException exc) {
      LOGGER.info("failed to resolve '{}' for '{}'", val, getToken(), exc);
    }
    return Optional.fromNullable(reference);
  }

  protected static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
