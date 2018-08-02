package com.celements.component;

import com.google.common.base.Supplier;
import com.xpn.xwiki.web.Utils;

public class ComponentInstanceSupplier<T> implements Supplier<T> {

  private final Class<T> role;
  private final String hint;

  public ComponentInstanceSupplier(Class<T> role) {
    this(role, "default");
  }

  public ComponentInstanceSupplier(Class<T> role, String hint) {
    this.role = role;
    this.hint = hint;
  }

  @Override
  public T get() {
    return Utils.getComponent(role, hint);
  }

}
