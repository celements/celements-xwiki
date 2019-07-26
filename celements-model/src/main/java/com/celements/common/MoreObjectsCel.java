package com.celements.common;

import java.util.Optional;

public final class MoreObjectsCel {

  public static <F, T> Optional<T> tryCast(F candidate, Class<T> targetClass) {
    return targetClass.isInstance(candidate)
        ? Optional.of(targetClass.cast(candidate))
        : Optional.empty();
  }

}
