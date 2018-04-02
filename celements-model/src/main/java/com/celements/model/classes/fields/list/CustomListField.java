package com.celements.model.classes.fields.list;

import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.celements.marshalling.Marshaller;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;

@Immutable
public class CustomListField<T> extends ListField<T> {

  protected final List<T> values;

  public static class Builder<B extends Builder<B, T>, T> extends ListField.Builder<B, T> {

    private List<T> values;

    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<T> marshaller) {
      super(classDefName, name, marshaller);
    }

    @Override
    @SuppressWarnings("unchecked")
    public B getThis() {
      return (B) this;
    }

    public B values(List<T> values) {
      this.values = values;
      return getThis();
    }

    @Override
    public CustomListField<T> build() {
      return new CustomListField<>(getThis());
    }

  }

  protected CustomListField(@NotNull Builder<?, T> builder) {
    super(builder);
    this.values = builder.values != null ? ImmutableList.copyOf(builder.values)
        : ImmutableList.<T>of();
  }

  @Override
  protected PropertyClass newPropertyClass() {
    return new StaticListClass();
  }

  @Override
  protected boolean updatePropertyClass(PropertyClass propertyClass) {
    boolean updated = super.updatePropertyClass(propertyClass);
    // TODO createOrUpdateProperty for certain subtypes, e.g. EnumListField/ComponentListField
    updated |= createProperty(propertyClass, StringProperty.class, "values",
        serializedValuesForPropertyClass());
    return updated;
  }

  protected String serializedValuesForPropertyClass() {
    // always use DEFAULT_SEPARATOR, XWiki expects it in the PropertyClass
    return FluentIterable.from(getValues()).transform(getMarshaller().getSerializer()).filter(
        Predicates.notNull()).join(Joiner.on(DEFAULT_SEPARATOR));
  }

  public List<T> getValues() {
    return values;
  }

}
