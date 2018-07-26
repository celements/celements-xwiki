package com.celements.store;

import static com.xpn.xwiki.XWikiException.*;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.EntityReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentAccessException;
import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.metadata.DocumentMetaData;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Singleton
@Component(CelementsDefaultStore.COMPONENT_NAME)
public class CelementsDefaultStore extends DelegateStore implements CelementsStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(CelementsDefaultStore.class);

  public static final String COMPONENT_NAME = "CelementsDefaultStore";

  public static final String BACKING_STORE_CFG_NAME = "celements.store.storeStrategy";

  @Requirement
  private IModelAccessFacade modelAccess;

  @Override
  protected String getBackingStoreConfigName() {
    return BACKING_STORE_CFG_NAME;
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      modelAccess.saveDocument(doc, doc.getComment(), doc.isMinorEdit());
    } catch (DocumentSaveException exc) {
      throw newXWikiExceptionWrapper(exc);
    }
  }

  @Override
  public void saveXWikiDoc(XWikiDocument doc, XWikiContext context, boolean bTransaction)
      throws XWikiException {
    try {
      modelAccess.saveDocument(doc, doc.getComment(), doc.isMinorEdit());
    } catch (DocumentSaveException exc) {
      throw newXWikiExceptionWrapper(exc);
    }
  }

  @Override
  public XWikiDocument loadXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    return modelAccess.getOrCreateDocument(doc.getDocumentReference());
  }

  @Override
  public void deleteXWikiDoc(XWikiDocument doc, XWikiContext context) throws XWikiException {
    try {
      modelAccess.deleteDocument(doc, true);
    } catch (DocumentDeleteException exc) {
      throw newXWikiExceptionWrapper(exc);
    }
  }

  @Override
  public boolean exists(XWikiDocument doc, XWikiContext context) throws XWikiException {
    return modelAccess.exists(doc.getDocumentReference());
  }

  @Override
  public Set<DocumentMetaData> listDocumentMetaData(EntityReference filterRef) {
    if (getBackingStore() instanceof MetaDataStoreExtension) {
      return ((MetaDataStoreExtension) getBackingStore()).listDocumentMetaData(filterRef);
    } else {
      return new LinkedHashSet<>();
    }
  }

  private XWikiException newXWikiExceptionWrapper(DocumentAccessException exc) {
    return new XWikiException(MODULE_XWIKI_STORE, ERROR_XWIKI_STORE_HIBERNATE_SAVING_DOC, "", exc);
  }

}
