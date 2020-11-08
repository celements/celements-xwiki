package com.celements.model.object;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

import com.celements.model.object.restriction.ObjectQuery;

/**
 * Handles objects O on a document D for the defined query. Can be an {@link ObjectFetcher} or
 * {@link ObjectEditor}.
 *
 * @param <D>
 *          document type
 * @param <O>
 *          object type
 */
public interface ObjectHandler<D, O> extends Cloneable {

  @NotNull
  DocumentReference getDocRef();

  @NotNull
  D getDocument();

  @NotNull
  Optional<D> getTranslationDoc();

  @NotNull
  ObjectHandler<D, O> withTranslation(@Nullable D transDoc);

  /**
   * @return clone of the current query
   */
  @NotNull
  ObjectQuery<O> getQuery();

  @NotNull
  ObjectHandler<D, O> clone();

}
