package com.celements.convert.bean;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.restriction.ObjectRestriction;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * a utility component for loading a generic bean T for a given xDoc/docRef. It uses the
 * {@link XObjectBeanConverter} internally and simplifies it's usage.
 *
 * @param <T>
 *          type of bean to load
 */
@ComponentRole
public interface XDocBeanLoader<T> {

  /**
   * initialize the loader for a bean T
   */
  void initialize(@NotNull Class<T> token, @NotNull ClassIdentity classId);

  @NotNull
  Class<T> getToken();

  @NotNull
  ClassIdentity getClassId();

  /**
   * loads a bean T for the given docRef. the first object of class {@link #getClassId()} on the doc
   * will be used for conversion to a bean.
   *
   * @throws BeanLoadException
   *           if unable to load a bean from doc or object
   */
  @NotNull
  T load(@NotNull DocumentReference docRef) throws BeanLoadException;

  /**
   * loads a bean T for the given docRef. the first object of class {@link #getClassId()}
   * considering given restrictions will be used for conversion to a bean.
   *
   * @throws BeanLoadException
   *           if unable to load a bean from doc or object
   */
  @NotNull
  T load(@NotNull DocumentReference docRef,
      @Nullable Iterable<ObjectRestriction<BaseObject>> restrictions) throws BeanLoadException;

  /**
   * loads a bean T for the given doc. the first object of class {@link #getClassId()} on the doc
   * will be used for conversion to a bean.
   *
   * @throws BeanLoadException
   *           if unable to load a bean from doc or object
   */
  @NotNull
  T load(@NotNull XWikiDocument doc) throws BeanLoadException;

  /**
   * loads a bean T for the given doc. the first object of class {@link #getClassId()}
   * considering given restrictions will be used for conversion to a bean.
   *
   * @throws BeanLoadException
   *           if unable to load a bean from doc or object
   */
  @NotNull
  T load(@NotNull XWikiDocument doc, @Nullable Iterable<ObjectRestriction<BaseObject>> restrictions)
      throws BeanLoadException;

  class BeanLoadException extends Exception {

    private static final long serialVersionUID = 6902845825260242861L;

    public BeanLoadException(String message) {
      super(message);
    }

    public BeanLoadException(String message, Throwable cause) {
      super(message, cause);
    }

  }

}
