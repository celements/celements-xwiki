package com.celements.model.field.internal;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.model.classes.fields.ClassField;
import com.celements.model.field.FieldAccessException;

/**
 * implementations allow to access values of any generic instance with {@link ClassField} objects
 */
@ComponentRole
public interface InternalFieldAccessor<T> {

  @NotNull
  String getName();

  /**
   * TODO javadoc
   *
   * @param <V>
   * @param instance
   * @param field
   * @return
   * @throws FieldAccessException
   */
  @NotNull
  Optional<Object> get(@NotNull T instance, @NotEmpty String fieldName);

  boolean set(@NotNull T instance, @NotEmpty String fieldName, @Nullable Object newValue);

}
