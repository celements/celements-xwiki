package com.celements.model.classes.fields;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.google.common.primitives.Ints;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

@Immutable
public final class BooleanField extends AbstractClassField<Boolean> implements
    CustomClassField<Boolean> {

  private final String displayFormType;
  private final String displayType;
  private final Integer defaultValue;

  public static class Builder extends AbstractClassField.Builder<Builder, Boolean> {

    private String displayFormType;
    private String displayType;
    private Integer defaultValue;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name) {
      super(classDefName, name);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name) {
      super(classRef, name);
    }

    @Override
    public Builder getThis() {
      return this;
    }

    public Builder displayFormType(@Nullable String val) {
      displayFormType = val;
      return getThis();
    }

    public Builder displayType(@Nullable String val) {
      displayType = val;
      return getThis();
    }

    public Builder defaultValue(@Nullable Integer val) {
      defaultValue = val;
      return getThis();
    }

    @Override
    public BooleanField build() {
      return new BooleanField(getThis());
    }
  }

  protected BooleanField(@NotNull Builder builder) {
    super(builder);
    this.displayFormType = builder.displayFormType;
    this.displayType = builder.displayType;
    this.defaultValue = builder.defaultValue;
  }

  @Override
  public Class<Boolean> getType() {
    return Boolean.class;
  }

  public String getDisplayFormType() {
    return displayFormType;
  }

  public String getDisplayType() {
    return displayType;
  }

  public Integer getDefaultValue() {
    return defaultValue;
  }

  @Override
  protected PropertyClass getPropertyClass() {
    BooleanClass element = new BooleanClass();
    if (displayFormType != null) {
      element.setDisplayFormType(displayFormType);
    }
    if (displayType != null) {
      element.setDisplayType(displayType);
    }
    if (defaultValue != null) {
      element.setDefaultValue(defaultValue);
    }
    return element;
  }

  @Override
  public Optional<Integer> serialize(Boolean value) {
    return Optional.ofNullable(value)
        .map(val -> Boolean.TRUE.equals(val) ? 1 : 0);
  }

  @Override
  public Optional<Boolean> resolve(Object obj) {
    return Optional.ofNullable(obj)
        .map(Object::toString)
        .map(Ints::tryParse)
        .map(val -> !Integer.valueOf(0).equals(val));
  }

}
