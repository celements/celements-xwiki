package com.celements.model.access;

import static com.celements.logging.LogUtils.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.exception.AttachmentNotExistsException;
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.access.exception.ModelAccessRuntimeException;
import com.celements.model.access.exception.TranslationCreateException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.ClassIdentity;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.CustomClassField;
import com.celements.model.context.ModelContext;
import com.celements.model.object.ObjectFetcher;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ClassFieldValue;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.ReferenceSerializationMode;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.rights.access.exceptions.NoAccessRightsException;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.util.Util;

@Component
public class DefaultModelAccessFacade implements IModelAccessFacade {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelAccessFacade.class);

  @Requirement
  protected ModelAccessStrategy strategy;

  @Requirement
  protected IRightsAccessFacadeRole rightsAccess;

  @Requirement
  protected ModelUtils modelUtils;

  @Requirement
  protected ModelContext context;

  @Override
  public XWikiDocument getDocument(DocumentReference docRef) throws DocumentNotExistsException {
    return getDocument(docRef, DEFAULT_LANG);
  }

  @Override
  public XWikiDocument getDocument(DocumentReference docRef, String lang)
      throws DocumentNotExistsException {
    return cloneDoc(getDocumentReadOnly(docRef, lang));
  }

  @Override
  public Document getApiDocument(XWikiDocument doc) throws NoAccessRightsException {
    if (rightsAccess.hasAccessLevel(doc.getDocumentReference(), EAccessLevel.VIEW)) {
      return doc.newDocument(context.getXWikiContext());
    }
    throw new NoAccessRightsException(doc.getDocumentReference(), context.getUser(),
        EAccessLevel.VIEW);
  }

  /**
   * CAUTION: never ever change anything on the returned XWikiDocument, because it is the object in
   * cache. Thus the same object will be returned for the following requests. If you change this
   * object, concurrent request might get a partially modified object, or worse, if an error occurs
   * during the save (or no save call happens), the cached object will not reflect the actual
   * document at all.
   *
   * @param docRef
   * @param lang
   * @return an xwiki document for readonly usage
   * @throws DocumentNotExistsException
   */
  private XWikiDocument getDocumentReadOnly(DocumentReference docRef, String lang)
      throws DocumentNotExistsException {
    checkNotNull(docRef);
    XWikiDocument mainDoc = strategy.getDocument(docRef, DEFAULT_LANG);
    lang = normalizeLang(lang);
    if (lang.equals(DEFAULT_LANG) || lang.equals(mainDoc.getDefaultLanguage())) {
      return mainDoc; // return main doc if the requested language is the actual default language
    } else {
      XWikiDocument doc = strategy.getDocument(docRef, lang);
      if (!doc.isNew()) {
        return doc;
      } else {
        throw new DocumentNotExistsException(docRef);
      }
    }
  }

  @Override
  public XWikiDocument createDocument(DocumentReference docRef)
      throws DocumentAlreadyExistsException {
    return createDocument(docRef, DEFAULT_LANG);
  }

  private XWikiDocument createDocument(DocumentReference docRef, String lang)
      throws DocumentAlreadyExistsException {
    checkNotNull(docRef);
    lang = normalizeLang(lang);
    if (!exists(docRef, lang)) {
      return strategy.createDocument(docRef, lang);
    } else {
      throw new DocumentAlreadyExistsException(docRef, lang);
    }
  }

  @Override
  public XWikiDocument getOrCreateDocument(DocumentReference docRef) {
    return getOrCreateDocument(docRef, DEFAULT_LANG);
  }

  @Override
  public XWikiDocument getOrCreateDocument(DocumentReference docRef, String lang) {
    try {
      return getDocument(docRef, lang);
    } catch (DocumentNotExistsException exc) {
      return strategy.createDocument(docRef, lang);
    }
  }

  @Override
  public boolean exists(DocumentReference docRef) {
    return exists(docRef, DEFAULT_LANG);
  }

  @Override
  public boolean exists(DocumentReference docRef, String lang) {
    boolean exists = false;
    if (docRef != null) {
      lang = normalizeLang(lang);
      if (DEFAULT_LANG.equals(lang)) {
        exists = strategy.exists(docRef, DEFAULT_LANG);
      } else {
        return !strategy.getDocument(docRef, lang).isNew();
      }
    }
    return exists;
  }

  @Override
  public void saveDocument(XWikiDocument doc) throws DocumentSaveException {
    saveDocument(doc, "", false);
  }

  @Override
  public void saveDocument(XWikiDocument doc, String comment) throws DocumentSaveException {
    saveDocument(doc, comment, false);
  }

  @Override
  public void saveDocument(XWikiDocument doc, String comment, boolean isMinorEdit)
      throws DocumentSaveException {
    checkNotNull(doc);
    String username = context.getUserName();
    doc.setAuthor(username);
    if (doc.isNew()) {
      doc.setCreator(username);
    }
    sanitizeLangBeforeSave(doc);
    LOGGER.info("saveDocument: doc '{}, {}', comment '{}', isMinorEdit '{}'",
        serialize(doc.getDocumentReference()), doc.getLanguage(), comment, isMinorEdit);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("saveDocument: context db '{}' and StackTrace:",
          serialize(context.getWikiRef()), new Throwable());
    }
    strategy.saveDocument(doc, comment, isMinorEdit);
  }

  private void sanitizeLangBeforeSave(XWikiDocument doc) throws DocumentSaveException {
    if (doc.getDefaultLanguage().isEmpty()) {
      LOGGER.warn("sanitizeLangBeforeSave: default lang missing on doc [{}]",
          serialize(doc.getDocumentReference()));
      doc.setDefaultLanguage(context.getDefaultLanguage());
    }
    if (doc.getLanguage().equals(doc.getDefaultLanguage())) {
      LOGGER.warn("sanitizeLangBeforeSave: set lang equals default lang on doc [{}]",
          serialize(doc.getDocumentReference()));
      doc.setTranslation(0);
      doc.setLanguage(DEFAULT_LANG);
    } else if ((doc.getTranslation() == 0) && !doc.getLanguage().equals(DEFAULT_LANG)) {
      LOGGER.warn("sanitizeLangBeforeSave: lang [{}] set on main doc [{}]",
          doc.getLanguage(), serialize(doc.getDocumentReference()));
      doc.setLanguage(DEFAULT_LANG);
    } else if ((doc.getTranslation() != 0) && doc.getLanguage().equals(DEFAULT_LANG)) {
      throw new DocumentSaveException(doc.getDocumentReference(), doc.getLanguage(),
          "translation doc without set language");
    }
  }

  @Override
  public void deleteDocument(DocumentReference docRef, boolean totrash)
      throws DocumentDeleteException {
    try {
      deleteDocument(getDocument(docRef), totrash);
    } catch (DocumentNotExistsException exc) {
      LOGGER.debug("doc trying to delete does not exist '{}'", serialize(docRef), exc);
    } catch (ModelAccessRuntimeException exc) {
      throw new DocumentDeleteException(docRef, exc);
    }
  }

  @Override
  public void deleteDocument(XWikiDocument doc, boolean totrash) throws DocumentDeleteException {
    checkNotNull(doc);
    List<XWikiDocument> toDelDocs = new ArrayList<>();
    toDelDocs.addAll(getTranslations(doc.getDocumentReference()).values());
    toDelDocs.add(doc);
    for (XWikiDocument toDel : toDelDocs) {
      deleteDocumentWithoutTranslations(toDel, totrash);
    }
  }

  @Override
  public void deleteDocumentWithoutTranslations(XWikiDocument doc, boolean totrash)
      throws DocumentDeleteException {
    checkNotNull(doc);
    LOGGER.debug("deleteDocument: doc '{}, {}', totrash '{}'",
        serialize(doc.getDocumentReference()), doc.getLanguage(), totrash);
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("deleteDocument: context db '{}' and StackTrace:",
          serialize(context.getWikiRef()), new Throwable());
    }
    strategy.deleteDocument(doc, totrash);
  }

  @Override
  public XWikiDocument createTranslation(DocumentReference docRef, String lang)
      throws TranslationCreateException {
    if (!isMultiLingual()) {
      throw new TranslationCreateException(docRef, lang);
    }
    try {
      XWikiDocument mainDoc = getDocument(docRef);
      // TODO check if lang is valid already happening here?
      XWikiDocument transDoc = createDocument(docRef, lang);
      transDoc.setTranslation(1);
      transDoc.setDefaultLanguage(mainDoc.getDefaultLanguage());
      transDoc.setStore(mainDoc.getStore());
      transDoc.setContent(mainDoc.getContent());
      return transDoc;
    } catch (DocumentNotExistsException | DocumentAlreadyExistsException exc) {
      throw new TranslationCreateException(docRef, lang, exc);
    }
  }

  private boolean isMultiLingual() {
    return context.getXWikiContext().getWiki().isMultiLingual(context.getXWikiContext());
  }

  @Override
  public Map<String, XWikiDocument> getTranslations(DocumentReference docRef) {
    Map<String, XWikiDocument> transMap = new HashMap<>();
    for (String lang : strategy.getTranslations(docRef)) {
      lang = normalizeLang(lang);
      try {
        transMap.put(lang, getDocument(docRef, lang));
      } catch (DocumentNotExistsException exc) {
        LOGGER.error("failed to load existing translation '{}' for doc '{}'",
            lang, serialize(docRef), exc);
      }
    }
    return transMap;
  }

  @Override
  public boolean isTranslation(XWikiDocument doc) {
    return checkNotNull(doc).getTranslation() == 1;
  }

  /**
   * We need to clone this document first, since a cached storage would return the same object for
   * the following requests, so concurrent request might get a partially modified object, or worse,
   * if an error occurs during the save, the cached object will not reflect the actual document at
   * all.
   */
  private XWikiDocument cloneDoc(XWikiDocument doc) {
    if (doc.isFromCache()) {
      doc = doc.clone();
      // missing docRef clone in XWikiDocument.clone() doesn't matter since:
      // [CELDEV-522] Use ImmutableDocumentReference in DocumentBuilder
      doc.setFromCache(false);
    }
    return doc;
  }

  private String normalizeLang(String lang) {
    lang = Util.normalizeLanguage(lang);
    return "default".equals(lang) ? DEFAULT_LANG : nullToEmpty(lang);
  }

  @Override
  @Deprecated
  public BaseObject getXObject(DocumentReference docRef, DocumentReference classRef)
      throws DocumentNotExistsException {
    return Iterables.getFirst(getXObjects(getDocumentReadOnly(docRef, DEFAULT_LANG), classRef),
        null);
  }

  @Override
  @Deprecated
  public BaseObject getXObject(DocumentReference docRef, DocumentReference classRef, String key,
      Object value) throws DocumentNotExistsException {
    return Iterables.getFirst(getXObjects(getDocumentReadOnly(docRef, DEFAULT_LANG), classRef, key,
        value), null);
  }

  @Override
  @Deprecated
  public BaseObject getXObject(XWikiDocument doc, DocumentReference classRef) {
    return XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).fetch().first().orNull();
  }

  @Override
  @Deprecated
  public BaseObject getXObject(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    return Iterables.getFirst(getXObjects(doc, classRef, key, value), null);
  }

  @Override
  @Deprecated
  public Optional<BaseObject> getXObject(DocumentReference docRef, DocumentReference classRef,
      int objectNumber) throws DocumentNotExistsException {
    return getXObject(getDocumentReadOnly(docRef, DEFAULT_LANG), classRef, objectNumber);
  }

  @Override
  @Deprecated
  public Optional<BaseObject> getXObject(XWikiDocument doc, DocumentReference classRef,
      int objectNumber) {
    return XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).filter(
        objectNumber).fetch().first();
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef)
      throws DocumentNotExistsException {
    return getXObjects(getDocumentReadOnly(docRef, DEFAULT_LANG), classRef);
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef,
      String key, Object value) throws DocumentNotExistsException {
    return getXObjects(getDocumentReadOnly(docRef, DEFAULT_LANG), classRef, key, value);
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(DocumentReference docRef, DocumentReference classRef,
      String key, Collection<?> values) throws DocumentNotExistsException {
    return getXObjects(getDocumentReadOnly(docRef, DEFAULT_LANG), classRef, key, values);
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef) {
    return XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).fetch().list();
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    return getXObjects(doc, classRef, key, Arrays.asList(value));
  }

  @Override
  @Deprecated
  public List<BaseObject> getXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Collection<?> values) {
    ObjectFetcher<XWikiDocument, BaseObject> objFetcher = XWikiObjectEditor.on(doc).filter(
        new ClassReference(classRef)).fetch();
    List<BaseObject> ret = new ArrayList<>();
    for (BaseObject obj : objFetcher.list()) {
      if (checkPropertyKeyValues(obj, key, values)) {
        ret.add(obj);
      }
    }
    return ImmutableList.copyOf(ret);
  }

  @Override
  @Deprecated
  public Map<DocumentReference, List<BaseObject>> getXObjects(XWikiDocument doc) {
    Map<ClassIdentity, List<BaseObject>> map = XWikiObjectEditor.on(doc).fetch().map();
    WikiReference wikiRef = doc.getDocumentReference().getWikiReference();
    Map<DocumentReference, List<BaseObject>> ret = new HashMap<>();
    for (ClassIdentity classId : map.keySet()) {
      ret.put(classId.getDocRef(wikiRef), map.get(classId));
    }
    return ImmutableMap.copyOf(ret);
  }

  @Deprecated
  private boolean checkPropertyKeyValues(BaseObject obj, String key, Collection<?> checkValues) {
    boolean valid = (key == null);
    if (!valid && (checkValues != null)) {
      Object val = getProperty(obj, key);
      for (Object checkVal : checkValues) {
        valid |= Objects.equal(val, checkVal);
      }
    }
    return valid;
  }

  @Override
  public com.xpn.xwiki.api.Object getApiObject(BaseObject obj) throws NoAccessRightsException {
    com.xpn.xwiki.api.Object ret = null;
    if (obj != null) {
      try {
        if (rightsAccess.hasAccessLevel(obj.getDocumentReference(), EAccessLevel.VIEW)) {
          return getApiObjectWithoutRightCheck(obj);
        } else {
          throw new NoAccessRightsException(obj.getDocumentReference(), context.getUser(),
              EAccessLevel.VIEW);
        }
      } catch (IllegalStateException exp) {
        LOGGER.warn("getApiObject failed for '{}'", obj, exp);
      }
    }
    return ret;
  }

  @Override
  public com.xpn.xwiki.api.Object getApiObjectWithoutRightCheck(@Nullable BaseObject obj) {
    if (obj != null) {
      return obj.newObjectApi(obj, context.getXWikiContext());
    }
    return null;
  }

  @Override
  public List<com.xpn.xwiki.api.Object> getApiObjects(List<BaseObject> objs) {
    List<com.xpn.xwiki.api.Object> ret = new ArrayList<>();
    for (BaseObject obj : objs) {
      try {
        if (obj != null) {
          com.xpn.xwiki.api.Object apiObject = getApiObject(obj);
          if (apiObject != null) {
            ret.add(apiObject);
          }
        }
      } catch (NoAccessRightsException exp) {
        LOGGER.debug("getApiObjects ommits object '{}'", obj, exp);
      }
    }
    return ret;
  }

  @Override
  public List<com.xpn.xwiki.api.Object> getApiObjectsWithoutRightChecks(List<BaseObject> objs) {
    List<com.xpn.xwiki.api.Object> ret = new ArrayList<>();
    for (BaseObject obj : objs) {
      if (obj != null) {
        com.xpn.xwiki.api.Object apiObject = getApiObjectWithoutRightCheck(obj);
        if (apiObject != null) {
          ret.add(apiObject);
        }
      }
    }
    return ret;
  }

  @Override
  @Deprecated
  public BaseObject newXObject(XWikiDocument doc, DocumentReference docClassRef) {
    ClassReference classRef = new ClassReference(docClassRef);
    return XWikiObjectEditor.on(doc).filter(classRef).create().get(classRef);
  }

  @Override
  @Deprecated
  public BaseObject getOrCreateXObject(XWikiDocument doc, DocumentReference classRef) {
    return getOrCreateXObject(doc, classRef, null, null);
  }

  @Override
  @Deprecated
  public BaseObject getOrCreateXObject(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    BaseObject obj = getXObject(doc, classRef, key, value);
    if (obj == null) {
      obj = newXObject(doc, classRef);
      if (key != null) {
        setProperty(obj, key, value);
      }
    }
    return obj;
  }

  @Override
  @Deprecated
  public boolean removeXObject(XWikiDocument doc, BaseObject objToRemove) {
    return XWikiObjectEditor.on(doc).filter(objToRemove).deleteFirst().isPresent();
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, List<BaseObject> objsToRemove) {
    checkNotNull(doc);
    boolean changed = false;
    for (BaseObject obj : new ArrayList<>(objsToRemove)) {
      if (obj != null) {
        changed |= XWikiObjectEditor.on(doc).filter(obj).deleteFirst().isPresent();
      }
    }
    return changed;
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, DocumentReference classRef) {
    return !XWikiObjectEditor.on(doc).filter(new ClassReference(classRef)).delete().isEmpty();
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Object value) {
    return removeXObjects(doc, classRef, key, Arrays.asList(value));
  }

  @Override
  @Deprecated
  public boolean removeXObjects(XWikiDocument doc, DocumentReference classRef, String key,
      Collection<?> values) {
    return removeXObjects(doc, getXObjects(doc, classRef, key, values));
  }

  @Override
  public Object getProperty(DocumentReference docRef, DocumentReference classRef, String name)
      throws DocumentNotExistsException {
    return getProperty(getDocumentReadOnly(docRef, DEFAULT_LANG), classRef, name);
  }

  @Override
  public Object getProperty(XWikiDocument doc, DocumentReference classRef, String name) {
    return getProperty(getXObject(doc, classRef), name);
  }

  @Override
  public Object getProperty(BaseObject obj, String name) {
    Object value = null;
    BaseProperty prop = getBaseProperty(obj, name);
    if (prop != null) {
      value = prop.getValue();
      if (value instanceof String) {
        // avoid comparing empty string to null
        value = Strings.emptyToNull(value.toString().trim());
      } else if (value instanceof Date) {
        // avoid returning Timestamp since Timestamp.equals(Date) always returns false
        value = new Date(((Date) value).getTime());
      }
    }
    return value;
  }

  private BaseProperty getBaseProperty(BaseObject obj, String key) {
    BaseProperty prop = null;
    try {
      if (obj != null) {
        prop = (BaseProperty) obj.get(key);
      }
    } catch (XWikiException | ClassCastException exc) {
      // does not happen since
      // XWikiException is never thrown in BaseObject.get()
      // BaseObject only contains BaseProperties
      LOGGER.error("should not happen", exc);
    }
    return prop;
  }

  @Override
  public <T> Optional<T> getFieldValue(BaseObject obj, ClassField<T> field) {
    checkClassRef(obj, field);
    return Optional.fromNullable(resolvePropertyValue(field, getProperty(obj, field.getName())));
  }

  @Override
  public <T> Optional<T> getFieldValue(XWikiDocument doc, ClassField<T> field) {
    checkNotNull(doc);
    checkNotNull(field);
    return Optional.fromNullable(resolvePropertyValue(field, getProperty(doc,
        field.getClassDef().getClassRef(), field.getName())));
  }

  @Override
  public <T> Optional<T> getFieldValue(DocumentReference docRef, ClassField<T> field)
      throws DocumentNotExistsException {
    checkNotNull(docRef);
    checkNotNull(field);
    return Optional.fromNullable(resolvePropertyValue(field, getProperty(docRef,
        field.getClassDef().getClassRef(), field.getName())));
  }

  @Override
  public <T> Optional<T> getFieldValue(XWikiDocument doc, ClassField<T> field, T ignoreValue) {
    checkNotNull(ignoreValue);
    Optional<T> property = getFieldValue(doc, field);
    if (property.isPresent() && Objects.equal(property.get(), ignoreValue)) {
      property = Optional.absent();
    }
    return property;
  }

  @Override
  public <T> Optional<T> getFieldValue(DocumentReference docRef, ClassField<T> field, T ignoreValue)
      throws DocumentNotExistsException {
    checkNotNull(ignoreValue);
    Optional<T> property = getFieldValue(docRef, field);
    if (property.isPresent() && Objects.equal(property.get(), ignoreValue)) {
      property = Optional.absent();
    }
    return property;
  }

  @Override
  @Deprecated
  public <T> T getProperty(DocumentReference docRef, ClassField<T> field)
      throws DocumentNotExistsException {
    return getFieldValue(docRef, field).orNull();
  }

  @Override
  @Deprecated
  public <T> T getProperty(XWikiDocument doc, ClassField<T> field) {
    return getFieldValue(doc, field).orNull();
  }

  private <T> T resolvePropertyValue(ClassField<T> field, Object value) {
    try {
      if (field instanceof CustomClassField) {
        return ((CustomClassField<T>) field).resolve(value);
      } else {
        return field.getType().cast(value);
      }
    } catch (ClassCastException | IllegalArgumentException ex) {
      throw new IllegalArgumentException("Field '" + field + "' ill defined, expecting type '"
          + field.getType() + "' but got '" + value.getClass() + "'", ex);
    }
  }

  @Override
  public List<ClassFieldValue<?>> getProperties(XWikiDocument doc, ClassDefinition classDef) {
    List<ClassFieldValue<?>> ret = new ArrayList<>();
    for (ClassField<?> field : classDef.getFields()) {
      ret.add(new ClassFieldValue<>(castField(field), getProperty(doc, field)));
    }
    return ret;
  }

  // unchecked suppression is ok because every wildcard extends Object
  @SuppressWarnings("unchecked")
  private ClassField<Object> castField(ClassField<?> field) {
    return (ClassField<Object>) field;
  }

  @Override
  public boolean setProperty(BaseObject obj, String name, Object value) {
    boolean hasChange = !Objects.equal(value, getProperty(obj, name));
    if (hasChange) {
      if (value instanceof Collection) {
        value = Joiner.on('|').join((Collection<?>) value);
      }
      try {
        obj.set(name, value, context.getXWikiContext());
      } catch (ClassCastException ex) {
        throw new IllegalArgumentException("Unable to set value '" + value + "' on field '"
            + modelUtils.serializeRefLocal(obj.getXClassReference()) + "." + name + "'", ex);
      }
    }
    return hasChange;
  }

  @Override
  public <T> XWikiDocument setProperty(DocumentReference docRef, ClassField<T> field, T value)
      throws DocumentNotExistsException {
    XWikiDocument doc = getDocument(docRef);
    setProperty(doc, field, value);
    return doc;
  }

  @Override
  public <T> boolean setProperty(XWikiDocument doc, ClassField<T> field, T value) {
    return setProperty(getOrCreateXObject(doc, field.getClassDef().getClassRef()), field, value);
  }

  @Override
  public <T> boolean setProperty(XWikiDocument doc, ClassFieldValue<T> fieldValue) {
    return setProperty(doc, fieldValue.getField(), fieldValue.getValue());
  }

  @Override
  public <T> boolean setProperty(BaseObject obj, ClassField<T> field, T value) {
    checkClassRef(obj, field);
    return setProperty(obj, field.getName(), serializePropertyValue(field, value));
  }

  private <T> Object serializePropertyValue(ClassField<T> field, T value) {
    try {
      if (field instanceof CustomClassField) {
        return ((CustomClassField<T>) field).serialize(value);
      } else {
        return value;
      }
    } catch (ClassCastException | IllegalArgumentException ex) {
      throw new IllegalArgumentException("Field '" + field + "' ill defined", ex);
    }
  }

  private void checkClassRef(BaseObject obj, ClassField<?> field) {
    DocumentReference classRef = checkNotNull(obj).getXClassReference();
    checkArgument(classRef.equals(checkNotNull(field).getClassDef().getClassRef(
        classRef.getWikiReference())), "class refs from obj and field do not match");
  }

  @Override
  public XWikiAttachment getAttachmentNameEqual(XWikiDocument doc, String filename)
      throws AttachmentNotExistsException {
    for (XWikiAttachment attach : doc.getAttachmentList()) {
      if ((attach != null) && attach.getFilename().equals(filename)) {
        return attach;
      }
    }
    LOGGER.debug("getAttachmentNameEqual: not found! file: [{}], doc: [{}]", filename,
        serialize(doc.getDocumentReference()));
    // FIXME empty or null filename leads to exception:
    // java.lang.IllegalArgumentException: An Entity Reference name cannot be null or
    // empty
    if (Strings.isNullOrEmpty(filename)) {
      throw new AttachmentNotExistsException(null);
    } else {
      throw new AttachmentNotExistsException(new AttachmentReference(filename,
          doc.getDocumentReference()));
    }
  }

  private Supplier<String> serialize(EntityReference ref) {
    return defer(() -> modelUtils.serializeRef(ref, ReferenceSerializationMode.GLOBAL));
  }

}
