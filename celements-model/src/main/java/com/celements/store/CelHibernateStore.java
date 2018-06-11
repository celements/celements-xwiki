package com.celements.store;

import static com.xpn.xwiki.XWikiException.*;

import java.text.MessageFormat;

import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableDocumentReference;

import com.celements.logging.LogLevel;
import com.celements.logging.LogUtils;
import com.celements.model.context.ModelContext;
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

  @Requirement
  private ModelContext modelContext;

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
        log(LogLevel.INFO, "saveXWikiDoc - start", doc);
        documentStorePart.saveXWikiDoc(doc, context, bTransaction);
        log(LogLevel.INFO, "saveXWikiDoc - end", doc);
        return null;
      }
    };
    try {
      exec.execute(doc);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC);
    } catch (Exception exc) {
      logError("saveXWikiDoc - error", doc, exc);
      throw exc;
    }
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
        log(LogLevel.INFO, "loadXWikiDoc - start", doc);
        documentStorePart.loadXWikiDoc(doc, context);
        log(LogLevel.INFO, "loadXWikiDoc - end", doc);
        return doc;
      }
    };
    try {
      return exec.execute(doc);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_READING_DOC);
    } catch (Exception exc) {
      logError("loadXWikiDoc - error", doc, exc);
      throw exc;
    }
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      log(LogLevel.INFO, "deleteXWikiDoc - start", doc);
      documentStorePart.deleteXWikiDoc(doc, context);
      log(LogLevel.INFO, "deleteXWikiDoc - end", doc);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("deleteXWikiDoc - failed", doc, exc,
          ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC);
    } catch (Exception exc) {
      logError("deleteXWikiDoc - error", doc, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiCollection(BaseCollection object, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      log(LogLevel.DEBUG, "saveXObject - start", object);
      collectionStorePart.saveXWikiCollection(object, context, bTransaction);
      log(LogLevel.DEBUG, "saveXObject - end", object);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT);
    } catch (Exception exc) {
      logError("saveXObject - error", object, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void loadXWikiCollection(BaseCollection object, XWikiDocument doc, XWikiContext context,
      boolean bTransaction, boolean alreadyLoaded) throws XWikiException {
    try {
      log(LogLevel.DEBUG, "loadXObject - start", object);
      collectionStorePart.loadXWikiCollection(object, doc, context, bTransaction, alreadyLoaded);
      log(LogLevel.DEBUG, "loadXObject - end", object);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT);
    } catch (Exception exc) {
      logError("loadXObject - error", object, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void deleteXWikiCollection(BaseCollection object, XWikiContext context,
      boolean bTransaction, boolean evict) throws XWikiException {
    try {
      log(LogLevel.DEBUG, "deleteXObject - start", object);
      collectionStorePart.deleteXWikiCollection(object, context, bTransaction, evict);
      log(LogLevel.DEBUG, "deleteXObject - end", object);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("deleteXObject - failed", object, exc,
          ERROR_XWIKI_STORE_HIBERNATE_DELETING_OBJECT);
    } catch (Exception exc) {
      logError("deleteXObject - error", object, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void loadXWikiProperty(PropertyInterface property, XWikiContext context,
      boolean bTransaction) throws XWikiException {
    try {
      log(LogLevel.TRACE, "loadXProperty - start", property);
      propertyStorePart.loadXWikiProperty(property, context, bTransaction);
      log(LogLevel.TRACE, "loadXProperty - end", property);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("loadXProperty - failed", property, exc,
          ERROR_XWIKI_STORE_HIBERNATE_LOADING_OBJECT);
    } catch (Exception exc) {
      logError("loadXProperty - error", property, exc);
      throw exc;
    }
  }

  /**
   * @deprecated This is internal to XWikiHibernateStore and may be removed in the future.
   */
  @Override
  @Deprecated
  public void saveXWikiProperty(final PropertyInterface property, final XWikiContext context,
      final boolean runInOwnTransaction) throws XWikiException {
    try {
      log(LogLevel.TRACE, "saveXProperty - start", property);
      propertyStorePart.saveXWikiProperty(property, context, runInOwnTransaction);
      log(LogLevel.TRACE, "saveXProperty - end", property);
    } catch (HibernateException | XWikiException exc) {
      throw newXWikiException("saveXProperty - failed", property, exc,
          ERROR_XWIKI_STORE_HIBERNATE_SAVING_OBJECT);
    } catch (Exception exc) {
      logError("saveXProperty - error", property, exc);
      throw exc;
    }
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

  public void log(LogLevel level, String msg, Object obj) {
    if (LogUtils.isLevelEnabled(LOGGER, level)) {
      LogUtils.log(LOGGER, level, buildLogMessage(msg, obj));
    }
  }

  public void logError(String msg, Object obj, Throwable cause) {
    LOGGER.error(buildLogMessage(msg, obj), cause);
  }

  public XWikiException newXWikiException(String msg, Object obj, Throwable cause, int code) {
    return new XWikiException(MODULE_XWIKI_STORE, code, buildLogMessage(msg, obj), cause);
  }

  public String buildLogMessage(String msg, Object obj) {
    if (obj instanceof XWikiDocument) {
      XWikiDocument doc = (XWikiDocument) obj;
      return MessageFormat.format("{0}: {1} {2}", msg, Long.toString(doc.getId()),
          modelUtils.serializeRef(doc.getDocumentReference()));
    } else if (obj instanceof PropertyInterface) {
      PropertyInterface property = (PropertyInterface) obj;
      return MessageFormat.format("{0}: {1} {2}", msg, Long.toString(property.getId()), property);
    } else {
      return MessageFormat.format("{0}: {1}", msg, obj);
    }
  }

  public ModelUtils getModelUtils() {
    return modelUtils;
  }

  public ModelContext getModelContext() {
    return modelContext;
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
