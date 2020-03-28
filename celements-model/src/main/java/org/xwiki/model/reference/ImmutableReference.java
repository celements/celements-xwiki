package org.xwiki.model.reference;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import com.celements.model.util.ReferenceSerializationMode;

public interface ImmutableReference {

  @NotNull
  EntityReference getMutable();

  @NotEmpty
  String serialize(@NotNull ReferenceSerializationMode mode);

}
