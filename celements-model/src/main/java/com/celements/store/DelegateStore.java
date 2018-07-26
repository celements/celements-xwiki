package com.celements.store;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

public abstract class DelegateStore implements CelementsStore {

  private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

  @Requirement("xwikiproperties")
  private ConfigurationSource cfgSrc;

  /**
   * Lazy initialized according to backing store strategy configuration. The store field is
   * immutable in the following way: even though it could be initialized in multiple threads because
   * of missing memory visibility, it always results in the same value. Thus a thread could not tell
   * if it was multiple times initialized. Hence no volatile keyword is needed.
   */
  private CelementsStore store;

  @Override
  public CelementsStore getBackingStore() {
    // TODO still working?
    // return default store here because XWiki.getHibernateStore() relies on it
    // return Utils.getComponent(XWikiStoreInterface.class);
    if (this.store == null) {
      String hint = cfgSrc.getProperty(getBackingStoreConfigName(), "default");
      XWikiStoreInterface store = Utils.getComponent(CelementsStore.class, hint);
      if (store == this) {
        throw new IllegalArgumentException("circular backing store: " + hint);
      }
      LOGGER.info("backing store initialized '{}' for hint '{}'", this.store, hint);
    }
    return this.store;
  }

  protected abstract String getBackingStoreConfigName();

  @Override
  public List<String> getClassList(XWikiContext context) throws XWikiException {
    return getBackingStore().getClassList(context);
  }

  @Override
  public int countDocuments(String wheresql, XWikiContext context) throws XWikiException {
    return getBackingStore().countDocuments(wheresql, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocumentReferences(wheresql, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocumentsNames(wheresql, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(wheresql, nb, start, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, int nb, int start, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocumentsNames(wheresql, nb, start, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String wheresql, int nb, int start,
      String selectColumns, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(wheresql, nb, start, selectColumns, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String wheresql, int nb, int start, String selectColumns,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentsNames(wheresql, nb, start, selectColumns, context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause, int nb,
      int start, List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(parametrizedSqlClause, nb, start,
        parameterValues, context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String parametrizedSqlClause, int nb, int start,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentsNames(parametrizedSqlClause, nb, start, parameterValues,
        context);
  }

  @Override
  public List<DocumentReference> searchDocumentReferences(String parametrizedSqlClause,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentReferences(parametrizedSqlClause, parameterValues,
        context);
  }

  @Override
  @Deprecated
  public List<String> searchDocumentsNames(String parametrizedSqlClause, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocumentsNames(parametrizedSqlClause, parameterValues, context);
  }

  @Override
  public boolean isCustomMappingValid(BaseClass bclass, String custommapping1, XWikiContext context)
      throws XWikiException {
    return getBackingStore().isCustomMappingValid(bclass, custommapping1, context);
  }

  @Override
  public boolean injectCustomMapping(BaseClass doc1class, XWikiContext context)
      throws XWikiException {
    return getBackingStore().injectCustomMapping(doc1class, context);
  }

  @Override
  public boolean injectCustomMappings(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    return getBackingStore().injectCustomMappings(doc, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      boolean customMapping, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, customMapping, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname, int nb,
      int start, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, nb, start, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      boolean customMapping, int nb, int start, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, customMapping, nb, start,
        context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, nb, start, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbyname,
      boolean customMapping, boolean checkRight, int nb, int start, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbyname, customMapping, checkRight,
        nb, start, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage, int nb,
      int start, List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbylanguage, nb, start,
        parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping, int nb, int start, List<?> parameterValues, XWikiContext context)
      throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbylanguage, customMapping, nb, start,
        parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, int nb, int start,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, nb, start, parameterValues, context);
  }

  @Override
  public List<XWikiDocument> searchDocuments(String wheresql, boolean distinctbylanguage,
      boolean customMapping, boolean checkRight, int nb, int start, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().searchDocuments(wheresql, distinctbylanguage, customMapping,
        checkRight, nb, start, parameterValues, context);
  }

  @Override
  public int countDocuments(String parametrizedSqlClause, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().countDocuments(parametrizedSqlClause, parameterValues, context);
  }

  @Override
  public XWikiLock loadLock(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    return getBackingStore().loadLock(docId, context, bTransaction);
  }

  @Override
  public void saveLock(XWikiLock lock, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().saveLock(lock, context, bTransaction);
  }

  @Override
  public void deleteLock(XWikiLock lock, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().deleteLock(lock, context, bTransaction);
  }

  @Override
  public List<XWikiLink> loadLinks(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    return getBackingStore().loadLinks(docId, context, bTransaction);
  }

  @Override
  public List<DocumentReference> loadBacklinks(DocumentReference documentReference,
      boolean bTransaction, XWikiContext context) throws XWikiException {
    return getBackingStore().loadBacklinks(documentReference, bTransaction, context);
  }

  @Override
  @Deprecated
  public List<String> loadBacklinks(String fullName, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    return getBackingStore().loadBacklinks(fullName, context, bTransaction);
  }

  @Override
  public void saveLinks(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().saveLinks(doc, context, bTransaction);
  }

  @Override
  public void deleteLinks(long docId, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    getBackingStore().deleteLinks(docId, context, bTransaction);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, XWikiContext context)
      throws XWikiException {
    return getBackingStore().search(sql, nb, start, context);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams,
      XWikiContext context) throws XWikiException {
    return getBackingStore().search(sql, nb, start, whereParams, context);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, List<?> parameterValues,
      XWikiContext context) throws XWikiException {
    return getBackingStore().search(sql, nb, start, parameterValues, context);
  }

  @Override
  public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams,
      List<?> parameterValues, XWikiContext context) throws XWikiException {
    return getBackingStore().search(sql, nb, start, whereParams, parameterValues, context);
  }

  @Override
  public synchronized void cleanUp(XWikiContext context) {
    getBackingStore().cleanUp(context);
  }

  @Override
  public boolean isWikiNameAvailable(String wikiName, XWikiContext context) throws XWikiException {
    return getBackingStore().isWikiNameAvailable(wikiName, context);
  }

  @Override
  public synchronized void createWiki(String wikiName, XWikiContext context) throws XWikiException {
    getBackingStore().createWiki(wikiName, context);
  }

  @Override
  public synchronized void deleteWiki(String wikiName, XWikiContext context) throws XWikiException {
    getBackingStore().deleteWiki(wikiName, context);
  }

  @Override
  public List<String> getCustomMappingPropertyList(BaseClass bclass) {
    return getBackingStore().getCustomMappingPropertyList(bclass);
  }

  @Override
  public synchronized void injectCustomMappings(XWikiContext context) throws XWikiException {
    getBackingStore().injectCustomMappings(context);
  }

  @Override
  public void injectUpdatedCustomMappings(XWikiContext context) throws XWikiException {
    getBackingStore().injectUpdatedCustomMappings(context);
  }

  @Override
  public List<String> getTranslationList(XWikiDocument doc, XWikiContext context)
      throws XWikiException {
    return getBackingStore().getTranslationList(doc, context);
  }

  @Override
  public QueryManager getQueryManager() {
    return getBackingStore().getQueryManager();
  }

}
