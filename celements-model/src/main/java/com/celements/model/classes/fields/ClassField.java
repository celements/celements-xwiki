package com.celements.model.classes.fields;

import javax.validation.constraints.NotNull;

import com.celements.model.classes.ClassDefinition;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.PropertyClass;

public interface ClassField<T> {

  @NotNull
  public ClassDefinition getClassDef();

  @NotNull
  public String getName();

  @NotNull
  public Class<T> getType();

  /**
   * @deprecated instead use {@link #createXWikiPropertyClass()} or
   *             {@link #updateXWikiPropertyClass(PropertyClass)}
   */
  @Deprecated
  @NotNull
  public PropertyInterface getXField();

  @NotNull
  public PropertyClass createXWikiPropertyClass();

  public boolean updateXWikiPropertyClass(@NotNull PropertyClass propertyClass);

}
