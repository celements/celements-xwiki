package com.celements.model.object;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.field.FieldAccessor;
import com.google.common.collect.FluentIterable;

/**
 * Bridge for effective access on document and objects, primarily used by {@link ObjectHandler}s to
 * allow generic implementations
 *
 * @param <D>
 *          document type
 * @param <O>
 *          object type
 */
@Immutable
@Singleton
@ComponentRole
public interface ObjectBridge<D, O> {

  @NotNull
  Class<D> getDocumentType();

  @NotNull
  Class<O> getObjectType();

  /**
   * @deprecated without replacement since 4.8
   */
  @Deprecated
  void checkDoc(@NotNull D doc);

  @NotNull
  DocumentReference getDocRef(@NotNull D doc);

  @NotNull
  String getLanguage(@NotNull D doc);

  @NotNull
  String getDefaultLanguage(@NotNull D doc);

  @NotNull
  FluentIterable<? extends ClassIdentity> getDocClasses(@NotNull D doc);

  @NotNull
  FluentIterable<O> getObjects(@NotNull D doc, @NotNull ClassIdentity classId);

  int getObjectNumber(@NotNull O obj);

  @NotNull
  ClassIdentity getObjectClass(@NotNull O obj);

  @NotNull
  O cloneObject(@NotNull O obj);

  @NotNull
  O createObject(@NotNull D doc, @NotNull ClassIdentity classId);

  boolean deleteObject(@NotNull D doc, @NotNull O obj);

  @NotNull
  FieldAccessor<D> getDocumentFieldAccessor();

  @NotNull
  FieldAccessor<O> getObjectFieldAccessor();

}
