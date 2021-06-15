package com.celements.common.function;

import static com.celements.common.MoreFunctions.*;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.concurrent.Immutable;

/**
 * A specialized composite predicate that facilitates forked execution. Consists of a predicate
 * 'when' and the two consumers 'then' and 'else'. If 'when' evaluates to true for an element,
 * 'then' is executed for it, otherwise 'else'.
 *
 * <pre>
 * stream.map(new ForkPredicate<F, T>()
 *     .when(x -> test(x))
 *     .thenDo(x -> doAccept(x))
 *     .elseDo(x -> doFail(x)))
 * </pre>
 *
 * If no 'else' is supplied or returns null, false elements will be filtered by returning
 * {@link Optional#empty()} instead.
 */
@Immutable
public class ForkPredicate<T> implements Predicate<T> {

  private final ForkFunction<T, Boolean> delegate;

  private ForkPredicate(ForkFunction<T, Boolean> delegate) {
    this.delegate = delegate;
  }

  public ForkPredicate(Predicate<T> when, Consumer<T> thenDo, Consumer<T> elseDo) {
    this(new ForkFunction<>(when, asFunction(thenDo).andThen(t -> true)).elseFilter(elseDo));
  }

  public ForkPredicate(Predicate<T> when) {
    this(new ForkFunction<>(when, t -> true, t -> false));
  }

  public ForkPredicate() {
    this(new ForkFunction<>(null, t -> true, t -> false));
  }

  public ForkPredicate<T> when(Predicate<T> pred) {
    return new ForkPredicate<>(delegate.when(pred));
  }

  public ForkPredicate<T> thenDo(Consumer<T> thenDo) {
    return new ForkPredicate<>(delegate.thenMap(asFunction(thenDo).andThen(t -> true)));
  }

  public ForkPredicate<T> elseDo(Consumer<T> elseDo) {
    return new ForkPredicate<>(delegate.elseFilter(elseDo));
  }

  @Override
  public boolean test(T t) {
    return delegate.apply(t).orElse(false);
  }

  public Consumer<T> asConsumer() {
    return delegate::apply;
  }

}
