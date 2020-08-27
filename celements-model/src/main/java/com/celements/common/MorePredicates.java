package com.celements.common;

import static com.google.common.base.Preconditions.*;

public final class MorePredicates {

  private MorePredicates() {}

  public static com.google.common.base.Predicate<String> stringNotEmptyPredicate() {
    return str -> !checkNotNull(str).isEmpty();
  }

  public static com.google.common.base.Predicate<String> stringNotBlankPredicate() {
    return str -> !checkNotNull(str).trim().isEmpty();
  }

}
