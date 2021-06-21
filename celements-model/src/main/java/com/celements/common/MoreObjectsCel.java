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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Defaults;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Primitives;

public final class MoreObjectsCel {

  private MoreObjectsCel() {}

  @NotNull
  public static <F, T> Optional<T> tryCast(@Nullable F candidate, @NotNull Class<T> targetClass) {
    return targetClass.isInstance(candidate)
        ? Optional.of(targetClass.cast(candidate))
        : Optional.empty();
  }

  /**
   * When the returned {@code Function} is passed as an argument to {@link Stream#flatMap}, the
   * result is a stream of instances of {@code targetClass}.
   *
   * <pre>
   *   Stream<SourceType> streamSource = ...
   *   Stream<TargetType> streamTarget = streamSource.flatMap(tryCast(TargetType.class))
   * </pre>
   */
  @NotNull
  public static <F, T> Function<F, Stream<T>> tryCast(@NotNull Class<T> targetClass) {
    return candidate -> targetClass.isInstance(candidate)
        ? Stream.of(targetClass.cast(candidate))
        : Stream.empty();
  }

  /**
   * @deprecated since 5.2, instead use {@link MoreOptional#toJavaUtil}
   */
  @Deprecated
  @NotNull
  public static <F, T> Function<F, Optional<T>> optToJavaUtil(
      @NotNull Function<F, com.google.common.base.Optional<T>> func) {
    return MoreOptional.toJavaUtil(func);
  }

  /**
   * @deprecated since 5.2, instead use {@link MoreOptional#asNonBlank}
   */
  @Deprecated
  @NotNull
  public static Optional<String> asOptNonBlank(@Nullable String str) {
    return MoreOptional.asNonBlank(str);
  }

  /**
   * @deprecated since 5.2, instead use {@link MoreOptional#findFirstPresent(Supplier...)}
   */
  @Deprecated
  @NotNull
  @SafeVarargs
  public static <T> Optional<T> findFirstPresent(@NotNull Supplier<Optional<T>>... suppliers) {
    return MoreOptional.findFirstPresent(suppliers);
  }

  /**
   * Similar to {@link Defaults#defaultValue(Class)} but also supports {@link String} and commonly
   * non-nullable types, see {@link #defaultValueNonNullable(Class)}
   */
  @Nullable
  public static <T> T defaultValue(@NotNull Class<T> type) {
    return defaultValue(type, false);
  }

  /**
   * Similar to {@link Defaults#defaultValue(Class)} but also supports {@link String} and commonly
   * non-nullable types, see {@link #defaultMutableValueNonNullable(Class)}
   */
  @Nullable
  public static <T> T defaultMutableValue(@NotNull Class<T> type) {
    return defaultValue(type, true);
  }

  @SuppressWarnings("unchecked")
  private static <T> T defaultValue(Class<T> type, boolean mutable) {
    T value = Defaults.defaultValue(Primitives.unwrap(type));
    if (value != null) {
      return value;
    } else if (String.class.equals(type)) {
      return (T) "";
    } else {
      return defaultValueNonNullable(type, mutable);
    }
  }

  /**
   * @return the default immutable value for the following commonly non-nullable types:
   *         List, Set, Queue, Iterable, Map, Stream, Optional
   */
  @Nullable
  public static <T> T defaultValueNonNullable(@NotNull Class<T> type) {
    return defaultValueNonNullable(type, false);
  }

  /**
   * @return the default mutable (if available) value for the following commonly non-nullable types:
   *         List, Set, Queue, Iterable, Map, Stream, Optional
   */
  @Nullable
  public static <T> T defaultMutableValueNonNullable(@NotNull Class<T> type) {
    return defaultValueNonNullable(type, true);
  }

  @SuppressWarnings("unchecked")
  private static <T> T defaultValueNonNullable(Class<T> type, boolean mutable) {
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
      return null;
    }
  }

}
