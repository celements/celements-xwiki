package com.celements.store.part;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;

import org.easymock.Capture;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.store.CelHibernateStore;
import com.celements.store.id.CelementsIdComputer;
import com.celements.store.id.IdVersion;
import com.celements.store.id.UniqueHashIdComputer;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class DocumentSavePreparationCommandTest extends AbstractComponentTest {

  private DocumentSavePreparationCommand savePrepCmd;

  private CelHibernateStore storeMock;
  private Session sessionMock;

  @Before
  public void prepareTest() throws Exception {
    storeMock = createMockAndAddToDefault(CelHibernateStore.class);
    expect(getWikiMock().getStore()).andReturn(storeMock).anyTimes();
    expect(storeMock.getIdComputer()).andReturn(Utils.getComponent(CelementsIdComputer.class,
        UniqueHashIdComputer.NAME)).anyTimes();
    sessionMock = createMockAndAddToDefault(Session.class);
    sessionMock.setFlushMode(FlushMode.COMMIT);
    expectLastCall().anyTimes();
    expect(storeMock.getSession(getContext())).andReturn(sessionMock).anyTimes();
    savePrepCmd = new DocumentSavePreparationCommand(storeMock);
  }

  @Test
  public void test_execute() throws Exception {
    XWikiDocument doc = createDoc(false, null);

    replayDefault();
    Session ret = savePrepCmd.execute(doc, false, getContext());
    verifyDefault();
    assertSame(sessionMock, ret);
    assertSame("store should be set", storeMock, doc.getStore());
    assertEquals("doc should be set to context db", getContext().getDatabase(),
        doc.getDocumentReference().getWikiReference().getName());
    assertFalse(doc.hasElement(XWikiDocument.HAS_OBJECTS));
    assertFalse(doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
  }

  @Test
  public void test_execute_transaction() throws Exception {
    XWikiDocument doc = createDoc(false, null);

    storeMock.checkHibernate(same(getContext()));
    expectLastCall();
    SessionFactory sfactoryMock = createMockAndAddToDefault(SessionFactory.class);
    expect(storeMock.injectCustomMappingsInSessionFactory(same(doc), same(getContext()))).andReturn(
        sfactoryMock);
    expect(storeMock.beginTransaction(same(sfactoryMock), same(getContext()))).andReturn(true);

    replayDefault();
    Session ret = savePrepCmd.execute(doc, true, getContext());
    verifyDefault();
    assertSame(sessionMock, ret);
  }

  @Test
  public void test_execute_object() throws Exception {
    XWikiDocument doc = createDoc(true, null);
    BaseObject obj = addObject(doc);

    assertTrue(obj.getGuid().isEmpty());
    assertEquals(0, obj.getId());
    replayDefault();
    savePrepCmd.execute(doc, false, getContext());
    verifyDefault();
    assertTrue(doc.hasElement(XWikiDocument.HAS_OBJECTS));
    assertFalse(obj.getGuid().isEmpty());
    assertEquals(-974136870929809407L, obj.getId());
  }

  @Test
  public void test_execute_object_load() throws Exception {
    XWikiDocument doc = createDoc(false, "ch");
    BaseObject obj = addObject(doc);
    XWikiDocument origDoc = createDoc(false, "ch");
    BaseObject origObj = addObject(origDoc);
    origObj.setId(1234, IdVersion.CELEMENTS_3);
    Capture<XWikiDocument> docCaptureExists = new Capture<>();
    Capture<XWikiDocument> docCaptureLoad = new Capture<>();
    expect(storeMock.exists(capture(docCaptureExists), same(getContext()))).andReturn(true);
    expect(storeMock.loadXWikiDoc(capture(docCaptureLoad), same(getContext()))).andReturn(origDoc);

    replayDefault();
    savePrepCmd.execute(doc, false, getContext());
    verifyDefault();
    assertDocCapture(doc, docCaptureExists);
    assertDocCapture(doc, docCaptureLoad);
    assertEquals(origObj.getId(), obj.getId());
  }

  private void assertDocCapture(XWikiDocument doc, Capture<XWikiDocument> docCapture) {
    assertNotSame(doc, docCapture.getValue());
    assertEquals(doc.getDocumentReference(), docCapture.getValue().getDocumentReference());
    assertEquals(doc.getLanguage(), docCapture.getValue().getLanguage());
  }

  @Test
  public void test_execute_object_load_noObj() throws Exception {
    XWikiDocument doc = createDoc(false, null);
    BaseObject obj = addObject(doc);
    expect(storeMock.exists(anyObject(XWikiDocument.class), same(getContext()))).andReturn(true);
    expect(storeMock.loadXWikiDoc(anyObject(XWikiDocument.class), same(getContext()))).andReturn(
        createDoc(false, null));

    replayDefault();
    savePrepCmd.execute(doc, false, getContext());
    verifyDefault();
    assertEquals(-974136870929809407L, obj.getId());
  }

  @Test
  public void test_execute_object_load_notExists() throws Exception {
    XWikiDocument doc = createDoc(false, null);
    BaseObject obj = addObject(doc);
    expect(storeMock.exists(anyObject(XWikiDocument.class), same(getContext()))).andReturn(false);

    replayDefault();
    savePrepCmd.execute(doc, false, getContext());
    verifyDefault();
    assertEquals(-974136870929809407L, obj.getId());
  }

  @Test
  public void test_execute_object_load_XWE() throws Exception {
    final XWikiDocument doc = createDoc(false, null);
    addObject(doc);
    XWikiException cause = new XWikiException();
    expect(storeMock.exists(anyObject(XWikiDocument.class), same(getContext()))).andThrow(cause);

    replayDefault();
    new ExceptionAsserter<XWikiException>(XWikiException.class, cause) {

      @Override
      protected void execute() throws Exception {
        savePrepCmd.execute(doc, false, getContext());
      }
    }.evaluate();
    verifyDefault();
  }

  private BaseObject addObject(XWikiDocument doc) {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(new DocumentReference("xwikidb", "space", "class"));
    obj.setGuid("");
    doc.addXObject(obj);
    return obj;
  }

  @Test
  public void test_execute_attachment() throws Exception {
    XWikiDocument doc = createDoc(false, null);
    doc.setAttachmentList(Arrays.asList(new XWikiAttachment(doc, "file")));

    replayDefault();
    savePrepCmd.execute(doc, false, getContext());
    verifyDefault();
    assertTrue(doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
  }

  private XWikiDocument createDoc(boolean isNew, String language) {
    DocumentReference docRef = new DocumentReference("xwikidb", "space", "doc");
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(language);
    doc.setNew(isNew);
    return doc;
  }

}
