package com.celements.common;

/**
 * A task that returns a result and may throw an exception. Implementors define a single method with
 * one arguments called {@code call}.
 * <p>
 * Similar to {@link java.util.concurrent.Callable}, but with argument and throws a customizable
 * Exception instead.
 *
 * @param <A>
 *          the argument type of method {@code call}
 * @param <R>
 *          the result type of method {@code call}
 * @param <E>
 *          the exception type
 */
public interface Callable<A, R, E extends Exception> {

  /**
   * @param arg
   *          argument
   * @return computed result r
   * @throws E
   *           if unable to compute a result
   */
  R call(A arg) throws E;

}
