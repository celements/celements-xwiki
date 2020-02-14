package com.celements.model.classes.fields.list;

import static com.google.common.base.MoreObjects.*;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.Marshaller;
import com.google.common.base.Strings;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public abstract class ListField<T> extends AbstractListField<List<T>, T> {

  private final Boolean multiSelect;
  private final String separator;

  public abstract static class Builder<B extends Builder<B, T>, T> extends
      AbstractListField.Builder<B, List<T>, T> {

    protected Boolean multiSelect;
    protected String separator;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name, marshaller);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classRef, name, marshaller);
    }

    public B multiSelect(@Nullable Boolean val) {
      multiSelect = val;
      return getThis();
    }

    public B separator(@Nullable String val) {
      separator = val;
      return getThis();
    }

  }

  protected ListField(@NotNull Builder<?, T> builder) {
    super(getBuilderSizeDefaultSet(builder));
    this.multiSelect = builder.multiSelect;
    this.separator = firstNonNull(Strings.emptyToNull(builder.separator), DEFAULT_SEPARATOR);
  }

  static <T> Builder<? extends Builder<?, T>, T> getBuilderSizeDefaultSet(Builder<?, T> builder) {
    if ((builder.multiSelect != null) && builder.multiSelect) {
      builder.size(firstNonNull(builder.size, 2));
    }
    return builder;
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
  protected PropertyClass getPropertyClass() {
    ListClass element = (ListClass) super.getPropertyClass();
    if (multiSelect != null) {
      element.setMultiSelect(multiSelect);
    }
    return element;
  }

}
