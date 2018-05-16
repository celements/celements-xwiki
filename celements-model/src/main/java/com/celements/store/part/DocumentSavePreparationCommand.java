package com.celements.store.part;

import static com.celements.model.util.References.*;
import static com.google.common.base.Preconditions.*;

import java.text.MessageFormat;
import java.util.List;
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

import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;

@Immutable
class DocumentSavePreparationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;

  DocumentSavePreparationCommand(CelHibernateStore store) {
    this.store = checkNotNull(store);
  }

  Session execute(XWikiDocument doc, boolean bTransaction, XWikiContext context)
      throws HibernateException, XWikiException, IdComputationException {
    doc.setStore(store);
    // Make sure the database name is stored
    doc.getDocumentReference().setWikiReference(new WikiReference(context.getDatabase()));
    // Let's update the class XML since this is the new way to store it
    updateBaseClassXml(doc, context);
    // These informations will allow to not look for objects and attachments on saving
    doc.setElement(XWikiDocument.HAS_OBJECTS, XWikiObjectFetcher.on(doc).exists());
    doc.setElement(XWikiDocument.HAS_ATTACHMENTS, (doc.getAttachmentList().size() != 0));
    // prepare object ids
    new XObjectPreparer(doc, context).execute();
    // begin transaction
    if (bTransaction) {
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      store.beginTransaction(sfactory, context);
    }
    Session session = store.getSession(context);
    session.setFlushMode(FlushMode.COMMIT);
    return session;
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
      XWikiDocument origDoc = new XWikiDocument(cloneRef(doc.getDocumentReference(),
          DocumentReference.class));
      origDoc.setLanguage(doc.getLanguage());
      if (!doc.isNew() && doc.hasElement(XWikiDocument.HAS_OBJECTS) && getPrimaryStore(
          context).exists(origDoc, context)) {
        origDoc = getPrimaryStore(context).loadXWikiDoc(origDoc, context);
      }
      logOrigDocObjects(origDoc);
      return origDoc;
    }

    private void logOrigDocObjects(XWikiDocument origDoc) {
      if (LOGGER.isDebugEnabled()) {
        List<BaseObject> objects = XWikiObjectFetcher.on(origDoc).list();
        LOGGER.debug("saveXWikiDoc - for {} doc fetched {} existing objects", (doc.isNew() ? "new"
            : "existing"), objects.size());
        if (LOGGER.isTraceEnabled()) {
          for (BaseObject obj : objects) {
            LOGGER.trace("saveXWikiDoc - loaded existing obj [{}]", obj);
          }
        }
      }
    }

    void execute() throws IdComputationException {
      for (BaseObject obj : XWikiObjectEditor.on(doc).fetch().iter()) {
        obj.setDocumentReference(doc.getDocumentReference());
        if (Strings.isNullOrEmpty(obj.getGuid())) {
          obj.setGuid(UUID.randomUUID().toString());
        }
        if (!obj.hasValidId()) {
          Optional<BaseObject> existingObj = XWikiObjectFetcher.on(origDoc).filter(
              new ClassReference(obj.getXClassReference())).filter(obj.getNumber()).first();
          if (!existingObj.isPresent()) {
            long nextId = store.getIdComputer().computeNextObjectId(doc);
            obj.setId(nextId, store.getIdComputer().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] is new, computed new id", obj);
          } else if (existingObj.get().hasValidId()) {
            obj.setId(existingObj.get().getId(), existingObj.get().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] already existed, keeping id", obj);
          } else {
            throw new IdComputationException(MessageFormat.format("saveXWikiDoc - unable to set id "
                + "for obj [{0}] because existingObj [{1}] has invalid id", obj,
                existingObj.get()));
          }
        }
      }
    }

  }

  /**
   * @return the cache store if one is configured, else it's self referencing
   */
  private XWikiStoreInterface getPrimaryStore(XWikiContext context) {
    return context.getWiki().getStore();
  }

}
