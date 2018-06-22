package com.celements.configuration;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.xwiki.configuration.ConfigurationSource;

import com.celements.common.MorePredicates;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;

public class ConfigSourceUtils {

  private ConfigSourceUtils() {
  }

  public static @NotNull List<String> getStringListProperty(@NotNull ConfigurationSource configSrc,
      @NotNull String key) {
    FluentIterable<?> values;
    Object prop = configSrc.getProperty(key);
    if (prop instanceof Iterable) {
      values = FluentIterable.from((Iterable<?>) prop);
    } else if (prop != null) {
      values = FluentIterable.of(prop);
    } else {
      values = FluentIterable.of();
    }
    return values.filter(Predicates.notNull()).transform(Functions.toStringFunction()).filter(
        MorePredicates.stringNotBlankPredicate()).toList();
  }

}
