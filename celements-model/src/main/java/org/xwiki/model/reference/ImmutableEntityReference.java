package org.xwiki.model.reference;

import static com.celements.model.util.References.*;
import static java.text.MessageFormat.*;

import javax.annotation.concurrent.Immutable;

import org.xwiki.model.EntityType;

@Immutable
public class ImmutableEntityReference extends EntityReference implements ImmutableReference {

  private static final long serialVersionUID = 4196990820112451663L;

  private boolean initialised = false;

  public ImmutableEntityReference(EntityReference reference) {
    super(reference.getName(), reference.getType(), reference.getParent());
    setChild(reference.getChild());
    initialised = true;
  }

  public ImmutableEntityReference(String name, EntityType type, EntityReference parent) {
    super(name, type, parent);
    initialised = true;
  }

  protected final void checkInit() {
    if (initialised) {
      throw new IllegalStateException(format("unable to modify already initialised {0}: {1}",
          this.getClass().getSimpleName(), this));
    }
  }

  @Override
  public void setName(String name) {
    checkInit();
    super.setName(name);
  }

  @Override
  public void setType(EntityType type) {
    checkInit();
    super.setType(type);
  }

  @Override
  public EntityReference getParent() {
    return super.getParent() != null ? super.getParent().clone() : null;
  }

  @Override
  public void setParent(EntityReference parent) {
    checkInit();
    if (parent != null) {
      super.setParent(cloneRef(parent));
    }
  }

  @Override
  public EntityReference getChild() {
    return super.getChild() != null ? super.getChild().clone() : null;
  }

  @Override
  public void setChild(EntityReference child) {
    checkInit();
    if (child != null) {
      super.setChild(cloneRef(child));
    }
  }

  @Override
  public ImmutableEntityReference clone() {
    return this;
  }

  @Override
  public EntityReference getMutable() {
    EntityReference ret = new EntityReference(getName(), getType(), getParent());
    ret.setChild(getChild());
    return ret;
  }

}
