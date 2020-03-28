package org.xwiki.model.reference;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import org.xwiki.model.EntityType;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.PseudoClassDefinition;
import com.celements.model.context.ModelContext;
import com.google.common.base.Splitter;
import com.xpn.xwiki.web.Utils;

@Immutable
public class ClassReference extends ImmutableEntityReference
    implements ImmutableReference, ClassIdentity {

  private static final long serialVersionUID = -8664491352611685779L;

  public ClassReference(EntityReference reference) {
    super(reference);
  }

  public ClassReference(String spaceName, String className) {
    super(className, EntityType.DOCUMENT, new EntityReference(spaceName, EntityType.SPACE));
  }

  public ClassReference(String fullName) {
    this(extractPart(fullName, 0), extractPart(fullName, 1));
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
    return new ImmutableDocumentReference(getName(), new SpaceReference(getParentName(), wikiRef));
  }

  @Override
  public boolean isValidObjectClass() {
    return !PseudoClassDefinition.CLASS_SPACE.equals(getParentName());
  }

  @Override
  public String serialize() {
    return getParentName() + "." + getName();
  }

  // is called in static context, do not use ModelUtils
  private static String extractPart(String fullName, int i) {
    List<String> parts = Splitter.on('.').omitEmptyStrings()
        .splitToList(fullName.substring(fullName.indexOf(':') + 1));
    checkArgument(parts.size() > i, "illegal class fullName [{0}]", fullName);
    return parts.get(i);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClassDefinition) {
      obj = ((ClassDefinition) obj).getClassReference();
    }
    return super.equals(obj);
  }

  private static ModelContext getModelContext() {
    return Utils.getComponent(ModelContext.class);
  }

}
