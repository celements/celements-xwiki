package com.celements.marshalling;

import static com.google.common.base.Preconditions.*;

import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.fields.ClassField;
import com.google.common.base.Optional;

public class ClassFieldMarshaller<T> extends AbstractMarshaller<ClassField<T>> {

  private final Class<T> token;
  private final ClassReference classRef;

  @SuppressWarnings("unchecked")
  public ClassFieldMarshaller(Class<T> token, ClassReference classRef) {
    super((Class<ClassField<T>>) (Object) ClassField.class);
    this.token = checkNotNull(token);
    this.classRef = checkNotNull(classRef);
  }

  @Override
  public String serialize(ClassField<T> val) {
    return val.getName();
  }

  @Override
  public @NotNull Optional<ClassField<T>> resolve(String val) {
    return Optional.fromJavaUtil(classRef.getClassDefinition()
        .flatMap(classDef -> classDef.getField(val, token)));
  }

}
