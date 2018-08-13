package com.celements.convert;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

import com.google.common.base.Function;

/**
 * implementations allow to convert an instance of type A to type B. they can be used as a
 * {@link Function}.
 */
@ComponentRole
public interface Converter<A, B> extends Function<A, B> {

  @NotNull
  String getName();

  @NotNull
  @Override
  B apply(@Nullable A data) throws ConversionException;

  /**
   * @param instance
   *          instance converted to
   * @return same as instance
   */
  @NotNull
  B apply(@NotNull B instance, @Nullable A data) throws ConversionException;

}
