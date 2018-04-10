package com.celements.model.classes.fields;

import javax.validation.constraints.NotNull;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.field.Field;
import com.xpn.xwiki.objects.PropertyInterface;

public interface ClassField<T> extends Field<T> {

  @NotNull
  ClassDefinition getClassDef();

  @NotNull
  PropertyInterface getXField();

}
