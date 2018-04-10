package com.celements.model.doc.field;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.model.field.Field;
import com.google.common.base.Optional;
import com.xpn.xwiki.doc.XWikiDocument;

@Immutable
@ComponentRole
public interface DocField<T> extends Field<T> {

  @NotNull
  Optional<T> getValue(@NotNull XWikiDocument doc);

  boolean setValue(@NotNull XWikiDocument doc, @Nullable T value);

}
