package com.celements.component;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.google.common.base.Supplier;
import com.xpn.xwiki.web.Utils;

public class ComponentInstanceSupplier<T> implements Supplier<T> {

  private final Class<T> role;
  private final String hint;

  public ComponentInstanceSupplier(@NotNull Class<T> role) {
    this(role, null);
  }

  public ComponentInstanceSupplier(@NotNull Class<T> role, @Nullable String hint) {
    this.role = checkNotNull(role);
    this.hint = firstNonNull(emptyToNull(hint), "default");
  }

  @Override
  public T get() {
    return Utils.getComponent(role, hint);
  }

}
