package com.celements.store.part;

import static com.google.common.base.Preconditions.*;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.celements.common.Callable;
import com.celements.store.CelHibernateStore;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class StoreTransactionExecutor {

  protected final CelHibernateStore store;
  protected final XWikiContext context;
  protected final Callable<Session, Void, XWikiException> callable;

  private FlushMode flushMode = FlushMode.COMMIT;
  private boolean withTransaction = true;
  private boolean withCommit = false;
  private XWikiDocument doc = null;

  StoreTransactionExecutor(CelHibernateStore store, XWikiContext context,
      Callable<Session, Void, XWikiException> callable) {
    this.store = checkNotNull(store);
    this.context = checkNotNull(context);
    this.callable = checkNotNull(callable);
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

  public void execute() throws HibernateException, XWikiException {
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
      session.setFlushMode(flushMode);
      callable.call(session);
      commit = withCommit;
    } finally {
      if (bTransaction) {
        store.endTransaction(context, commit);
      }
    }
  }

}
