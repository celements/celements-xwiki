package com.celements.model.field;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.model.classes.fields.ClassField;

/**
 * implementations allow to access values of any generic instance with {@link ClassField} objects
 */
@ComponentRole
public interface FieldAccessor<T> {

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
  <V> Optional<V> get(@NotNull T instance, @NotNull ClassField<V> field);

  <V> boolean set(@NotNull T instance, @NotNull ClassField<V> field, @Nullable V newValue);

  @NotNull
  @Deprecated
  <V> com.google.common.base.Optional<V> getValue(@NotNull T instance, @NotNull ClassField<V> field)
      throws FieldAccessException;

  @Deprecated
  <V> boolean setValue(@NotNull T instance, @NotNull ClassField<V> field, @Nullable V value)
      throws FieldAccessException;

}
