package com.celements.store.part;

import static com.google.common.base.MoreObjects.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.util.Util;

//TODO CELDEV-626 - CelHibernateStore refactoring
public class CelHibernateStoreDocumentPart {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelHibernateStore.class);

  private final CelHibernateStore store;

  public CelHibernateStoreDocumentPart(CelHibernateStore store) {
    this.store = Preconditions.checkNotNull(store);
  }

  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    logXWikiDoc("saveXWikiDoc - start", doc);
    boolean commit = false;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
      }

      XWikiDocument origDoc;
      if (getXObjectFetcher(doc).exists()) {
        origDoc = getPrimaryStore(context).loadXWikiDoc(doc, context);
        Session session = store.getSession(context);
        if (session != null) {
          session.clear();
        }
      } else {
        origDoc = new XWikiDocument(doc.getDocumentReference());
      }

      doc.setStore(store);
      // Make sure the database name is stored
      doc.getDocumentReference().setWikiReference(new WikiReference(context.getDatabase()));

      if (bTransaction) {
        store.checkHibernate(context);
        SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
        bTransaction = store.beginTransaction(sfactory, context);
      }
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.COMMIT);

      // These informations will allow to not look for attachments and objects on loading
      doc.setElement(XWikiDocument.HAS_ATTACHMENTS, (doc.getAttachmentList().size() != 0));
      doc.setElement(XWikiDocument.HAS_OBJECTS, getXObjectFetcher(doc).exists());

      // Let's update the class XML since this is the new way to store it
      // TODO If all the properties are removed, the old xml stays?
      setBaseClass(doc, context);

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

      prepareXObjects(doc, origDoc);
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

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }

    logXWikiDoc("saveXWikiDoc - end", doc);
  }

  private void setBaseClass(XWikiDocument doc, XWikiContext context) {
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

  private void deleteAndSaveXObjects(XWikiDocument doc, XWikiContext context) throws XWikiException,
      IdComputationException {
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

  private void prepareXObjects(XWikiDocument doc, XWikiDocument origDoc)
      throws IdComputationException, XWikiException {
    for (BaseObject obj : getXObjectFetcher(doc).iter()) {
      obj.setDocumentReference(doc.getDocumentReference());
      if (Strings.isNullOrEmpty(obj.getGuid())) {
        obj.setGuid(UUID.randomUUID().toString());
      }
      if (!obj.hasValidId()) {
        Optional<BaseObject> existingObj = XWikiObjectFetcher.on(origDoc).filter(new ClassReference(
            obj.getXClassReference())).filter(obj.getNumber()).first();
        if (existingObj.isPresent() && existingObj.get().hasValidId()) {
          obj.setId(existingObj.get().getId(), existingObj.get().getIdVersion());
          LOGGER.debug("saveXWikiDoc - obj [{}] already existed, keeping id", obj);
        } else {
          long nextId = store.getIdComputer().computeNextObjectId(doc);
          obj.setId(nextId, store.getIdComputer().getIdVersion());
          LOGGER.debug("saveXWikiDoc - obj [{}] is new, computed new id", obj);
        }
      }
    }
  }

  private Optional<BaseObject> fetchExistingObject(XWikiDocument doc, BaseObject obj,
      XWikiContext context) throws XWikiException {
    if (getPrimaryStore(context).exists(doc, context)) {
      XWikiDocument origDoc = getPrimaryStore(context).loadXWikiDoc(doc, context);
      store.getSession(context).clear();
      return XWikiObjectFetcher.on(origDoc).filter(new ClassReference(
          obj.getXClassReference())).filter(obj.getNumber()).first();
    } else {
      return Optional.absent();
    }
  }

  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    logXWikiDoc("loadXWikiDoc - start", doc);
    // To change body of implemented methods use Options | File Templates.
    boolean bTransaction = true;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
      }
      doc.setStore(store);
      store.checkHibernate(context);

      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = bTransaction && store.beginTransaction(sfactory, false, context);
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.MANUAL);

      try {
        session.load(doc, new Long(doc.getId()));
        doc.setDatabase(context.getDatabase());
        doc.setNew(false);
        doc.setMostRecent(true);
        // Fix for XWIKI-1651
        doc.setDate(new Date(doc.getDate().getTime()));
        doc.setCreationDate(new Date(doc.getCreationDate().getTime()));
        doc.setContentUpdateDate(new Date(doc.getContentUpdateDate().getTime()));
      } catch (ObjectNotFoundException e) { // No document
        doc.setNew(true);
        return doc;
      }

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
        boolean hasGroups = false;
        EntityReference localGroupEntityReference = new EntityReference("XWikiGroups",
            EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));
        DocumentReference groupsDocumentReference = new DocumentReference(context.getDatabase(),
            localGroupEntityReference.getParent().getName(), localGroupEntityReference.getName());

        Query query = store.getSession(context).createQuery(
            "from BaseObject as obj where obj.name = :name order by obj.className, obj.number");
        query.setText("name", store.getModelUtils().serializeRefLocal(doc.getDocumentReference()));
        @SuppressWarnings("unchecked")
        Iterator<BaseObject> objIter = query.iterate();
        while (objIter.hasNext()) {
          BaseObject object = objIter.next();
          DocumentReference classReference = object.getXClassReference();

          // It seems to search before is case insensitive. And this would break the loading if we
          // get an object which doesn't really belong to this document
          if (!object.getDocumentReference().equals(doc.getDocumentReference())) {
            LOGGER.warn("loadXWikiDoc - skipping obj [{}], doc [{}] not matching", object,
                store.getModelUtils().serializeRef(doc.getDocumentReference()));
            continue;
          }

          BaseObject newobject;
          if (classReference.equals(doc.getDocumentReference())) {
            newobject = bclass.newCustomClassInstance(context);
          } else {
            newobject = BaseClass.newCustomClassInstance(classReference, context);
          }
          if (newobject != null) {
            newobject.setId(object.getId(), object.getIdVersion());
            newobject.setClassName(object.getClassName());
            newobject.setDocumentReference(object.getDocumentReference());
            newobject.setNumber(object.getNumber());
            newobject.setGuid(object.getGuid());
            object = newobject;
          }

          if (classReference.equals(groupsDocumentReference)) {
            // Groups objects are handled differently.
            hasGroups = true;
          } else {
            store.loadXWikiCollection(object, doc, context, false, true);
          }
          doc.setXObject(object.getNumber(), object);
        }

        // AFAICT this was added as an emergency patch because loading of objects has proven
        // too slow and the objects which cause the most overhead are the XWikiGroups objects
        // as each group object (each group member) would otherwise cost 2 database queries.
        // This will do every group member in a single query.
        if (hasGroups) {
          Query query2 = session.createQuery(
              "select bobject.number, prop.value from StringProperty as prop, "
                  + "BaseObject as bobject where bobject.name = :name and bobject.className='XWiki.XWikiGroups' "
                  + "and bobject.id=prop.id.id and prop.id.name='member' order by bobject.number");
          query2.setText("name", doc.getFullName());
          Iterator<?> it2 = query2.list().iterator();
          while (it2.hasNext()) {
            Object[] result = (Object[]) it2.next();
            Integer number = (Integer) result[0];
            String member = (String) result[1];
            BaseObject obj = BaseClass.newCustomClassInstance(groupsDocumentReference, context);
            obj.setDocumentReference(doc.getDocumentReference());
            obj.setXClassReference(localGroupEntityReference);
            obj.setNumber(number.intValue());
            obj.setStringValue("member", member);
            doc.setXObject(obj.getNumber(), obj);
          }
        }
      }

      // We need to ensure that the loaded document becomes the original document
      doc.setOriginalDocument(doc.clone());
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

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }

    logXWikiDoc("loadXWikiDoc - end", doc);
    return doc;
  }

  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    logXWikiDoc("deleteXWikiDoc - start", doc);
    boolean bTransaction = false;
    boolean commit = false;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
      }
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

      // End monitoring timer
      if (monitor != null) {
        monitor.endTimer("hibernate");
      }
    }
    logXWikiDoc("deleteXWikiDoc - end", doc);
  }

  private XWikiObjectFetcher getXObjectFetcher(XWikiDocument doc) {
    return XWikiObjectEditor.on(doc).fetch();
  }

  /**
   * @return the cache store if one is configured, else it's self referencing
   */
  private XWikiStoreInterface getPrimaryStore(XWikiContext context) {
    return context.getWiki().getStore();
  }

  private void logXWikiDoc(String msg, XWikiDocument doc) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(msg + ": {} {}", doc.getId(), store.getModelUtils().serializeRef(
          doc.getDocumentReference()));
    }
  }

}
