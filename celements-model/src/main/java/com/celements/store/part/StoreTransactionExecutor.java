package com.celements.store.part;

import static com.google.common.base.Preconditions.*;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.celements.store.CelHibernateStore;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public abstract class StoreTransactionExecutor {

  protected final CelHibernateStore store;

  private FlushMode flushMode = null;
  private boolean withTransaction = true;
  private boolean withCommit = false;
  private XWikiDocument doc = null;

  StoreTransactionExecutor(CelHibernateStore store) {
    this.store = checkNotNull(store);
  }

  public StoreTransactionExecutor flushMode(FlushMode flushMode) {
    this.flushMode = flushMode;
    return this;
  }

  public StoreTransactionExecutor withTransaction(boolean withTransaction) {
    this.withTransaction = withTransaction;
    return this;
  }

  public StoreTransactionExecutor withCommit() {
    withCommit = true;
    return this;
  }

  public StoreTransactionExecutor injectCustomMappings(XWikiDocument doc) {
    this.doc = doc;
    return this;
  }

  public void execute(XWikiContext context) throws HibernateException, XWikiException {
    boolean bTransaction = withTransaction;
    boolean commit = false;
    try {
      if (bTransaction) {
        store.checkHibernate(context);
        SessionFactory sfactory = null;
        if (doc != null) {
          sfactory = store.injectCustomMappingsInSessionFactory(doc, context);
        }
        bTransaction = store.beginTransaction(sfactory, context);
      }
      Session session = store.getSession(context);
      if (flushMode != null) {
        session.setFlushMode(flushMode);
      }
      call(session);
      commit = withCommit;
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }
  }

  protected abstract void call(Session session) throws HibernateException, XWikiException;

}
