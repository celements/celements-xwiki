package com.celements.convert.bean;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;

import com.celements.component.ComponentInstanceSupplier;
import com.celements.convert.Converter;
import com.google.common.base.Supplier;

/**
 * <p>
 * Interface for {@link Converter} handling beans to initialize needed parameters.
 * </p>
 * <p>
 * IMPORTANT: call {@link #initialize(Supplier)} exactly once per component instantiation, else
 * {@link #apply(A)} will throw {@link IllegalStateException}. When used as a {@link Requirement},
 * implementing {@link Initializable} is suitable.
 * </p>
 */
@ComponentRole
public interface BeanConverter<A, B> extends Converter<A, B> {

  /**
   * initialize the converter with a bean B supplier for creating instances
   *
   * @param instanceSupplier
   *          e.g. {@link ComponentInstanceSupplier}
   */
  void initialize(@NotNull Supplier<B> instanceSupplier);

}
