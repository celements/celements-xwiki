package com.celements.model.field;

import javax.validation.constraints.NotNull;

public interface Field<T> {

  @NotNull
  String getName();

  @NotNull
  Class<T> getType();

}
