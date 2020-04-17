package com.celements.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.base.Defaults;
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
    if (List.class.isAssignableFrom(type)) {
      return (T) (mutable ? new ArrayList<>() : ImmutableList.of());
    } else if (Set.class.isAssignableFrom(type)) {
      return (T) (mutable ? new LinkedHashSet<>() : ImmutableSet.of());
    } else if (Queue.class.isAssignableFrom(type)) {
      return (T) new LinkedList<>();
    } else if (Iterable.class.isAssignableFrom(type)) {
      return (T) defaultValue(List.class, mutable);
    } else if (Map.class.isAssignableFrom(type)) {
      return (T) (mutable ? new LinkedHashMap<>() : ImmutableMap.of());
    } else if (Stream.class.isAssignableFrom(type)) {
      return (T) Stream.empty();
    } else if (Optional.class.isAssignableFrom(type)) {
      return (T) Optional.empty();
    } else if (com.google.common.base.Optional.class.isAssignableFrom(type)) {
      return (T) com.google.common.base.Optional.absent();
    } else {
      return Defaults.defaultValue(type);
    }
  }

}
