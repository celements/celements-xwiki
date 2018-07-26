package com.celements.common;

import static com.google.common.base.Preconditions.*;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.EntityReference;

import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.google.common.base.Function;
import com.xpn.xwiki.web.Utils;

public class MoreFunctions {

  private MoreFunctions() {
  }

  public static Function<Object, Integer> hashCodeFunction() {
    return HASHCODE_FUNCTION;
  }

  private static final Function<Object, Integer> HASHCODE_FUNCTION = new Function<Object, Integer>() {

    @Override
    public Integer apply(Object o) {
      return checkNotNull(o).hashCode();
    }
  };

  public static <T extends EntityReference> Function<T, String> serializeRefFunction(
      @NotNull final ReferenceSerializationMode mode) {
    return new Function<T, String>() {

      @Override
      public String apply(T reference) {
        return getModelUtils().serializeRef(reference, mode);
      }
    };
  }

  public static <T extends EntityReference> Function<String, T> resolveRefFunction(
      final Class<T> token) {
    return new Function<String, T>() {

      @Override
      public T apply(String name) {
        return getModelUtils().resolveRef(name, token);
      }
    };
  }

  private static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
