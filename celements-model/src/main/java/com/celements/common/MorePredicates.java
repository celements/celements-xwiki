package com.celements.common;

import static com.google.common.base.Preconditions.*;

import com.google.common.base.Predicate;

public class MorePredicates {

  private MorePredicates() {}

  public static Predicate<String> stringNotEmptyPredicate() {
    return STRING_NOT_EMPTY_PREDICATE;
  }

  private static final Predicate<String> STRING_NOT_EMPTY_PREDICATE = str -> !checkNotNull(str)
      .isEmpty();

  public static Predicate<String> stringNotBlankPredicate() {
    return STRING_NOT_BLANK_PREDICATE;
  }

  private static final Predicate<String> STRING_NOT_BLANK_PREDICATE = str -> !checkNotNull(str)
      .trim().isEmpty();

}
