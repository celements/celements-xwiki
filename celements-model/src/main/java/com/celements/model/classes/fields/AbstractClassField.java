package com.celements.model.classes.fields;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.apache.commons.lang.StringUtils;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.xpn.xwiki.objects.PropertyInterface;
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

    protected final String classDefName;
    protected final String name;
    protected String prettyName;
    protected String validationRegExp;
    protected String validationMessage;

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
    this.prettyName = Optional.fromNullable(builder.prettyName).or(new Supplier<String>() {

      @Override
      public String get() {
        // lazy evaluation
        return Joiner.on(' ').join(StringUtils.splitByCharacterTypeCamelCase(name));
      }
    });
    this.validationRegExp = builder.validationRegExp;
    this.validationMessage = firstNonNull(builder.validationMessage, builder.classDefName + "_"
        + builder.name);
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

  @Override
  public PropertyInterface getXField() {
    PropertyClass element = getPropertyClass();
    element.setName(name);
    if (prettyName != null) {
      element.setPrettyName(prettyName);
    }
    if (validationRegExp != null) {
      element.setValidationRegExp(validationRegExp);
      element.setValidationMessage(validationMessage);
    }
    return element;
  }

  protected abstract PropertyClass getPropertyClass();

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

}
