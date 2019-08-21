package com.celements.common;

import static com.google.common.base.Preconditions.*;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.EntityReference;

import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.google.common.base.Function;
import com.xpn.xwiki.web.Utils;

public final class MoreFunctions {

  private MoreFunctions() {}

  public static Function<Object, Integer> hashCodeFunction() {
    return HASHCODE_FUNCTION;
  }

  private static final Function<Object, Integer> HASHCODE_FUNCTION = o -> checkNotNull(o)
      .hashCode();

  public static <T extends EntityReference> Function<T, String> serializeRefFunction(
      @NotNull final ReferenceSerializationMode mode) {
    return reference -> getModelUtils().serializeRef(reference, mode);
  }

  public static <T extends EntityReference> Function<String, T> resolveRefFunction(
      final Class<T> token) {
    return name -> getModelUtils().resolveRef(name, token);
  }

  private static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
