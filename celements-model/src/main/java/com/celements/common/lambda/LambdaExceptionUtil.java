package com.celements.common.lambda;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This helper simplifies the usage of checked exceptions with lambdas. It sneakily throws the 
 * checked exception, but still enforces catching for callers of the rethrow methods.
 * Based on https://stackoverflow.com/a/27644392
 */
public final class LambdaExceptionUtil {

  @FunctionalInterface
  public interface ThrowingFunction<T, R, E extends Exception> {
    R apply(T t) throws E;
  }

  public static <T, R, E extends Exception> Function<T, R> rethrowFunction(
      ThrowingFunction<T, R, E> function) throws E {
    return t -> {
      try {
        return function.apply(t);
      } catch (Exception exception) {
        sneakyThrow(exception);
        throw new RuntimeException("sneakyThrow always throws");
      }
    };
  }

  @FunctionalInterface
  public interface ThrowingPredicate<T, E extends Exception> {
    boolean test(T t) throws E;
  }

  public static <T, E extends Exception> Predicate<T> rethrowPredicate(
      ThrowingPredicate<T, E> predicate) throws E {
    return t -> {
      try {
        return predicate.test(t);
      } catch (Exception exception) {
        sneakyThrow(exception);
        throw new RuntimeException("sneakyThrow always throws");
      }
    };
  }

  @FunctionalInterface
  public interface ThrowingConsumer<T, E extends Exception> {
    void accept(T t) throws E;
  }

  public static <T, E extends Exception> Consumer<T> rethrowConsumer(
      ThrowingConsumer<T, E> consumer) throws E {
    return t -> {
      try {
        consumer.accept(t);
      } catch (Exception exception) {
        sneakyThrow(exception);
      }
    };
  }

  @FunctionalInterface
  public interface ThrowingBiConsumer<T, U, E extends Exception> {
    void accept(T t, U u) throws E;
  }

  public static <T, U, E extends Exception> BiConsumer<T, U> rethrowBiConsumer(
      ThrowingBiConsumer<T, U, E> biConsumer) throws E {
    return (t, u) -> {
      try {
        biConsumer.accept(t, u);
      } catch (Exception exception) {
        sneakyThrow(exception);
      }
    };
  }

  @FunctionalInterface
  public interface ThrowingSupplier<T, E extends Exception> {
    T get() throws E;
  }

  public static <T, E extends Exception> Supplier<T> rethrowSupplier(
      ThrowingSupplier<T, E> supplier) throws E {
    return () -> {
      try {
        return supplier.get();
      } catch (Exception exception) {
        sneakyThrow(exception);
        throw new RuntimeException("sneakyThrow always throws");
      }
    };
  }

  @FunctionalInterface
  public interface ThrowingRunnable<E extends Exception> {
    void run() throws E;
  }

  public static <T, E extends Exception> Runnable rethrowRunnable(
      ThrowingRunnable<E> runnable) throws E {
    return () -> {
      try {
        runnable.run();
      } catch (Exception exception) {
        sneakyThrow(exception);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private static <E extends Throwable> void sneakyThrow(Exception exception) throws E {
    throw (E) exception;
  }

}
