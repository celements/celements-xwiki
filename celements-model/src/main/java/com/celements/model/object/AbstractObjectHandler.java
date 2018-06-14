package com.celements.model.object;

import static com.google.common.base.Preconditions.*;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

import com.celements.model.field.FieldAccessor;
import com.celements.model.object.restriction.ObjectQueryBuilder;

@NotThreadSafe
public abstract class AbstractObjectHandler<R extends AbstractObjectHandler<R, D, O>, D, O> extends
    ObjectQueryBuilder<R, O> implements ObjectHandler<D, O> {

  private final D doc;

  public AbstractObjectHandler(@NotNull D doc) {
    this.doc = checkNotNull(doc);
    getBridge().checkDoc(doc);
  }

  @Override
  public D getDocument() {
    return doc;
  }

  @Override
  public DocumentReference getDocRef() {
    return getBridge().getDocRef(getDocument());
  }

  @Override
  protected abstract @NotNull ObjectBridge<D, O> getBridge();

  protected abstract @NotNull FieldAccessor<O> getFieldAccessor();

  @Override
  public abstract AbstractObjectHandler<?, D, O> clone();

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [doc=" + getDocRef() + ", query=" + getQuery() + "]";
  }

}
