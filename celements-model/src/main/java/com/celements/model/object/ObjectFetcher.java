package com.celements.model.object;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldFetcher;
import com.celements.model.util.Fetchable;

/**
 * Fetches objects O on a document D for the defined query. Returned objects are intended for
 * read-only operations. Use {@link ObjectEditor} instead for manipulations.
 *
 * @param <D>
 *          document type
 * @param <O>
 *          object type
 */
public interface ObjectFetcher<D, O> extends ObjectHandler<D, O>, Fetchable<O> {

  /**
   * @return a {@link Map} of all fetched objects indexed by their {@link ClassIdentity}
   */
  @NotNull
  Map<ClassIdentity, List<O>> map();

  <T> FieldFetcher<O, T> field(ClassField<T> field);

  <T> List<FieldFetcher<O, T>> fields(List<ClassField<T>> fields);

}
