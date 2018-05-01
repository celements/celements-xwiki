package com.celements.store.part;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.hibernate.FlushMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
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
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
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
      doc.setElement(XWikiDocument.HAS_OBJECTS, (doc.getXObjects().size() != 0));

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
          } catch (XWikiException e) {
            // this is a non critical error
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

      if (bTransaction) {
        store.endTransaction(context, true);
      }

      doc.setNew(false);

      // We need to ensure that the saved document becomes the original document
      doc.setOriginalDocument(doc.clone());

    } catch (Exception e) {
      Object[] args = { doc.getDocumentReference() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
          "Exception while saving document {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, false);
        }
      } catch (Exception e) {
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
    prepareXObjects(doc);
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

  private void prepareXObjects(XWikiDocument doc) throws IdComputationException {
    for (BaseObject obj : getXObjectFetcher(doc).iter()) {
      obj.setDocumentReference(doc.getDocumentReference());
      if (Strings.isNullOrEmpty(obj.getGuid())) {
        obj.setGuid(UUID.randomUUID().toString());
      }
      if (!obj.hasValidId()) {
        long nextId = store.getIdComputer().computeNextObjectId(doc);
        obj.setId(nextId, store.getIdComputer().getIdVersion());
        LOGGER.debug("saveXWikiDoc - computed id [{}]", obj.getId());
      }
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
        Query query = session.createQuery(
            "from BaseObject as bobject where bobject.name = :name order by " + "bobject.number");
        query.setText("name", doc.getFullName());
        Iterator<BaseObject> it = query.list().iterator();

        EntityReference localGroupEntityReference = new EntityReference("XWikiGroups",
            EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));
        DocumentReference groupsDocumentReference = new DocumentReference(context.getDatabase(),
            localGroupEntityReference.getParent().getName(), localGroupEntityReference.getName());

        boolean hasGroups = false;
        while (it.hasNext()) {
          BaseObject object = it.next();
          DocumentReference classReference = object.getXClassReference();

          if (classReference == null) {
            continue;
          }

          // It seems to search before is case insensitive. And this would break the loading if we
          // get an
          // object which doesn't really belong to this document
          if (!object.getDocumentReference().equals(doc.getDocumentReference())) {
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

      if (bTransaction) {
        store.endTransaction(context, false, false);
      }
    } catch (Exception e) {
      Object[] args = { doc.getDocumentReference() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
          "Exception while reading document {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, false, false);
        }
      } catch (Exception e) {
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
    boolean bTransaction = true;
    MonitorPlugin monitor = Util.getMonitorPlugin(context);
    try {
      // Start monitoring timer
      if (monitor != null) {
        monitor.startTimer("hibernate");
      }
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = bTransaction && store.beginTransaction(sfactory, context);
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.COMMIT);

      if (doc.getStore() == null) {
        Object[] args = { doc.getFullName() };
        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
            "Impossible to delete document {0} if it is not loaded", null, args);
      }

      // Let's delete any attachment this document might have
      List attachlist = doc.getAttachmentList();
      for (int i = 0; i < attachlist.size(); i++) {
        XWikiAttachment attachment = (XWikiAttachment) attachlist.get(i);
        context.getWiki().getAttachmentStore().deleteXWikiAttachment(attachment, false, context,
            false);
      }

      // deleting XWikiLinks
      if (context.getWiki().hasBacklinks(context)) {
        store.deleteLinks(doc.getId(), context, true);
      }

      // Find the list of classes for which we have an object
      // Remove properties planned for removal
      if (doc.getObjectsToRemove().size() > 0) {
        for (int i = 0; i < doc.getObjectsToRemove().size(); i++) {
          BaseObject bobj = doc.getObjectsToRemove().get(i);
          if (bobj != null) {
            store.deleteXWikiObject(bobj, context, false);
          }
        }
        doc.setObjectsToRemove(new ArrayList<BaseObject>());
      }
      for (List<BaseObject> objects : doc.getXObjects().values()) {
        for (BaseObject obj : objects) {
          if (obj != null) {
            store.deleteXWikiObject(obj, context, false);
          }
        }
      }
      context.getWiki().getVersioningStore().deleteArchive(doc, false, context);

      session.delete(doc);

      // We need to ensure that the deleted document becomes the original document
      doc.setOriginalDocument(doc.clone());

      if (bTransaction) {
        store.endTransaction(context, true);
      }
    } catch (Exception e) {
      Object[] args = { doc.getDocumentReference() };
      throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
          XWikiException.ERROR_XWIKI_STORE_HIBERNATE_DELETING_DOC,
          "Exception while deleting document {0}", e, args);
    } finally {
      try {
        if (bTransaction) {
          store.endTransaction(context, false);
        }
      } catch (Exception e) {
        LOGGER.error("failed commit/rollback for {}", doc, e);
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

  private void logXWikiDoc(String msg, XWikiDocument doc) {
    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(msg + ": {} {}", doc.getId(), store.getModelUtils().serializeRef(
          doc.getDocumentReference()));
    }
  }

}
