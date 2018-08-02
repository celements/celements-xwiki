package com.celements.convert.classes;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.Requirement;

import com.celements.convert.Converter;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.field.FieldAccessor;

/**
 * <p>
 * Interface for {@link Converter} simplifying conversions based on a specific
 * {@link ClassDefinition} by using {@link FieldAccessor} implementations.
 * </p>
 * <p>
 * IMPORTANT: call {@link #initialize(ClassDefinition)} exactly once per component instantiation,
 * else {@link #apply(A)} will throw {@link IllegalStateException}. When used as a
 * {@link Requirement}, implementing {@link Initializable} is suitable.
 * </p>
 *
 * @see AbstractClassDefConverter
 */
public interface ClassDefinitionConverter<A, B> extends Converter<A, B> {

  void initialize(@NotNull ClassDefinition classDef);

  @NotNull
  ClassDefinition getClassDef();

  @NotNull
  FieldAccessor<A> getFromFieldAccessor();

  @NotNull
  FieldAccessor<B> getToFieldAccessor();

}
