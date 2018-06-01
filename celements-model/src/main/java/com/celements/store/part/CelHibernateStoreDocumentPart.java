package com.celements.store.part;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

//TODO CELDEV-626 - CelHibernateStore refactoring
public class CelHibernateStoreDocumentPart {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;
  private final DocumentSavePreparationCommand savePrepCmd;

  public CelHibernateStoreDocumentPart(CelHibernateStore store) {
    this.store = checkNotNull(store);
    this.savePrepCmd = new DocumentSavePreparationCommand(store);
  }

  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    boolean commit = false;
    try {
      Session session = savePrepCmd.execute(doc, bTransaction, context);

      if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
        store.saveAttachmentList(doc, context, false);
      }

      // Handle the latest text file
      if (doc.isContentDirty() || doc.isMetaDataDirty()) {
        Date ndate = new Date();
        doc.setDate(ndate);
        if (doc.isContentDirty()) {
          doc.setContentUpdateDate(ndate);
          doc.setContentAuthor(doc.getAuthor());
        }
        doc.incrementVersion();
        if (context.getWiki().hasVersioning(context)) {
          context.getWiki().getVersioningStore().updateXWikiDocArchive(doc, false, context);
        }

        doc.setContentDirty(false);
        doc.setMetaDataDirty(false);
      } else {
        if (doc.getDocumentArchive() != null) {
          // Let's make sure we save the archive if we have one
          // This is especially needed if we load a document from XML
          if (context.getWiki().hasVersioning(context)) {
            context.getWiki().getVersioningStore().saveXWikiDocArchive(doc.getDocumentArchive(),
                false, context);
          }
        } else {
          // Make sure the getArchive call has been made once
          // with a valid context
          try {
            if (context.getWiki().hasVersioning(context)) {
              doc.getDocumentArchive(context);
            }
          } catch (XWikiException xwe) {
            LOGGER.debug("saveXWikiDoc - this is a non critical error: {} {}", doc.getId(),
                store.getModelUtils().serializeRef(doc.getDocumentReference()), xwe);
          }
        }
      }

      // Verify if the document already exists
      Query query = session.createQuery(
          "select xwikidoc.id from XWikiDocument as xwikidoc where xwikidoc.id = :id");
      query.setLong("id", doc.getId());
      if (query.uniqueResult() == null) {
        session.save(doc);
      } else {
        session.update(doc);
        // TODO: this is slower!! How can it be improved?
        // session.saveOrUpdate(doc);
      }

      deleteAndSaveXObjects(doc, context);

      if (context.getWiki().hasBacklinks(context)) {
        store.saveLinks(doc, context, true);
      }

      commit = true;
      doc.setNew(false);
      // We need to ensure that the saved document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } catch (HibernateException | XWikiException | IdComputationException exc) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "Exception while saving document:"
              + doc.getDocumentReference(), exc);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, commit);
        }
      } catch (HibernateException exc) {
        LOGGER.error("saveXWikiDoc - failed {} for {}", (commit ? "commit" : "rollback"),
            doc.getDocumentReference(), exc);
      }
    }

  }

  private void deleteAndSaveXObjects(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    if ((doc.getXObjectsToRemove() != null) && (doc.getXObjectsToRemove().size() > 0)) {
      for (BaseObject removedObject : doc.getXObjectsToRemove()) {
        store.deleteXWikiObject(removedObject, context, false);
      }
      doc.setXObjectsToRemove(new ArrayList<BaseObject>());
    }
    for (BaseObject obj : getXObjectFetcher(doc).iter()) {
      store.saveXWikiCollection(obj, context, false);
    }
  }

  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    // To change body of implemented methods use Options | File Templates.
    boolean bTransaction = true;
    try {
      doc.setStore(store);
      store.checkHibernate(context);

      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = bTransaction && store.beginTransaction(sfactory, false, context);
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.MANUAL);

      session.load(doc, new Long(doc.getId()));
      doc.setDatabase(context.getDatabase());
      doc.setNew(false);
      doc.setMostRecent(true);
      // convert java.sql.Timestamp to java.util.Date
      doc.setDate(new Date(doc.getDate().getTime()));
      doc.setCreationDate(new Date(doc.getCreationDate().getTime()));
      doc.setContentUpdateDate(new Date(doc.getContentUpdateDate().getTime()));

      // Loading the attachment list
      if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
        store.loadAttachmentList(doc, context, false);
      }

      // TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
      BaseClass bclass = new BaseClass();
      String cxml = doc.getXClassXML();
      if (cxml != null) {
        bclass.fromXML(cxml);
        bclass.setDocumentReference(doc.getDocumentReference());
        doc.setXClass(bclass);
      }
      // Store this XWikiClass in the context so that we can use it in case of recursive usage
      // of classes
      context.addBaseClass(bclass);

      if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
        Iterator<BaseObject> objIter = loadXObjects(doc, context);
        Map<Integer, BaseObject> groupObjs = new HashMap<>();
        while (objIter.hasNext()) {
          BaseObject loadedObject = objIter.next();
          if (!loadedObject.getDocumentReference().equals(doc.getDocumentReference())) {
            LOGGER.warn("loadXWikiDoc - skipping obj [{}], doc [{}] not matching", loadedObject,
                store.getModelUtils().serializeRef(doc.getDocumentReference()));
            continue;
          }
          BaseObject object = copyToNewXObject(doc, loadedObject, context);
          if (object.getXClassReference().equals(getXWikiGroupsClassDocRef(context))) {
            // Groups objects are handled differently.
            groupObjs.put(object.getNumber(), object);
          } else {
            store.loadXWikiCollection(object, doc, context, false, true);
          }
          doc.setXObject(object.getNumber(), object);
        }
        if (groupObjs.size() > 0) {
          loadFieldsForGroupObjects(doc, groupObjs, context);
        }
      }

      // We need to ensure that the loaded document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } catch (ObjectNotFoundException e) { // document doesn't exist
      doc.setNew(true);
    } catch (HibernateException | XWikiException exc) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
          "Exception while reading document: " + doc.getDocumentReference(), exc);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, false);
        }
      } catch (HibernateException exc) {
        LOGGER.error("loadXWikiDoc - failed rollback for {}", doc.getDocumentReference(), exc);
      }
    }
    return doc;
  }

  @SuppressWarnings("unchecked")
  private Iterator<BaseObject> loadXObjects(XWikiDocument doc, XWikiContext context) {
    String hql = "from BaseObject as obj where obj.name = :name order by obj.className, obj.number";
    Query query = store.getSession(context).createQuery(hql);
    query.setText("name", store.getModelUtils().serializeRefLocal(doc.getDocumentReference()));
    return query.iterate();
  }

  private BaseObject copyToNewXObject(XWikiDocument doc, BaseObject loadedObject,
      XWikiContext context) throws XWikiException {
    BaseObject object;
    if (loadedObject.getXClassReference().equals(doc.getDocumentReference())) {
      object = doc.getXClass().newCustomClassInstance(context);
    } else {
      object = BaseClass.newCustomClassInstance(loadedObject.getXClassReference(), context);
    }
    object.setXClassReference(loadedObject.getXClassReference());
    object.setDocumentReference(new DocumentReference(doc.getDocumentReference()));
    object.setNumber(loadedObject.getNumber());
    object.setGuid(loadedObject.getGuid());
    object.setId(loadedObject.getId(), loadedObject.getIdVersion());
    return object;
  }

  // AFAICT this was added as an emergency patch because loading of objects has proven too slow and
  // the objects which cause the most overhead are the XWikiGroups objects as each group object
  // (each group member) would otherwise cost 2 database queries. This will do every group member in
  // a single query.
  private void loadFieldsForGroupObjects(XWikiDocument doc, Map<Integer, BaseObject> groupObjs,
      XWikiContext context) {
    String hql = "select obj.number, prop.value from StringProperty as prop, BaseObject as obj "
        + "where obj.name = :name and obj.className = 'XWiki.XWikiGroups' and obj.id = prop.id.id "
        + "and prop.id.name = 'member'";
    Query query = store.getSession(context).createQuery(hql);
    query.setText("name", store.getModelUtils().serializeRefLocal(doc.getDocumentReference()));
    Iterator<?> dataIter = query.iterate();
    while (dataIter.hasNext()) {
      Object[] row = (Object[]) dataIter.next();
      groupObjs.get(row[0]).setStringValue("member", (String) row[1]);
    }
  }

  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    validateWikis(doc, context);
    boolean bTransaction = false;
    boolean commit = false;
    try {
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = store.beginTransaction(sfactory, context);
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.COMMIT);

      if (doc.getStore() == null) {
        Object[] args = { doc.getFullName() };
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
            "Impossible to delete document {0} if it is not loaded", null, args);
      }

      // Let's delete any attachment this document might have
      for (XWikiAttachment attachment : doc.getAttachmentList()) {
        context.getWiki().getAttachmentStore().deleteXWikiAttachment(attachment, false, context,
            false);
      }

      // deleting XWikiLinks
      if (context.getWiki().hasBacklinks(context)) {
        store.deleteLinks(doc.getId(), context, true);
      }

      for (BaseObject object : getXObjectFetcher(doc).iter().append(firstNonNull(
          doc.getXObjectsToRemove(), Collections.<BaseObject>emptyList()))) {
        store.deleteXWikiObject(object, context, false);
      }
      context.getWiki().getVersioningStore().deleteArchive(doc, false, context);

      session.delete(doc);
      commit = true;
      // We need to ensure that the deleted document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } catch (HibernateException | XWikiException exc) {
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC,
          "Exception while deleting document: " + doc.getDocumentReference(), exc);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, commit);
        }
      } catch (HibernateException exc) {
        LOGGER.error("deleteXWikiDoc - failed {} for {}", (commit ? "commit" : "rollback"),
            doc.getDocumentReference(), exc);
      }
    }
  }

  private DocumentReference getXWikiGroupsClassDocRef(XWikiContext context) {
    return new DocumentReference(context.getDatabase(), "XWiki", "XWikiGroups");
  }

  private XWikiObjectFetcher getXObjectFetcher(XWikiDocument doc) {
    return XWikiObjectEditor.on(doc).fetch();
  }

  private void validateWikis(XWikiDocument doc, XWikiContext context) {
    WikiReference docWiki = doc.getDocumentReference().getWikiReference();
    WikiReference providedContextWiki = new WikiReference(context.getDatabase());
    WikiReference executionContextWiki = store.getModelContext().getWikiRef();
    checkArgument(docWiki.equals(providedContextWiki) && docWiki.equals(executionContextWiki),
        "wikis not matching for doc [%s], providedContextWiki [%s], executionContextWiki [%s]",
        doc.getDocumentReference(), providedContextWiki, executionContextWiki);
  }

}
