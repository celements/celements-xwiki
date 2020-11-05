package com.celements.model.object;

import static com.celements.common.MoreObjectsCel.*;
import static com.celements.model.access.IModelAccessFacade.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;

import java.util.Optional;
import java.util.function.Predicate;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.DocumentReference;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.restriction.ClassRestriction;
import com.celements.model.object.restriction.FieldRestriction;
import com.celements.model.object.restriction.ObjectQueryBuilder;

@NotThreadSafe
public abstract class AbstractObjectHandler<R extends AbstractObjectHandler<R, D, O>, D, O> extends
    ObjectQueryBuilder<R, O> implements ObjectHandler<D, O> {

  private final D doc;
  private D transDoc;

  public AbstractObjectHandler(@NotNull D doc) {
    this.doc = checkNotNull(doc);
  }

  @Override
  public D getDocument() {
    return doc;
  }

  @Override
  public Optional<D> getTranslationDoc() {
    return Optional.ofNullable(transDoc);
  }

  @Override
  public R withTranslation(D transDoc) {
    if (transDoc != null) {
      checkArgument(getBridge().getDocRef(transDoc).equals(getDocRef()));
    }
    this.transDoc = transDoc;
    return getThis();
  }

  @Override
  public DocumentReference getDocRef() {
    return getBridge().getDocRef(getDocument());
  }

  @Override
  protected R filterInternal(Predicate<O> restriction) {
    tryCast(restriction, ClassRestriction.class)
        .filter(classRestr -> getTranslationDoc().isPresent())
        .ifPresent(this::filterObjectLangIfPresent);
    return super.filterInternal(restriction);
  }

  private void filterObjectLangIfPresent(ClassRestriction<O> restriction) {
    restriction.getClassIdentity().getClassDefinition()
        .filter(ClassIdentity::isValidObjectClass)
        .flatMap(ClassDefinition::getLangField)
        .ifPresent(langField -> super.filterInternal(
            new FieldRestriction<>(getBridge(), langField, getObjectLanguage())));
  }

  private String getObjectLanguage() {
    return getTranslationDoc()
        .map(getBridge()::getLanguage)
        .filter(not(DEFAULT_LANG::equals))
        .orElseGet(() -> getBridge().getDefaultLanguage(getDocument()));
  }

  @Override
  protected abstract @NotNull ObjectBridge<D, O> getBridge();

  @Override
  public abstract AbstractObjectHandler<?, D, O> clone();

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + " [doc=" + getDocRef() + ", query=" + getQuery() + "]";
  }

}
