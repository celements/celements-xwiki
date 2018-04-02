package com.celements.model.classes.fields;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Strings;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.Utils;

/**
 * Subclasses are expected to be immutable
 */
public abstract class AbstractClassField<T> implements ClassField<T> {

  private final String classDefName;
  private final String name;
  private final String prettyName;
  private final String validationRegExp;
  private final String validationMessage;

  public abstract static class Builder<B extends Builder<B, T>, T> {

    private final String classDefName;
    private final String name;
    private String prettyName;
    private String validationRegExp;
    private String validationMessage;

    public Builder(@NotNull String classDefName, @NotNull String name) {
      this.classDefName = checkNotNull(classDefName);
      this.name = checkNotNull(Strings.emptyToNull(name));
    }

    public abstract B getThis();

    public B prettyName(@Nullable String val) {
      prettyName = val;
      return getThis();
    }

    public B validationRegExp(@Nullable String val) {
      validationRegExp = val;
      return getThis();
    }

    public B validationMessage(@Nullable String val) {
      validationMessage = val;
      return getThis();
    }

    public abstract AbstractClassField<T> build();

  }

  protected AbstractClassField(@NotNull Builder<?, T> builder) {
    this.classDefName = builder.classDefName;
    this.name = builder.name;
    this.prettyName = firstNonNull(builder.prettyName, builder.name);
    this.validationRegExp = builder.validationRegExp;
    this.validationMessage = builder.validationMessage;
  }

  @Override
  public ClassDefinition getClassDef() {
    return Utils.getComponent(ClassDefinition.class, classDefName);
  }

  @Override
  public String getName() {
    return name;
  }

  public String getPrettyName() {
    return prettyName;
  }

  public String getValidationRegExp() {
    return validationRegExp;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  @Deprecated
  @Override
  public PropertyInterface getXField() {
    return createXWikiPropertyClass();
  }

  @Override
  public PropertyClass createXWikiPropertyClass() {
    PropertyClass propertyClass = newPropertyClass();
    updateXWikiPropertyClass(propertyClass);
    return propertyClass;
  }

  @Override
  public boolean updateXWikiPropertyClass(PropertyClass propertyClass) {
    boolean updated = false;
    updated |= createOrUpdateProperty(propertyClass, StringProperty.class, "name", getName());
    updated |= createProperty(propertyClass, StringProperty.class, "prettyName", getPrettyName());
    updated |= createProperty(propertyClass, StringProperty.class, "validationRegExp",
        getValidationRegExp());
    updated |= createProperty(propertyClass, StringProperty.class, "validationMessage",
        getValidationMessage());
    updated |= updatePropertyClass(propertyClass);
    return updated;
  }

  protected abstract PropertyClass newPropertyClass();

  protected abstract boolean updatePropertyClass(PropertyClass propertyClass);

  @Override
  public int hashCode() {
    return Objects.hash(classDefName, name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof AbstractClassField) {
      AbstractClassField<?> other = (AbstractClassField<?>) obj;
      return Objects.equals(this.classDefName, other.classDefName) && Objects.equals(this.name,
          other.name);
    }
    return false;
  }

  @Override
  public String toString() {
    return getClassDef() + "." + name;
  }

  protected static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  protected boolean createProperty(PropertyClass propertyClass, Class<? extends BaseProperty> type,
      String name, Object value) {
    BaseProperty property = (BaseProperty) propertyClass.get(name);
    if (property == null) {
      try {
        property = type.newInstance();
      } catch (ReflectiveOperationException exc) {
        throw new IllegalArgumentException("illegal type [" + type.getSimpleName() + "]", exc);
      }
      property.setName(name);
      property.setValue(value);
      propertyClass.put(name, property);
      return true;
    }
    return false;
  }

  protected boolean createOrUpdateProperty(PropertyClass propertyClass,
      Class<? extends BaseProperty> type, String name, Object value) {
    if (!createProperty(propertyClass, type, name, value)) {
      BaseProperty prop = (BaseProperty) propertyClass.get(name);
      if (!Objects.equals(prop.getValue(), value)) {
        prop.setValue(value);
        return true;
      }
    }
    return false;
  }

  protected Integer asInteger(Boolean bool) {
    return bool != null ? (bool ? 1 : 0) : null;
  }

}
