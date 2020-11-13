package com.celements.model.field;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

/**
 * implementations allow to access values denoted by Strings of any generic instance. Use
 * {@link FieldAccessor} instead if possible.
 */
@ComponentRole
public interface StringFieldAccessor<T> {

  @NotNull
  String getName();

  /**
   * gets the value denoted by the field name on an instance
   *
   * @throws FieldAccessException
   *           if an exception occurred while trying to access the field
   */
  @NotNull
  Optional<Object> get(@NotNull T instance, @NotEmpty String fieldName);

  /**
   * sets the value denoted by the field name on an instance
   *
   * @return true if the field value has changed on the instance
   * @throws FieldAccessException
   *           if an exception occurred while trying to access the field
   */
  boolean set(@NotNull T instance, @NotEmpty String fieldName, @Nullable Object newValue);

}
