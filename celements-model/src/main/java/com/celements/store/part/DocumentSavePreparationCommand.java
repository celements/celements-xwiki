package com.celements.store.part;

import static com.celements.common.MoreFunctions.*;
import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;
import static com.xpn.xwiki.XWikiException.*;

import java.util.Optional;
import java.util.UUID;

import javax.annotation.concurrent.Immutable;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.XWikiDocumentCreator;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

@Immutable
class DocumentSavePreparationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final XWikiDocument doc;
  private final CelHibernateStore store;
  private final XWikiContext context;

  private Boolean hasExistingDoc;

  DocumentSavePreparationCommand(XWikiDocument doc, CelHibernateStore store, XWikiContext context) {
    this.doc = checkNotNull(doc);
    this.store = checkNotNull(store);
    this.context = checkNotNull(context);
  }

  Session execute(boolean bTransaction) throws HibernateException, XWikiException {
    doc.setStore(store);
    ensureDatabaseConsistency(doc, context);
    Session session = prepareSession(doc, bTransaction, context);
    if (!doc.hasValidId()) {
      computeAndSetId(doc, session);
    }
    if (doc.getTranslation() == 0) {
      updateBaseClassXml(doc, context);
      doc.setElement(XWikiDocument.HAS_OBJECTS, XWikiObjectFetcher.on(doc).exists());
      doc.setElement(XWikiDocument.HAS_ATTACHMENTS, !doc.getAttachmentList().isEmpty());
      try {
        new XObjectPreparer(doc, context).execute();
      } catch (IdComputationException exc) {
        throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
            "unable to compute id", exc);
      }
    } else {
      doc.setElement(XWikiDocument.HAS_OBJECTS, false);
      doc.setElement(XWikiDocument.HAS_ATTACHMENTS, false);
    }
    return session;
  }

  private Session prepareSession(XWikiDocument doc, boolean bTransaction, XWikiContext context)
      throws XWikiException {
    if (bTransaction) {
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      store.beginTransaction(sfactory, context);
    }
    Session session = store.getSession(context);
    session.setFlushMode(FlushMode.COMMIT);
    return session;
  }

  private void ensureDatabaseConsistency(XWikiDocument doc, XWikiContext context) {
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    if (!wikiRef.equals(doc.getDocumentReference().getWikiReference())) {
      LOGGER.warn("saveXWikiDoc - not matching wiki, adjusting to [{}] for doc [{}]", wikiRef,
          doc.getDocumentReference(), new Throwable());
      boolean isMetaDataDirty = doc.isMetaDataDirty();
      doc.setDocumentReference(RefBuilder.from(doc.getDocumentReference()).with(wikiRef)
          .build(DocumentReference.class));
      doc.setMetaDataDirty(isMetaDataDirty);
    }
  }

  private void computeAndSetId(XWikiDocument doc, Session session) throws XWikiException {
    try {
      long docId;
      byte collisionCount = -1;
      do {
        collisionCount++;
        docId = store.getIdComputer().computeDocumentId(
            doc.getDocumentReference(), doc.getLanguage(), collisionCount);
      } while (existsDifferentDoc(doc, docId, collisionCount, session));
      doc.setId(docId, store.getIdComputer().getIdVersion());
      LOGGER.debug("saveXWikiDoc - computed doc id [{}] for [{}]", doc.getId(), doc);
    } catch (IdComputationException exc) {
      String msg = format("unable to compute id for doc [{}] with id [{}]", doc, doc.getId()).get();
      throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
          msg, exc);
    }
  }

  boolean existsDifferentDoc(XWikiDocument doc, long docId, byte collisionCount, Session session) {
    DocumentReference docRef = doc.getDocumentReference();
    Optional<Object> existing = Optional.ofNullable(session
        .createQuery("select fullName from XWikiDocument where id = :id")
        .setLong("id", docId)
        .uniqueResult());
    hasExistingDoc = existing.isPresent();
    return existing
        .filter(not(store.getModelUtils().serializeRefLocal(docRef)::equals))
        .map(asFunction(existingFN -> LOGGER.warn("saveXWikiDoc - collision detected: "
            + "doc [{}] with existing doc [{}] for id [{}] and collisionCount [{}]",
            store.getModelUtils().serializeRef(docRef), existingFN, docId, collisionCount)))
        .isPresent();
  }

  boolean hasExistingDoc() {
    if (hasExistingDoc != null) {
      return hasExistingDoc;
    } else {
      throw new IllegalStateException("execute command first");
    }
  }

  private void updateBaseClassXml(XWikiDocument doc, XWikiContext context) {
    BaseClass bclass = doc.getXClass();
    if (bclass != null) {
      bclass.setDocumentReference(doc.getDocumentReference());
      if (bclass.getFieldList().size() > 0) {
        doc.setXClassXML(bclass.toXMLString());
      } else {
        doc.setXClassXML("");
      }
      // Store this XWikiClass in the context in case of recursive usage of classes
      context.addBaseClass(bclass);
    }
  }

  private class XObjectPreparer {

    private final XWikiDocument doc;
    private final XWikiDocument origDoc;

    XObjectPreparer(XWikiDocument doc, XWikiContext context) throws XWikiException {
      this.doc = doc;
      this.origDoc = loadOriginalDocument(context);
    }

    private XWikiDocument loadOriginalDocument(XWikiContext context) throws XWikiException {
      XWikiDocument dummyDoc = getDocCreator().createWithoutDefaults(doc.getDocumentReference());
      // XXX do not check doc.isNew() here, it is not reliably set. see [CELDEV-701]
      if (doc.hasElement(XWikiDocument.HAS_OBJECTS) && getPrimaryStore(context).exists(dummyDoc,
          context)) {
        return getPrimaryStore(context).loadXWikiDoc(dummyDoc, context);
      }
      return dummyDoc;
    }

    void execute() throws IdComputationException {
      for (BaseObject obj : XWikiObjectEditor.on(doc).fetch().iter()) {
        obj.setDocumentReference(doc.getDocumentReference());
        if (Strings.isNullOrEmpty(obj.getGuid())) {
          obj.setGuid(UUID.randomUUID().toString());
        }
        if (!obj.hasValidId()) {
          Optional<BaseObject> existingObj = XWikiObjectFetcher.on(origDoc)
              .filter(new ClassReference(obj.getXClassReference()))
              .filter(obj.getNumber())
              .stream().findFirst();
          if (existingObj.isPresent() && existingObj.get().hasValidId()) {
            obj.setId(existingObj.get().getId(), existingObj.get().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] already existed, keeping id", obj);
          } else {
            long nextId = store.getIdComputer().computeNextObjectId(doc);
            obj.setId(nextId, store.getIdComputer().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] is new, computed new id", obj);
            logExistingObject(existingObj.orElse(null));
          }
        }
      }
    }
  }

  private void logExistingObject(BaseObject existingObj) {
    if (existingObj != null) {
      // observed in com.xpn.xwiki.web.ObjectAddAction, see [CELDEV-693]
      LOGGER.warn("saveXWikiDoc - overwriting existing object [{}] because of invalid id, "
          + "possibly due to cache poisoning before save through 'XWiki#getDocument': {}",
          existingObj, existingObj.toXMLString(), new Throwable());
    }
  }

  private XWikiDocumentCreator getDocCreator() {
    return Utils.getComponent(XWikiDocumentCreator.class);
  }

  /**
   * @return the cache store if one is configured, else it's self referencing
   */
  private XWikiStoreInterface getPrimaryStore(XWikiContext context) {
    return context.getWiki().getStore();
  }

}
