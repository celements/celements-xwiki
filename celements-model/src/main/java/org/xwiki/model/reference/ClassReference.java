package org.xwiki.model.reference;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import javax.annotation.concurrent.Immutable;

import org.xwiki.model.EntityType;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.xpn.xwiki.web.Utils;

@Immutable
public class ClassReference extends ImmutableEntityReference implements ImmutableReference, ClassIdentity {

  private static final long serialVersionUID = -8664491352611685779L;

  public ClassReference(EntityReference reference) {
    super(reference);
  }

  public ClassReference(String spaceName, String className) {
    super(className, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE));
  }

  @Override
  public void setParent(EntityReference parent) {
    checkInit();
    checkArgument((parent != null) && (parent.getType() == EntityType.SPACE),
        defer(() -> "Invalid parent reference [" + parent + "] for a class reference"));
    super.setParent(parent);
  }

  @Override
  public void setType(EntityType type) {
    checkArgument(type == EntityType.DOCUMENT,
        defer(() -> "Invalid type [" + type + "] for a class reference"));
    super.setType(type);
  }

  @Override
  public DocumentReference getDocRef() {
    return getDocRef(getModelContext().getWikiRef());
  }

  @Override
  public DocumentReference getDocRef(WikiReference wikiRef) {
    return new ImmutableDocumentReference(getName(), new SpaceReference(getParent().getName(),
        wikiRef));
  }

  @Override
  public boolean isValidObjectClass() {
    return !PseudoClassDefinition.CLASS_SPACE.equals(getParent().getName());
  }

  @Override
  public String serialize() {
    return serialize(ReferenceSerializationMode.COMPACT_WIKI);
  }

  public String serialize(ReferenceSerializationMode mode) {
    return getModelUtils().serializeRef(this, mode);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassDefinition) {
      obj = ((ClassDefinition) obj).getClassReference();
    }
    return super.equals(obj);
  }

  private static ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  private static ModelContext getModelContext() {
    return Utils.getComponent(ModelContext.class);
  }

}
