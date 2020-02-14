package com.celements.model.classes.fields.list;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.Marshaller;
import com.celements.model.classes.fields.AbstractClassField;
import com.celements.model.classes.fields.CustomClassField;
import com.google.common.base.Enums;
import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public abstract class AbstractListField<T, E> extends AbstractClassField<T> implements
    CustomClassField<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractListField.class);

  protected static final String DEFAULT_SEPARATOR = "|";

  protected final Marshaller<E> marshaller;
  private final Integer size;
  private final DisplayType displayType;
  private final Boolean picker;

  public abstract static class Builder<B extends Builder<B, T, E>, T, E> extends
      AbstractClassField.Builder<B, T> {

    protected final Marshaller<E> marshaller;
    protected Integer size;
    protected DisplayType displayType;
    protected Boolean picker;

    @Deprecated
    public Builder(@NotNull String classDefName, @NotNull String name,
        @NotNull Marshaller<E> marshaller) {
      super(classDefName, name);
      this.marshaller = checkNotNull(marshaller);
    }

    public Builder(@NotNull ClassReference classRef, @NotNull String name,
        @NotNull Marshaller<E> marshaller) {
      super(classRef, name);
      this.marshaller = checkNotNull(marshaller);
    }

    public B size(@Nullable Integer val) {
      size = val;
      return getThis();
    }

    /**
     * @deprecated instead use {@link #displayType(DisplayType)}
     * @since 3.4
     */
    @Deprecated
    public B displayType(@Nullable String val) {
      displayType = Enums.getIfPresent(DisplayType.class, val).orNull();
      return getThis();
    }

    public B displayType(@Nullable DisplayType val) {
      displayType = val;
      return getThis();
    }

    public B picker(@Nullable Boolean val) {
      picker = val;
      return getThis();
    }

  }

  protected AbstractListField(@NotNull Builder<?, T, E> builder) {
    super(builder);
    this.marshaller = builder.marshaller;
    this.size = firstNonNull(builder.size, 1);
    this.displayType = builder.displayType;
    this.picker = builder.picker;
  }

  protected String serializeInternal(@NotNull List<E> values) {
    values = firstNonNull(values, ImmutableList.<E>of());
    return FluentIterable.from(values).transform(marshaller.getSerializer()).filter(
        Predicates.notNull()).join(Joiner.on(getSeparator().substring(0, 1)));
  }

  protected FluentIterable<E> resolveInternal(@Nullable Object obj) {
    FluentIterable<String> iter;
    if (obj instanceof String) {
      iter = FluentIterable.from(Splitter.onPattern("[" + getSeparator() + "]").split(
          (String) obj));
    } else if (obj instanceof List) {
      iter = FluentIterable.from((List<?>) obj).filter(Predicates.notNull()).transform(
          Functions.toStringFunction());
    } else {
      iter = FluentIterable.from(Collections.<String>emptyList());
      if (obj != null) {
        LOGGER.warn("unable to resolve value '{}' for '{}'", obj, this);
      }
    }
    return iter.transform(marshaller.getResolver()).filter(Predicates.notNull());
  }

  public Marshaller<E> getMarshaller() {
    return marshaller;
  }

  public Integer getSize() {
    return size;
  }

  /**
   * @deprecated instead use {@link #getDisplayTypeEnum()}
   * @since 3.4
   */
  @Deprecated
  public String getDisplayType() {
    String displayTypeStr = null;
    if (getDisplayTypeEnum() != null) {
      displayTypeStr = getDisplayTypeEnum().name();
    }
    return displayTypeStr;
  }

  public DisplayType getDisplayTypeEnum() {
    return displayType;
  }

  public Boolean getPicker() {
    return picker;
  }

  protected String getSeparator() {
    return DEFAULT_SEPARATOR;
  }

  @Override
  protected PropertyClass getPropertyClass() {
    ListClass element = getListClass();
    if (size != null) {
      element.setSize(size);
    }
    if (displayType != null) {
      element.setDisplayType(displayType.name());
    }
    if (picker != null) {
      element.setPicker(picker);
    }
    element.setSeparators(getSeparator());
    return element;
  }

  @NotNull
  protected abstract ListClass getListClass();

}
