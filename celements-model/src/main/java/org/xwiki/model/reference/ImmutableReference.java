package org.xwiki.model.reference;

import javax.validation.constraints.NotNull;

public interface ImmutableReference {

  @NotNull
  EntityReference getMutable();

}
