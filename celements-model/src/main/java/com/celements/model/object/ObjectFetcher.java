package com.celements.model.object;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.constraints.NotNull;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;

/**
 * Fetches objects O on a document D for the defined query. Returned objects are intended for
 * read-only operations. Use {@link ObjectEditor} instead for manipulations.
 *
 * @param <D>
 *          document type
 * @param <O>
 *          object type
 */
public interface ObjectFetcher<D, O> extends ObjectHandler<D, O> {

  @NotNull
  @Override
  ObjectFetcher<D, O> clone();

  /**
   * @return true if an object to fetch exists
   */
  boolean exists();

  /**
   * @return amount of fetched objects
   */
  int count();

  /**
   * @return the first fetched object
   */
  @NotNull
  Optional<O> first();

  /**
   * @return the first fetched object
   * @throws IllegalArgumentException
   *           if there is no object to fetch
   */
  @NotNull
  public O firstAssert();

  /**
   * @return the sole object to fetch
   * @throws IllegalArgumentException
   *           if there is no unique object to fetch
   */
  @NotNull
  O unique();

  /**
   * @return a {@link List} of all fetched objects
   */
  @NotNull
  List<O> list();

  /**
   * @return an {@link Iterable} of all fetched objects
   */
  @NotNull
  FluentIterable<O> iter();

  /**
   * @return a {@link Map} of all fetched objects indexed by their {@link ClassIdentity}
   */
  @NotNull
  Map<ClassIdentity, List<O>> map();

  /**
   * @param field
   * @return {@link FieldFetcher} which gets values for {@code field} from the queried objects
   */
  @NotNull
  <T> FieldFetcher<T> fetchField(@NotNull ClassField<T> field);

  interface FieldFetcher<T> {

    /**
     * @return the first field value
     */
    @NotNull
    Optional<T> first();

    /**
     * @return a {@link List} of all field values
     */
    @NotNull
    List<T> list();

    /**
     * @return a {@link Set} of all field values
     */
    @NotNull
    Set<T> set();

    /**
     * @return an {@link Iterable} of all not-null field values
     */
    @NotNull
    FluentIterable<T> iter();

    /**
     * @return an {@link Iterable} of all field values, may contain null
     */
    @NotNull
    FluentIterable<T> iterNullable();

  }

}
