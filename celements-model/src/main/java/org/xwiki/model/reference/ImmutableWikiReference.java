package org.xwiki.model.reference;

import static com.celements.model.util.References.*;
import static java.text.MessageFormat.*;

import javax.annotation.concurrent.Immutable;

import org.xwiki.model.EntityType;

import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.xpn.xwiki.web.Utils;

@Immutable
public class ImmutableWikiReference extends WikiReference implements ImmutableReference {

  private static final long serialVersionUID = -4557859208406583286L;

  private boolean initialised = false;

  public ImmutableWikiReference(EntityReference reference) {
    super(reference);
    initialised = true;
  }

  public ImmutableWikiReference(String wikiName) {
    super(wikiName);
    initialised = true;
  }

  ImmutableWikiReference(EntityReference reference, SpaceReference child) {
    super(reference);
    setChild(child);
    initialised = true;
  }

  private void checkInit() {
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
  public EntityReference getParent() {
    return null;
  }

  @Override
  public void setParent(EntityReference parent) {
    throw new UnsupportedOperationException();
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
  public void setType(EntityType type) {
    checkInit();
    super.setType(type);
  }

  @Override
  public ImmutableWikiReference clone() {
    return this;
  }

  @Override
  public WikiReference getMutable() {
    WikiReference ret = new WikiReference(this);
    ret.setChild(getChild());
    return ret;
  }

  @Override
  public String serialize(ReferenceSerializationMode mode) {
    return getModelUtils().serializeRef(this, mode);
  }

  private static final ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
