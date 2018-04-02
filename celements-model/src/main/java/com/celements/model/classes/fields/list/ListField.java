package com.celements.model.classes.fields.list;

import static com.google.common.base.MoreObjects.*;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.marshalling.Marshaller;
import com.google.common.base.Strings;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;

public abstract class ListField<T> extends AbstractListField<List<T>, T> {

  private final Boolean multiSelect;
  private final String separator;

  public abstract static class Builder<B extends Builder<B, T>, T> extends
      AbstractListField.Builder<B, List<T>, T> {

    private Boolean multiSelect;
    private String separator;

    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name, marshaller);
    }

    public B multiSelect(@Nullable Boolean val) {
      multiSelect = val;
      return getThis();
    }

    // XXX can be removed once all projects include the celements-model 1.2+, but for now
    // override needed for backwards compatibility with older version to avoid NoSuchMethodError
    @Override
    public B size(@Nullable Integer val) {
      return super.size(val);
    }

    // XXX can be removed once all projects include the celements-model 1.2+, but for now
    // override needed for backwards compatibility with older version to avoid NoSuchMethodError
    @Override
    public B displayType(@Nullable String val) {
      return super.displayType(val);
    }

    // XXX can be removed once all projects include the celements-model 1.2+, but for now
    // override needed for backwards compatibility with older version to avoid NoSuchMethodError
    @Override
    public B picker(@Nullable Boolean val) {
      return super.picker(val);
    }

    public B separator(@Nullable String val) {
      separator = val;
      return getThis();
    }

  }

  protected ListField(@NotNull Builder<?, T> builder) {
    super(builder);
    this.multiSelect = builder.multiSelect;
    this.separator = firstNonNull(Strings.emptyToNull(builder.separator), DEFAULT_SEPARATOR);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Class<List<T>> getType() {
    return (Class<List<T>>) (Object) List.class;
  }

  @Override
  public Object serialize(List<T> values) {
    return serializeInternal(values);
  }

  @Override
  public List<T> resolve(Object obj) {
    return resolveInternal(obj).toList();
  }

  public boolean isMultiSelect() {
    return firstNonNull(multiSelect, false);
  }

  public Boolean getMultiSelect() {
    return multiSelect;
  }

  @Override
  public String getSeparator() {
    return separator;
  }

  @Override
  protected boolean updatePropertyClass(PropertyClass propertyClass) {
    boolean updated = super.updatePropertyClass(propertyClass);
    updated |= createOrUpdateProperty(propertyClass, IntegerProperty.class, "multiSelect",
        asInteger(getMultiSelect()));
    return updated;
  }

}
