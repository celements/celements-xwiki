package com.celements.store.part;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.store.CelHibernateStore;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class StoreTransactionExecutorTest extends AbstractComponentTest {

  private CelHibernateStore storeMock;
  private Session sessionMock;

  @Before
  public void prepareTest() throws Exception {
    storeMock = createMockAndAddToDefault(CelHibernateStore.class);
    expect(getWikiMock().getStore()).andReturn(storeMock).anyTimes();
    sessionMock = createMockAndAddToDefault(Session.class);
  }

  @Test
  public void test_execute() throws Exception {
    expect(storeMock.getSession(getContext())).andReturn(sessionMock);
    storeMock.checkHibernate(getContext());
    expect(storeMock.beginTransaction(null, getContext())).andReturn(true);
    storeMock.endTransaction(getContext(), false);

    replayDefault();
    getSessionAssertExecutor().execute(getContext());
    verifyDefault();
  }

  @Test
  public void test_noTransaction() throws Exception {
    expect(storeMock.getSession(getContext())).andReturn(sessionMock);

    replayDefault();
    getSessionAssertExecutor().withTransaction(false).execute(getContext());
    verifyDefault();
  }

  @Test
  public void test_flushMode() throws Exception {
    expect(storeMock.getSession(getContext())).andReturn(sessionMock);
    FlushMode mode = FlushMode.ALWAYS;
    sessionMock.setFlushMode(mode);

    replayDefault();
    getSessionAssertExecutor().withTransaction(false).flushMode(mode).execute(getContext());
    verifyDefault();
  }

  @Test
  public void test_failBegin() throws Exception {
    expect(storeMock.getSession(getContext())).andReturn(sessionMock);
    storeMock.checkHibernate(getContext());
    expect(storeMock.beginTransaction(null, getContext())).andReturn(false);

    replayDefault();
    getSessionAssertExecutor().execute(getContext());
    verifyDefault();
  }

  @Test
  public void test_withCommit() throws Exception {
    expect(storeMock.getSession(getContext())).andReturn(sessionMock);
    storeMock.checkHibernate(getContext());
    expect(storeMock.beginTransaction(null, getContext())).andReturn(true);
    storeMock.endTransaction(getContext(), true);

    replayDefault();
    getSessionAssertExecutor().withCommit().execute(getContext());
    verifyDefault();
  }

  @Test
  public void test_fail_XWikiException() throws Exception {
    final XWikiException cause = new XWikiException();
    expect(storeMock.getSession(getContext())).andReturn(sessionMock);
    storeMock.checkHibernate(getContext());
    expect(storeMock.beginTransaction(null, getContext())).andReturn(true);
    storeMock.endTransaction(getContext(), false);

    replayDefault();
    new ExceptionAsserter<XWikiException>(XWikiException.class, cause) {

      @Override
      protected void execute() throws XWikiException {
        getThrowExceptionExecutor(cause).withCommit().execute(getContext());
      }
    }.execute();
    verifyDefault();
  }

  @Test
  public void test_injectCustomMappings() throws Exception {
    expect(storeMock.getSession(getContext())).andReturn(sessionMock);
    storeMock.checkHibernate(getContext());
    XWikiDocument doc = new XWikiDocument(new DocumentReference("xwikidb", "space", "doc"));
    SessionFactory sfactoryMock = createMockAndAddToDefault(SessionFactory.class);
    expect(storeMock.injectCustomMappingsInSessionFactory(doc, getContext())).andReturn(
        sfactoryMock);
    expect(storeMock.beginTransaction(sfactoryMock, getContext())).andReturn(true);
    storeMock.endTransaction(getContext(), false);

    replayDefault();
    getSessionAssertExecutor().injectCustomMappings(doc).execute(getContext());
    verifyDefault();
  }

  private StoreTransactionExecutor getSessionAssertExecutor() {
    return new StoreTransactionExecutor(storeMock) {

      @Override
      protected void call(Session session) throws HibernateException, XWikiException {
        assertSame(sessionMock, session);
      }
    };
  }

  private StoreTransactionExecutor getThrowExceptionExecutor(final Exception exc) {
    return new StoreTransactionExecutor(storeMock) {

      @Override
      protected void call(Session session) throws HibernateException, XWikiException {
        assertSame(sessionMock, session);
        if (exc instanceof HibernateException) {
          throw (HibernateException) exc;
        } else if (exc instanceof XWikiException) {
          throw (XWikiException) exc;
        }
      }
    };
  }

}
