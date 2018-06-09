package com.celements.store.part;

import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.XWikiException.*;

import java.util.UUID;

import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.XWikiDocumentCreator;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.References;
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
import com.xpn.xwiki.web.Utils;

@Immutable
class DocumentSavePreparationCommand {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;

  DocumentSavePreparationCommand(CelHibernateStore store) {
    this.store = checkNotNull(store);
  }

  void execute(XWikiDocument doc, XWikiContext context) throws XWikiException {
    doc.setStore(store);
    ensureDatabaseConsistency(doc, context);
    if (doc.getTranslation() == 0) {
      updateBaseClassXml(doc, context);
      doc.setElement(XWikiDocument.HAS_OBJECTS, XWikiObjectFetcher.on(doc).exists());
      doc.setElement(XWikiDocument.HAS_ATTACHMENTS, (doc.getAttachmentList().size() != 0));
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
  }

  private void ensureDatabaseConsistency(XWikiDocument doc, XWikiContext context) {
    WikiReference wikiRef = new WikiReference(context.getDatabase());
    if (!wikiRef.equals(doc.getDocumentReference().getWikiReference())) {
      LOGGER.warn("saveXWikiDoc - not matching wiki, adjusting to [{}] for doc [{}]", wikiRef,
          doc.getDocumentReference(), new Throwable());
      boolean isMetaDataDirty = doc.isMetaDataDirty();
      doc.setDocumentReference(References.adjustRef(doc.getDocumentReference(),
          DocumentReference.class, wikiRef));
      doc.setMetaDataDirty(isMetaDataDirty);
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
          Optional<BaseObject> existingObj = XWikiObjectFetcher.on(origDoc).filter(
              new ClassReference(obj.getXClassReference())).filter(obj.getNumber()).first();
          if (existingObj.isPresent() && existingObj.get().hasValidId()) {
            obj.setId(existingObj.get().getId(), existingObj.get().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] already existed, keeping id", obj);
          } else {
            long nextId = store.getIdComputer().computeNextObjectId(doc);
            obj.setId(nextId, store.getIdComputer().getIdVersion());
            LOGGER.debug("saveXWikiDoc - obj [{}] is new, computed new id", obj);
            logExistingObject(existingObj.orNull());
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
