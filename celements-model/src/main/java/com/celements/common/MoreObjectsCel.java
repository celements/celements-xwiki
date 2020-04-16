package com.celements.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.base.Defaults;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class MoreObjectsCel {

  private MoreObjectsCel() {}

  public static <F, T> Optional<T> tryCast(F candidate, Class<T> targetClass) {
    return targetClass.isInstance(candidate)
        ? Optional.of(targetClass.cast(candidate))
        : Optional.empty();
  }

  public static <T> T defaultValue(Class<T> type) {
    return defaultValue(type, false);
  }

  public static <T> T defaultMutableValue(Class<T> type) {
    return defaultValue(type, true);
  }

  @SuppressWarnings("unchecked")
  private static <T> T defaultValue(Class<T> type, boolean mutable) {
    if (Iterable.class.equals(type) || Collection.class.equals(type)
        || List.class.equals(type)) {
      return (T) (mutable ? new ArrayList<>() : ImmutableList.of());
    } else if (Set.class.equals(type)) {
      return (T) (mutable ? new HashSet<>() : ImmutableSet.of());
    } else if (Map.class.equals(type)) {
      return (T) (mutable ? new HashMap<>() : ImmutableMap.of());
    } else if (Stream.class.equals(type)) {
      return (T) Stream.empty();
    } else if (FluentIterable.class.equals(type)) {
      return (T) FluentIterable.of();
    } else if (Optional.class.equals(type)) {
      return (T) Optional.empty();
    } else if (com.google.common.base.Optional.class.equals(type)) {
      return (T) com.google.common.base.Optional.absent();
    } else {
      return Defaults.defaultValue(type);
    }
  }

}
