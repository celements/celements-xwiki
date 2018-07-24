package com.celements.model.access;

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.access.exception.DocumentDeleteException;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * Implementation of {@link ModelAccessStrategy} only accessing {@link XWikiStoreInterface}.
 *
 * @author Marc Sladek
 */
@Component
public class StoreModelAccessStrategy implements ModelAccessStrategy {

  @Requirement
  protected ModelContext context;

  @Requirement
  protected XWikiDocumentCreator docCreator;

  private XWikiStoreInterface getStore() {
    return context.getXWikiContext().getWiki().getStore();
  }

  @Override
  public boolean exists(final DocumentReference docRef, final String lang) {
    try {
      return new ContextExecutor<Boolean, XWikiException>() {

        @Override
        protected Boolean call() throws XWikiException {
          // FIXME [CELDEV-702] XWikiHibernateStore doesn't check language
          return getStore().exists(docCreator.createWithoutDefaults(docRef, lang),
              context.getXWikiContext());
        }
      }.inWiki(docRef.getWikiReference()).execute();
    } catch (XWikiException xwe) {
      throw new DocumentLoadException(docRef, xwe);
    }
  }

  @Override
  public XWikiDocument getDocument(final DocumentReference docRef, final String lang) {
    try {
      return new ContextExecutor<XWikiDocument, XWikiException>() {

        @Override
        protected XWikiDocument call() throws XWikiException {
          return getStore().loadXWikiDoc(docCreator.createWithoutDefaults(docRef, lang),
              context.getXWikiContext());
        }
      }.inWiki(docRef.getWikiReference()).execute();
    } catch (XWikiException xwe) {
      throw new DocumentLoadException(docRef, xwe);
    }
  }

  @Override
  public XWikiDocument createDocument(DocumentReference docRef, String lang) {
    return docCreator.create(docRef, lang);
  }

  @Override
  public void saveDocument(final XWikiDocument doc) throws DocumentSaveException {
    DocumentReference docRef = doc.getDocumentReference();
    try {
      new ContextExecutor<Void, XWikiException>() {

        @Override
        protected Void call() throws XWikiException {
          getStore().saveXWikiDoc(doc, context.getXWikiContext());
          return null;
        }
      }.inWiki(docRef.getWikiReference()).execute();
    } catch (XWikiException xwe) {
      throw new DocumentSaveException(docRef, xwe);
    }
  }

  @Override
  public void deleteDocument(final XWikiDocument doc) throws DocumentDeleteException {
    DocumentReference docRef = doc.getDocumentReference();
    try {
      new ContextExecutor<Void, XWikiException>() {

        @Override
        protected Void call() throws XWikiException {
          getStore().deleteXWikiDoc(doc, context.getXWikiContext());
          return null;
        }
      }.inWiki(docRef.getWikiReference()).execute();
    } catch (XWikiException xwe) {
      throw new DocumentDeleteException(docRef, xwe);
    }
  }

  @Override
  public List<String> getTranslations(final DocumentReference docRef) {
    try {
      return new ContextExecutor<List<String>, XWikiException>() {

        @Override
        protected List<String> call() throws XWikiException {
          return getStore().getTranslationList(docCreator.createWithoutDefaults(docRef,
              IModelAccessFacade.DEFAULT_LANG), context.getXWikiContext());
        }
      }.inWiki(docRef.getWikiReference()).execute();
    } catch (XWikiException xwe) {
      throw new DocumentLoadException(docRef, xwe);
    }
  }

}
