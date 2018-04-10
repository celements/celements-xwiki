package com.celements.model.doc.field;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;
import com.xpn.xwiki.doc.XWikiDocument;

@Immutable
public abstract class AbstractDocField<T> implements DocField<T> {

  private final String name;
  private final Class<T> type;

  protected AbstractDocField(String name, Class<T> type) {
    this.name = checkNotNull(emptyToNull(name));
    this.type = checkNotNull(type);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Class<T> getType() {
    return type;
  }

  @Override
  public Optional<T> getValue(XWikiDocument doc) {
    return Optional.fromNullable(getValueInternal(doc));
  }

  protected abstract @Nullable T getValueInternal(@NotNull XWikiDocument doc);

  @Override
  public boolean setValue(XWikiDocument doc, T value) {
    if (!Objects.equals(getValueInternal(doc), value)) {
      setValueInternal(doc, value);
      return true;
    }
    return false;
  }

  protected abstract void setValueInternal(@NotNull XWikiDocument doc, @Nullable T value);

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DocField) {
      DocField<?> other = (DocField<?>) obj;
      return Objects.equals(this.getName(), other.getName());
    }
    return false;
  }

  @Override
  public String toString() {
    return name;
  }

}
