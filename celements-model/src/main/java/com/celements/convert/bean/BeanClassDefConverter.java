package com.celements.convert.bean;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;

import com.celements.convert.Converter;
import com.celements.convert.classes.ClassDefinitionConverter;
import com.celements.model.classes.ClassDefinition;

/**
 * <p>
 * Interface for {@link Converter} handling beans with a {@link ClassDefinition} to initialize
 * needed parameters.
 * </p>
 * <p>
 * IMPORTANT: call each {@link #initialize(Supplier)} and {@link #initialize(ClassDefinition)}
 * exactly once per component instantiation, else {@link #apply(A)} will throw
 * {@link IllegalStateException}. When used as a {@link Requirement}, implementing
 * {@link Initializable} is suitable.
 * </p>
 *
 * @see BeanConverter
 * @see ClassDefinitionConverter
 */
@ComponentRole
public interface BeanClassDefConverter<A, B> extends BeanConverter<A, B>, ClassDefinitionConverter<A, B> {

}
