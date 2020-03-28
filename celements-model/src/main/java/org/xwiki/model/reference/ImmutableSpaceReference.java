package org.xwiki.model.reference;

import static com.google.common.base.Preconditions.*;

import javax.annotation.concurrent.Immutable;

import org.xwiki.model.EntityType;

@Immutable
public class ImmutableSpaceReference extends SpaceReference {

  private static final long serialVersionUID = 1992234242982281626L;

  private boolean initialised = false;

  public ImmutableSpaceReference(EntityReference reference) {
    super(reference);
    initialised = true;
  }

  public ImmutableSpaceReference(String spaceName, WikiReference parent) {
    super(spaceName, parent);
    initialised = true;
  }

  ImmutableSpaceReference(EntityReference reference, DocumentReference child) {
    super(reference);
    setChild(child);
    initialised = true;
  }

  private void checkInit() {
    checkState(!initialised, "unable to modify already initialised instance");
  }

  @Override
  public void setName(String name) {
    checkInit();
    super.setName(name);
  }

  @Override
  public void setParent(EntityReference parent) {
    checkInit();
    super.setParent(parent);
    parent = new ImmutableWikiReference(parent, this);
    ParentSetter.set(this, parent);
  }

  @Override
  public void setChild(EntityReference child) {
    checkInit();
    super.setChild(child);
  }

  @Override
  public void setType(EntityType type) {
    checkInit();
    super.setType(type);
  }

  @Override
  public ImmutableSpaceReference clone() {
    return this;
  }

}
