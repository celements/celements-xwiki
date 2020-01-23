package com.celements.marshalling;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Optional;
import com.xpn.xwiki.web.Utils;

public class ClassFieldMarshaller<T> extends AbstractMarshaller<ClassField<T>> {

  private final Class<T> token;
  private final ClassReference classRef;

  @SuppressWarnings("unchecked")
  ClassFieldMarshaller(Class<T> token, ClassReference classRef) {
    super((Class<ClassField<T>>) (Object) ClassField.class);
    this.token = token;
    this.classRef = classRef;
  }

  @Override
  public String serialize(ClassField<T> val) {
    return val.getName();
  }

  @Override
  public @NotNull Optional<ClassField<T>> resolve(String val) {
    return getClassDef().getField(val, token);
  }

  private ClassDefinition getClassDef() {
    return Utils.getComponent(ClassDefinition.class, classRef.serialize());
  };

}
