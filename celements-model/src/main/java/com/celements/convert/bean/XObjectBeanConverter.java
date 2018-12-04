package com.celements.convert.bean;

import static com.google.common.base.Preconditions.*;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;

import com.celements.common.reflect.ReflectiveInstanceSupplier;
import com.celements.component.ComponentInstanceSupplier;
import com.celements.convert.classes.ClassDefinitionConverter;
import com.celements.convert.classes.XObjectConverter;
import com.celements.model.field.FieldAccessor;
import com.google.common.base.Supplier;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Converts an XObject to a Bean.
 * IMPORTANT: see {@link ClassDefinitionConverter}
 */
@Component(XObjectBeanConverter.NAME)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XObjectBeanConverter<T> extends XObjectConverter<T> implements
    BeanClassDefConverter<BaseObject, T> {

  public static final String NAME = "xobjectbean";

  private Supplier<T> supplier;

  @Requirement(BeanFieldAccessor.NAME)
  private FieldAccessor<T> beanAccessor;

  @Override
  public void initialize(Supplier<T> instanceSupplier) {
    this.supplier = instanceSupplier;
  }

  @Override
  public void initialize(Class<T> token) {
    checkNotNull(token);
    if (token.isAnnotationPresent(ComponentRole.class)) {
      initialize(new ComponentInstanceSupplier<>(token));
    } else {
      initialize(new ReflectiveInstanceSupplier<>(token));
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public FieldAccessor<T> getToFieldAccessor() {
    return beanAccessor;
  }

  @Override
  protected Supplier<T> getInstanceSupplier() {
    checkState(supplier != null, "not initialized");
    return supplier;
  }

}
