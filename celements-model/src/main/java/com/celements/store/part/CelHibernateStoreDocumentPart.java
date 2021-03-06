package com.celements.store.part;

import static com.google.common.base.MoreObjects.*;
import static com.google.common.base.Preconditions.*;
import static com.xpn.xwiki.XWikiException.*;

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
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.classes.ClassDefinition;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ReferenceSerializationMode;
import com.celements.store.CelHibernateStore;
import com.celements.web.classes.oldcore.XWikiGroupsClass;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

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
      throws XWikiException, HibernateException {
    validateWikis(doc, context);
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

      DocumentReference existingDocRef = checkExistingDoc(doc, session);
      if (existingDocRef == null) {
        session.save(doc);
      } else if (existingDocRef.equals(doc.getDocumentReference())) {
        session.update(doc);
        // TODO: this is slower!! How can it be improved?
        // session.saveOrUpdate(doc);
      } else {
        LOGGER.error("saveXWikiDoc - collision detected: existing doc '{}' and new doc '{}'",
            existingDocRef, doc.getDocumentReference());
        throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC,
            "saveXWikiDoc - collision detected");
      }

      if (doc.getTranslation() == 0) {
        deleteAndSaveXObjects(doc, context);
      }

      if (context.getWiki().hasBacklinks(context)) {
        store.saveLinks(doc, context, true);
      }

      commit = true;
      doc.setNew(false);
      // We need to ensure that the saved document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }

  }

  private DocumentReference checkExistingDoc(XWikiDocument doc, Session session) {
    DocumentReference existingDocRef = null;
    Query query = session.createQuery("select fullName from XWikiDocument where id = :id");
    query.setLong("id", doc.getId());
    String existingDocFN = (String) query.uniqueResult();
    if (existingDocFN != null) {
      existingDocRef = store.getModelUtils().resolveRef(existingDocFN, DocumentReference.class,
          doc.getDocumentReference());
    }
    return existingDocRef;
  }

  private void deleteAndSaveXObjects(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    if ((doc.getXObjectsToRemove() != null) && (doc.getXObjectsToRemove().size() > 0)) {
      for (BaseObject removedObject : doc.getXObjectsToRemove()) {
        store.deleteXWikiObject(removedObject, context, false);
      }
      doc.setXObjectsToRemove(new ArrayList<BaseObject>());
    }
    if (doc.hasElement(XWikiDocument.HAS_OBJECTS)) {
      for (BaseObject obj : XWikiObjectEditor.on(doc).fetch().iter()) {
        store.saveXWikiCollection(obj, context, false);
      }
    }
  }

  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context)
      throws XWikiException, HibernateException {
    validateWikis(doc, context);
    boolean bTransaction = true;
    try {
      ImmutableDocumentReference immutableDocRef = new ImmutableDocumentReference(
          doc.getDocumentReference());

      doc.setStore(store);
      store.checkHibernate(context);
      SessionFactory sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
      bTransaction = bTransaction && store.beginTransaction(sfactory, false, context);
      Session session = store.getSession(context);
      session.setFlushMode(FlushMode.MANUAL);

      session.load(doc, new Long(doc.getId()));
      validateLoadedDoc(doc, immutableDocRef);
      doc.setNew(false);
      doc.setMostRecent(true);

      // Loading the attachment list
      if (doc.hasElement(XWikiDocument.HAS_ATTACHMENTS)) {
        store.loadAttachmentList(doc, context, false);
      }

      // TODO: handle the case where there are no xWikiClass and xWikiObject in the Database
      BaseClass bclass = new BaseClass();
      String cxml = doc.getXClassXML();
      if (cxml != null) {
        bclass.fromXML(cxml);
        bclass.setDocumentReference(immutableDocRef);
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
          if (!loadedObject.getDocumentReference().equals(immutableDocRef)) {
            LOGGER.warn("loadXWikiDoc - skipping obj [{}], doc [{}] not matching", loadedObject,
                store.getModelUtils().serializeRef(immutableDocRef));
            continue;
          }
          BaseObject object = copyToNewXObject(doc, loadedObject, context);
          if (isGroupsObject(object)) {
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

      doc.setContentDirty(false);
      // FIXME can be set to 'false' after CELDEV-784, see CELDEV-785
      doc.setMetaDataDirty(true);
      // We need to ensure that the loaded document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } catch (ObjectNotFoundException e) { // document doesn't exist
      doc.setNew(true);
    } finally {
      if (bTransaction) {
        store.endTransaction(context, false);
      }
    }
    return doc;
  }

  private void validateLoadedDoc(XWikiDocument doc, ImmutableDocumentReference immutableDocRef)
      throws XWikiException {
    if (!doc.getDocumentReference().equals(immutableDocRef)) {
      LOGGER.error("loadXWikiDoc - collision detected: loading doc '{}' returned doc '{}'",
          immutableDocRef, doc.getDocumentReference());
      throw new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
          "loadXWikiDoc - collision detected");
    }
    // ensure document reference immutability
    doc.setDocumentReference(immutableDocRef);
    // convert java.sql.Timestamp to java.util.Date
    doc.setDate(new Date(doc.getDate().getTime()));
    doc.setCreationDate(new Date(doc.getCreationDate().getTime()));
    doc.setContentUpdateDate(new Date(doc.getContentUpdateDate().getTime()));
  }

  @SuppressWarnings("unchecked")
  private Iterator<BaseObject> loadXObjects(XWikiDocument doc, XWikiContext context) {
    String hql = "from BaseObject as obj where obj.name = :name order by obj.className, obj.number";
    Query query = store.getSession(context).createQuery(hql);
    query.setText("name", serialize(doc));
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
    object.setDocumentReference(doc.getDocumentReference());
    object.setNumber(loadedObject.getNumber());
    object.setGuid(loadedObject.getGuid());
    object.setId(loadedObject.getId(), loadedObject.getIdVersion());
    return object;
  }

  private boolean isGroupsObject(BaseObject object) {
    ClassReference classRef = new ClassReference(object.getXClassReference());
    return classRef.equals(getXWikiGroupsClass().getClassReference());
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
    query.setText("name", serialize(doc));
    Iterator<?> dataIter = query.iterate();
    while (dataIter.hasNext()) {
      Object[] row = (Object[]) dataIter.next();
      groupObjs.get(row[0]).setStringValue("member", (String) row[1]);
    }
  }

  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context)
      throws XWikiException, HibernateException {
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
        throw new XWikiException(MODULE_XWIKI_STORE,
            ERROR_XWIKI_STORE_HIBERNATE_CANNOT_DELETE_UNLOADED_DOC,
            "Impossible to delete document if it is not loaded: " + doc.getDocumentReference());
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

      if (doc.getTranslation() == 0) {
        for (BaseObject object : XWikiObjectEditor.on(doc).fetch().iter().append(firstNonNull(
            doc.getXObjectsToRemove(), Collections.<BaseObject>emptyList()))) {
          store.deleteXWikiObject(object, context, false);
        }
      }

      context.getWiki().getVersioningStore().deleteArchive(doc, false, context);
      session.delete(doc);
      commit = true;
      // We need to ensure that the deleted document becomes the original document
      doc.setOriginalDocument(doc.clone());
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }
  }

  private ClassDefinition getXWikiGroupsClass() {
    return Utils.getComponent(ClassDefinition.class, XWikiGroupsClass.CLASS_DEF_HINT);
  }

  private void validateWikis(XWikiDocument doc, XWikiContext context) {
    WikiReference docWiki = doc.getDocumentReference().getWikiReference();
    WikiReference providedContextWiki = new WikiReference(context.getDatabase());
    WikiReference executionContextWiki = store.getModelContext().getWikiRef();
    checkArgument(docWiki.equals(providedContextWiki) && docWiki.equals(executionContextWiki),
        "wikis not matching for doc [%s], providedContextWiki [%s], executionContextWiki [%s]",
        doc.getDocumentReference(), providedContextWiki, executionContextWiki);
  }

  private String serialize(XWikiDocument doc) {
    return store.getModelUtils().serializeRef(doc.getDocumentReference(),
        ReferenceSerializationMode.LOCAL);
  }

}
