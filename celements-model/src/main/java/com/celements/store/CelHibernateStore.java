package com.celements.store;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableDocumentReference;

import com.celements.model.util.ModelUtils;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.UniqueHashIdComputer;
import com.celements.store.part.CelHibernateStoreCollectionPart;
import com.celements.store.part.CelHibernateStoreDocumentPart;
import com.celements.store.part.CelHibernateStorePropertyPart;
import com.google.common.collect.Iterables;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.XWikiHibernateStore;

@Singleton
@Component
public class CelHibernateStore extends XWikiHibernateStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  @Requirement(UniqueHashIdComputer.NAME)
  private CelementsIdComputer idComputer;

  @Requirement
  private ModelUtils modelUtils;

  private final CelHibernateStoreDocumentPart documentStorePart;
  private final CelHibernateStoreCollectionPart collectionStorePart;
  private final CelHibernateStorePropertyPart propertyStorePart;

  public CelHibernateStore() {
    super();
    documentStorePart = new CelHibernateStoreDocumentPart(this);
    collectionStorePart = new CelHibernateStoreCollectionPart(this);
    propertyStorePart = new CelHibernateStorePropertyPart(this);
  }

  // TODO CELDEV-625 - CelHibernateStore reference mutation
  @Override
  public void saveXWikiDoc(XWikiDocument doc, final XWikiContext context,
      final boolean bTransaction) throws XWikiException {
    // XWikiHibernateStore.saveXWikiDoc requires a mutable docRef
    DocRefMutabilityExecutor<Void> exec = new DocRefMutabilityExecutor<Void>() {

      @Override
      public Void call(XWikiDocument doc) throws XWikiException {
        documentStorePart.saveXWikiDoc(doc, context, bTransaction);
        return null;
      }
    };
    exec.execute(doc);
  }

  public CelementsIdComputer getIdComputer() {
    return idComputer;
  }

  // TODO CELDEV-625 - CelHibernateStore reference mutation
  // TODO CELDEV-531 - improve load performance
  @Override
  public XWikiDocument loadXWikiDoc(XWikiDocument doc, final XWikiContext context)
      throws XWikiException {
    // XWikiHibernateStore.loadXWikiDoc requires a mutable docRef
    DocRefMutabilityExecutor<XWikiDocument> exec = new DocRefMutabilityExecutor<XWikiDocument>() {

      @Override
      public XWikiDocument call(XWikiDocument doc) throws XWikiException {
        return documentStorePart.loadXWikiDoc(doc, context);
      }
    };
    return exec.execute(doc);
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    documentStorePart.deleteXWikiDoc(doc, context);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    collectionStorePart.saveXWikiCollection(object, context, bTransaction);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void loadXWikiCollection(BaseCollection object1, XWikiDocument doc, XWikiContext context,
      boolean bTransaction, boolean alreadyLoaded) throws XWikiException {
    collectionStorePart.loadXWikiCollection(object1, doc, context, bTransaction, alreadyLoaded);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void deleteXWikiCollection(BaseCollection object, XWikiContext context,
      boolean bTransaction, boolean evict) throws XWikiException {
    collectionStorePart.deleteXWikiCollection(object, context, bTransaction, evict);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void loadXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    propertyStorePart.loadXWikiProperty(property, context, bTransaction);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiProperty(final PropertyInterface property, final XWikiContext context,
      final boolean runInOwnTransaction) throws XWikiException {
    propertyStorePart.saveXWikiProperty(property, context, runInOwnTransaction);
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("Celements doesn't support class tables");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public BaseClass loadXWikiClass(BaseClass bclass, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    throw new UnsupportedOperationException("Celements doesn't support class tables");
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiClassProperty(PropertyClass property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    throw new UnsupportedOperationException("Celements doesn't support class tables");
  }

  public ModelUtils getModelUtils() {
    return modelUtils;
  }

  /**
   * DocRefMutabilityExecutor is used to execute code for an {@link XWikiDocument} within
   * {@link #call(XWikiDocument)} with a mutable {@link DocumentReference} injected. it will be set
   * immutable again after execution.
   */
  private abstract class DocRefMutabilityExecutor<T> {

    public T execute(XWikiDocument doc) throws XWikiException {
      try {
        injectMutableDocRef(doc);
        return call(doc);
      } finally {
        injectImmutableDocRef(doc);
      }
    }

    protected abstract T call(XWikiDocument doc) throws XWikiException;

    private void injectMutableDocRef(XWikiDocument doc) {
      injectRef(doc, new DocumentReference(doc.getDocumentReference()));
    }

    private void injectImmutableDocRef(XWikiDocument doc) {
      DocumentReference docRef = new ImmutableDocumentReference(doc.getDocumentReference());
      injectRefInDocAndObjects(doc, docRef);
    }

    private void injectRefInDocAndObjects(XWikiDocument doc, DocumentReference docRef) {
      injectRef(doc, docRef);
      // inject reference in objects
      for (BaseObject obj : Iterables.concat(doc.getXObjects().values())) {
        if (obj != null) {
          obj.setDocumentReference(docRef);
        }
      }
      // inject reference in parent doc
      if (doc.getOriginalDocument() != null) {
        injectRefInDocAndObjects(doc.getOriginalDocument(), docRef);
      }
    }

    @SuppressWarnings("deprecation")
    private void injectRef(XWikiDocument doc, DocumentReference docRef) {
      boolean metaDataDirty = doc.isMetaDataDirty();
      // set invalid docRef first to circumvent equals check in setDocumentReference
      doc.setDocumentReference(new DocumentReference("$", "$", "$"));
      doc.setDocumentReference(docRef);
      doc.setMetaDataDirty(metaDataDirty); // is set true by setDocumentReference
    }

  }

}
