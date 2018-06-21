package com.celements.model.object;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Optional;

/**
 * Manipulates objects O on a document D for the defined query or fetches them for manipulation. Use
 * {@link ObjectFetcher} instead for read-only operations.
 *
 * @param <D>
 *          document type
 * @param <O>
 *          object type
 */
public interface ObjectEditor<D, O> extends ObjectHandler<D, O> {

  @NotNull
  @Override
  ObjectEditor<D, O> clone();

  /**
   * creates all objects defined by the query and also sets fields if any
   *
   * @return a map of all created objects indexed by their {@link ClassIdentity}
   */
  @NotNull
  Map<ClassIdentity, O> create();

  /**
   * like {@link #create()} but fetches an object if it exists already
   *
   * @return a map of all created or fetched objects indexed by their {@link ClassIdentity}
   */
  @NotNull
  Map<ClassIdentity, O> createIfNotExists();

  /**
   * creates the first object defined by the query and also sets fields if any
   */
  @NotNull
  O createFirst();

  /**
   * like {@link #createFirst()} but fetches an object if it exists already
   */
  @NotNull
  O createFirstIfNotExists();

  /**
   * deletes all objects defined by the query
   *
   * @return a list of all deleted objects
   */
  @NotNull
  List<O> delete();

  /**
   * deletes the first object defined by the query
   */
  @NotNull
  Optional<O> deleteFirst();

  /**
   * @return a fetcher which returns objects for manipulation
   */
  @NotNull
  ObjectFetcher<D, O> fetch();

  /**
   * @param field
   * @return {@link FieldEditor} which edits values for {@code field} on the queried objects
   */
  @NotNull
  <T> FieldEditor<T> editField(@NotNull ClassField<T> field);

  interface FieldEditor<T> {

    /**
     * sets the field to {@code value} for the first object
     *
     * @param value
     * @return if the object has changed
     */
    public boolean first(@Nullable T value);

    /**
     * sets the field to {@code value} for all objects
     *
     * @param value
     * @return if at least one object has changed
     */
    public boolean all(@Nullable final T value);

  }

}
