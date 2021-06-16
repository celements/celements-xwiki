package com.celements.common;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;

/**
 * @deprecated since 5.2, instead use {@link com.google.common.collect.BiMap};
 */
@Deprecated
public class ReverseMap<K extends ValueGetter<V>, V> {

  private final Map<V, K> reverseMap = new HashMap<>();

  public ReverseMap(@NotNull K[] keys) {
    for (K theKey : keys) {
      reverseMap.put(theKey.getValue(), theKey);
    }
  }

  @NotNull
  public Optional<K> get(@Nullable V value) {
    return Optional.fromNullable(reverseMap.get(value));
  }

}
