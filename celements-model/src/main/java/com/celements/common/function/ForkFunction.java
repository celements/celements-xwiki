package com.celements.common.function;

import static com.celements.common.MoreFunctions.*;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.celements.common.MoreOptional;

/**
 * A specialized composite function that facilitates forked execution. Consists of a predicate
 * 'when' and the two functions 'then' and 'else'. If 'when' evaluates to true for an element,
 * 'then' is executed for it, otherwise 'else'.
 *
 * <pre>
 * stream.map(new ForkFunction<F, T>()
 *     .when(x -> test(x))
 *     .thenMap(x -> mapAccept(x))
 *     .elseMap(x -> mapFail(x)))
 * </pre>
 *
 * If no 'else' function is supplied or the function returns null, element for which the 'when'
 * predicate fails will be filtered by returning an {@link Optional#empty()} instead.
 */
@Immutable
public class ForkFunction<F, T> implements Function<F, Optional<T>> {

  protected final Predicate<F> whenPred;
  protected final Function<F, T> thenFunc;
  protected final Function<F, T> elseFunc;

  public ForkFunction(@Nullable Predicate<F> whenPred, @Nullable Function<F, T> thenFunc,
      @Nullable Function<F, T> elseFunc) {
    this.whenPred = (whenPred != null) ? whenPred : f -> true;
    this.thenFunc = thenFunc;
    this.elseFunc = elseFunc;
  }

  public ForkFunction(@Nullable Predicate<F> whenPred, @Nullable Function<F, T> thenFunc) {
    this(whenPred, thenFunc, null);
  }

  public ForkFunction(@Nullable Predicate<F> whenPred) {
    this(whenPred, null);
  }

  public ForkFunction() {
    this(null);
  }

  @NotNull
  public ForkFunction<F, T> when(@Nullable Predicate<F> whenPred) {
    return new ForkFunction<>(whenPred, thenFunc, elseFunc);
  }

  @NotNull
  public ForkFunction<F, T> thenMap(@Nullable Function<F, T> thenFunc) {
    return new ForkFunction<>(whenPred, thenFunc, elseFunc);
  }

  @NotNull
  public ForkFunction<F, T> elseMap(@Nullable Function<F, T> elseFunc) {
    return new ForkFunction<>(whenPred, thenFunc, elseFunc);
  }

  @NotNull
  public ForkFunction<F, T> elseFilter(@Nullable Consumer<F> elseConsumer) {
    return new ForkFunction<>(whenPred, thenFunc, asFunction(elseConsumer).andThen(f -> null));
  }

  @NotNull
  @Override
  public Optional<T> apply(F f) {
    return Optional.ofNullable(whenPred.test(f) ? thenFunc : elseFunc)
        .map(func -> func.apply(f))
        .filter(Objects::nonNull);
  }

  @NotNull
  public Function<F, Stream<T>> stream() {
    return f -> MoreOptional.stream(apply(f));
  }

  @NotNull
  public Consumer<F> asConsumer() {
    return this::apply;
  }

}
