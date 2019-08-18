package org.xwiki.model.reference;

import static com.celements.model.util.References.*;
import static com.google.common.base.Preconditions.*;

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

@Immutable
public class ObjectReference implements Serializable {

  private static final long serialVersionUID = 2638847219120582547L;

  private final ImmutableDocumentReference docRef;
  private final ClassReference classRef;
  private final int objNb;

  public ObjectReference(DocumentReference docRef, ClassReference classRef, int objNb) {
    this.docRef = cloneRef(docRef, ImmutableDocumentReference.class);
    this.classRef = classRef;
    this.objNb = objNb;
    checkArgument(objNb >= 0);
  }

  public DocumentReference getDocumentReference() {
    return docRef;
  }

  public ClassReference getClassReference() {
    return classRef;
  }

  public int getObjNb() {
    return objNb;
  }

}
