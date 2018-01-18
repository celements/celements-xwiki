package com.celements.store.id;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.store.id.CelementsIdComputer.IdComputationException;
import com.google.common.base.VerifyException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

public class UniqueHashIdComputerTest extends AbstractComponentTest {

  private UniqueHashIdComputer idComputer;
  private DocumentReference docRef;
  private String lang;

  @Before
  public void prepareTest() {
    idComputer = (UniqueHashIdComputer) Utils.getComponent(CelementsIdComputer.class,
        UniqueHashIdComputer.NAME);
    docRef = new DocumentReference("db", "space", "page");
    lang = "es";
  }

  @Test
  public void test_andifyRight() {
    long base = 0L;
    assertEquals(0x0000000000000000L, idComputer.andifyRight(base, (byte) 0));
    assertEquals(0x0000000000000001L, idComputer.andifyRight(base, (byte) 1));
    assertEquals(0x000000000000000fL, idComputer.andifyRight(base, (byte) 4));
    assertEquals(0x00000000ffffffffL, idComputer.andifyRight(base, (byte) 32));
    assertEquals(0x0fffffffffffffffL, idComputer.andifyRight(base, (byte) 60));
    assertEquals(0x7fffffffffffffffL, idComputer.andifyRight(base, (byte) 63));
  }

  @Test
  public void test_andifyLeft() {
    long base = 0L;
    assertEquals(0x0000000000000000L, idComputer.andifyLeft(base, (byte) 0));
    assertEquals(0x8000000000000000L, idComputer.andifyLeft(base, (byte) 1));
    assertEquals(0xf000000000000000L, idComputer.andifyLeft(base, (byte) 4));
    assertEquals(0xffffffff00000000L, idComputer.andifyLeft(base, (byte) 32));
    assertEquals(0xfffffffffffffff0L, idComputer.andifyLeft(base, (byte) 60));
    assertEquals(0xfffffffffffffffeL, idComputer.andifyLeft(base, (byte) 63));
  }

  @Test
  public void test_serializeLocalUid() {
    assertEquals("5:space4:page2:es", idComputer.serializeLocalUid(docRef, lang));
  }

  @Test
  public void test_hashMD5() throws Exception {
    assertEquals(0xf0da7f3f8545ded5L, idComputer.hashMD5(idComputer.serializeLocalUid(docRef,
        lang)));
  }

  @Test
  public void test_computeId_collisionCount() throws Exception {
    // full md5: 0xf0da7f3f8545ded5L
    assertEquals(0x30da7f3f8545ded5L, idComputer.computeId(docRef, lang, (byte) 0b00, 0xed5));
    assertEquals(0x70da7f3f8545ded5L, idComputer.computeId(docRef, lang, (byte) 0b01, 0xed5));
    assertEquals(0xb0da7f3f8545ded5L, idComputer.computeId(docRef, lang, (byte) 0b10, 0xed5));
    assertEquals(0xf0da7f3f8545ded5L, idComputer.computeId(docRef, lang, (byte) 0b11, 0xed5));
  }

  @Test
  public void test_computeId_collisionCount_negative() throws Exception {
    Throwable cause = new ExceptionAsserter<IdComputationException>(IdComputationException.class) {

      @Override
      protected void execute() throws IdComputationException {
        idComputer.computeId(docRef, lang, (byte) -1, 0);
      }
    }.evaluate().getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("negative"));
  }

  @Test
  public void test_computeId_collisionCount_overflow() throws Exception {
    Throwable cause = new ExceptionAsserter<IdComputationException>(IdComputationException.class) {

      @Override
      protected void execute() throws IdComputationException {
        idComputer.computeId(docRef, lang, (byte) 0b100, 0);
      }
    }.evaluate().getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("outside of defined range"));
  }

  @Test
  public void test_computeId_objectCount() throws Exception {
    // full md5: 0xf0da7f3f8545ded5L
    assertEquals(0xf0da7f3f8545d000L, idComputer.computeId(docRef, lang, (byte) 0b11, 0));
    assertEquals(0xf0da7f3f8545d005L, idComputer.computeId(docRef, lang, (byte) 0b11, 5));
    assertEquals(0xf0da7f3f8545ded5L, idComputer.computeId(docRef, lang, (byte) 0b11, 0xed5));
    assertEquals(0xf0da7f3f8545d0a0L, idComputer.computeId(docRef, lang, (byte) 0b11, 0xa0));
    assertEquals(0xf0da7f3f8545dfffL, idComputer.computeId(docRef, lang, (byte) 0b11, 0xfff));
  }

  @Test
  public void test_computeId_objectCount_negative() throws Exception {
    Throwable cause = new ExceptionAsserter<IdComputationException>(IdComputationException.class) {

      @Override
      protected void execute() throws IdComputationException {
        idComputer.computeId(docRef, lang, (byte) 0, -1);
      }
    }.evaluate().getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("negative"));
  }

  @Test
  public void test_computeId_objectCount_overflow() throws Exception {
    Throwable cause = new ExceptionAsserter<IdComputationException>(IdComputationException.class) {

      @Override
      protected void execute() throws IdComputationException {
        idComputer.computeId(docRef, lang, (byte) 0, 1 << 12);
      }
    }.evaluate().getCause();
    assertSame(VerifyException.class, cause.getClass());
    assertTrue(cause.getMessage().contains("outside of defined range"));
  }

  @Test
  public void test_computeDocumentId() throws Exception {
    System.out.println(Long.toHexString(idComputer.computeDocumentId(docRef, "")));
    assertEquals(0x30da7f3f8545d000L, idComputer.computeDocumentId(docRef, lang));
    assertEquals(0xb0da7f3f8545d000L, idComputer.computeDocumentId(docRef, lang, (byte) 0b10));
  }

  @Test
  public void test_computeDocumentId_docRef() throws Exception {
    long exp = 0x30da7f3f8545d000L;
    assertEquals(exp, idComputer.computeDocumentId(docRef, lang));
    docRef.getWikiReference().setName("asdf");
    assertEquals(exp, idComputer.computeDocumentId(docRef, lang));
    docRef.setName("asdf");
    assertFalse(exp == idComputer.computeDocumentId(docRef, lang));
    new ExceptionAsserter<NullPointerException>(NullPointerException.class) {

      @Override
      protected void execute() throws Exception {
        idComputer.computeDocumentId(null, lang);
      }
    }.evaluate();
  }

  @Test
  public void test_computeDocumentId_lang() throws Exception {
    long exp = 0x2ec3dd404a0a3000L;
    assertEquals(exp, idComputer.computeDocumentId(docRef, ""));
    assertEquals(exp, idComputer.computeDocumentId(docRef, " "));
    assertEquals(exp, idComputer.computeDocumentId(docRef, null));
    assertFalse(exp == idComputer.computeDocumentId(docRef, "de"));
  }

  @Test
  public void test_computeDocumentId_doc() throws Exception {
    XWikiDocument doc = new XWikiDocument(docRef);
    doc.setLanguage(lang);
    assertEquals(0x30da7f3f8545d000L, idComputer.computeDocumentId(doc));
  }

}
