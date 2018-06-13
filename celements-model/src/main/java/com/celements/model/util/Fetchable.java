package com.celements.model.util;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

/**
 * @param <T>
 *          element type
 */
public interface Fetchable<T> {

  /**
   * @return true if an element to fetch exists
   */
  boolean exists();

  /**
   * @return amount of fetched elements
   */
  int count();

  /**
   * @return the first fetched element
   */
  @NotNull
  Optional<T> first();

  /**
   * @return a {@link List} of all fetched elements
   */
  @NotNull
  List<T> list();

  /**
   * @return an {@link Iterable} for all fetched elements
   */
  @NotNull
  FluentIterable<T> iter();

}
