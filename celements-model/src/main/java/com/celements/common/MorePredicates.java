package com.celements.common;

import static com.google.common.base.Preconditions.*;

import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import com.celements.common.function.ForkPredicate;

public final class MorePredicates {

  private MorePredicates() {}

  /**
   * @deprecated since 5.2, instead use lambda directly
   */
  @Deprecated
  public static com.google.common.base.Predicate<String> stringNotEmptyPredicate() {
    return str -> !checkNotNull(str).isEmpty();
  }

  /**
   * @deprecated since 5.2, instead use lambda directly
   */
  @Deprecated
  public static com.google.common.base.Predicate<String> stringNotBlankPredicate() {
    return str -> !checkNotNull(str).trim().isEmpty();
  }

  /**
   * @see ForkPredicate
   */
  @NotNull
  public static <T> ForkPredicate<T> fork(@Nullable Predicate<T> when) {
    return new ForkPredicate<>(when);
  }

}
