package com.celements.model.classes.fields;

import java.util.Date;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.google.common.base.Strings;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

@Immutable
public final class DateField extends AbstractClassField<Date> {

  private final Integer size;
  private final Integer emptyIsToday;
  private final String dateFormat;

  public static class Builder extends AbstractClassField.Builder<Builder, Date> {

    private Integer size;
    private Integer emptyIsToday;
    private String dateFormat;

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

    public Builder size(@Nullable Integer val) {
      size = val;
      return getThis();
    }

    public Builder emptyIsToday(@Nullable Integer val) {
      emptyIsToday = val;
      return getThis();
    }

    public Builder dateFormat(@Nullable String val) {
      dateFormat = val;
      return getThis();
    }

    @Override
    public DateField build() {
      return new DateField(getThis());
    }
  }

  protected DateField(@NotNull Builder builder) {
    super(builder);
    this.size = builder.size;
    this.emptyIsToday = builder.emptyIsToday;
    this.dateFormat = builder.dateFormat;
  }

  @Override
  protected String generatePrettyName(AbstractClassField.Builder<?, Date> builder) {
    String prettyName = super.generatePrettyName(builder);
    String dateFormat = ((Builder) builder).dateFormat;
    if (!Strings.isNullOrEmpty(dateFormat)) {
      prettyName += " (" + dateFormat + ")";
    }
    return prettyName;
  }

  @Override
  public Class<Date> getType() {
    return Date.class;
  }

  public Integer getSize() {
    return size;
  }

  public Integer getEmptyIsToday() {
    return emptyIsToday;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  @Override
  protected PropertyClass getPropertyClass() {
    DateClass element = new DateClass();
    if (size != null) {
      element.setSize(size);
    }
    if (emptyIsToday != null) {
      element.setEmptyIsToday(emptyIsToday);
    }
    if (dateFormat != null) {
      element.setDateFormat(dateFormat);
    }
    return element;
  }

}
