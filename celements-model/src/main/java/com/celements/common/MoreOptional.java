package com.celements.common;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public final class MoreOptional {

  private MoreOptional() {}

  /**
   * Converts an Optional to a Stream, useful for flatmapping a stream of optionals.
   *
   * <pre>
   *   Stream<Optional<T>> streamOfOpts = ...
   *   Stream<T> stream = streamOfOpts.flatMap(MoreOptional::stream)
   * </pre>
   *
   * Can be replaced with {@link Optional#stream()} from Java 9+.
   */
  @NotNull
  public static <T> Stream<T> stream(@NotNull Optional<T> opt) {
    return opt.isPresent() ? Stream.of(opt.get()) : Stream.empty();
  }

  /**
   * Allows fore more concise guava to java util optional conversion in lambdas. Usage sample:
   *
   * <pre>
   * stream.map(toJavaUtil(GuavaStuff::returningGuavaOptional))
   * </pre>
   */
  @NotNull
  public static <F, T> Function<F, Optional<T>> toJavaUtil(
      @NotNull Function<F, com.google.common.base.Optional<T>> func) {
    return x -> func.apply(x).toJavaUtil();
  }

  @NotNull
  public static Optional<String> asNonBlank(@Nullable String str) {
    return ((str == null) || str.trim().isEmpty()) ? Optional.empty() : Optional.of(str);
  }

  /**
   * @return the first present result (if any) from a stream of suppliers which supply optionals
   */
  @NotNull
  public static <T> Optional<T> findFirstPresent(@NotNull Stream<Supplier<Optional<T>>> suppliers) {
    return suppliers.map(Supplier::get)
        .flatMap(MoreOptional::stream)
        .findFirst();
  }

  /**
   * @return the first present result (if any) from a varargs of suppliers which supply optionals
   */
  @SafeVarargs
  @NotNull
  public static <T> Optional<T> findFirstPresent(@NotNull Supplier<Optional<T>>... suppliers) {
    return findFirstPresent(Stream.of(suppliers));
  }

}
