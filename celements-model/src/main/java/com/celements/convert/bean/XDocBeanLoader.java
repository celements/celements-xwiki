package com.celements.convert.bean;

import java.util.Collection;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.restriction.ObjectRestriction;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@ComponentRole
public interface XDocBeanLoader<T> {

  /**
   * initialize the loader for a bean T
   */
  void initialize(@NotNull Class<T> token, @NotNull ClassIdentity classId);

  @NotNull
  T load(@NotNull XWikiDocument doc) throws BeanLoadException;

  @NotNull
  T load(@NotNull XWikiDocument doc,
      @Nullable Collection<ObjectRestriction<BaseObject>> restrictions) throws BeanLoadException;

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
