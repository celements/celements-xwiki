package org.xwiki.model.reference;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.xpn.xwiki.objects.BaseObject;

public class ImmutableObjectReferenceTest extends AbstractComponentTest {

  private ImmutableObjectReference objRef;

  private static final ImmutableObjectReference create() {
    return new ImmutableObjectReference(new DocumentReference("wiki", "space", "doc"),
        new ClassReference("classes", "class"), 5);
  }

  @Before
  public void prepareTest() {
    objRef = create();
  }

  @Test
  public void test_getDocumentReference() {
    assertEquals(new DocumentReference("wiki", "space", "doc"), objRef.getDocumentReference());
  }

  @Test
  public void test_getClassReference() {
    assertEquals(new ClassReference("classes", "class"), objRef.getClassReference());
  }

  @Test
  public void test_getNumber() {
    assertEquals(5, objRef.getNumber());
  }

  @Test
  public void test_equals() {
    assertEquals(objRef, create());
  }

  @Test
  public void test_hashCode() {
    assertEquals(objRef.hashCode(), create().hashCode());
  }

  @Test
  public void test_serialize() {
    assertEquals("wiki:space.doc_classes.class_5", objRef.serialize());
  }

  public void tesst_from_BaseObject() {
    BaseObject obj = new BaseObject();
    obj.setDocumentReference(objRef.getDocumentReference());
    obj.setXClassReference(objRef.getClassReference());
    obj.setNumber(objRef.getNumber());
    assertEquals(objRef, ImmutableObjectReference.from(obj));
  }

}
