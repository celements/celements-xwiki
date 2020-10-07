package com.celements.rights.function;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.rights.function.FunctionRightsAccess.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;
import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.ModelAccessStrategy;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.rights.classes.FunctionRightsClass;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class DefaultFunctionRightsAccessTest extends AbstractComponentTest {

  private String currentDb;
  private DefaultFunctionRightsAccess functionRightsAccess;

  @Before
  public void prepare() throws Exception {
    registerComponentMocks(
        IRightsAccessFacadeRole.class,
        ModelAccessStrategy.class);
    functionRightsAccess = (DefaultFunctionRightsAccess) Utils
        .getComponent(FunctionRightsAccess.class);
    getContext().setDatabase(currentDb = "current");
  }

  @Test
  public void test_getGroupsWithAccess() {
    String functionName = "editSth";
    String group = "XWiki.SomeGroup";
    expectFunctionDoc(new DocumentReference(currentDb, SPACE_NAME, functionName), group);
    expectFunctionDoc(new DocumentReference("xwiki", SPACE_NAME, functionName), null);

    replayDefault();
    assertEquals(ImmutableSet.of(getModelUtils().resolveRef(group)),
        functionRightsAccess.getGroupsWithAccess(functionName));
    verifyDefault();
  }

  @Test
  public void test_getGroupsWithAccess_inMainWiki() {
    String functionName = "editSth";
    String group = "XWiki.SomeGroup";
    expectFunctionDoc(new DocumentReference(currentDb, SPACE_NAME, functionName), null);
    expectFunctionDoc(new DocumentReference("xwiki", SPACE_NAME, functionName), group);

    replayDefault();
    assertEquals(ImmutableSet.of(getModelUtils().resolveRef(group)),
        functionRightsAccess.getGroupsWithAccess(functionName));
    verifyDefault();
  }

  @Test
  public void test_getGroupsWithAccess_inLocalAndMainWiki() {
    String functionName = "editSth";
    String group = "XWiki.SomeGroup";
    String groupMain = "XWiki.SomeMainGroup";
    expectFunctionDoc(new DocumentReference(currentDb, SPACE_NAME, functionName), group);
    expectFunctionDoc(new DocumentReference("xwiki", SPACE_NAME, functionName), groupMain);

    replayDefault();
    assertEquals(ImmutableSet.of(getModelUtils().resolveRef(group),
        getModelUtils().resolveRef(groupMain)),
        functionRightsAccess.getGroupsWithAccess(functionName));
    verifyDefault();
  }

  @Test
  public void test_getGroupsWithAccess_none() {
    String functionName = "editSth";
    expectFunctionDoc(new DocumentReference(currentDb, SPACE_NAME, functionName), null);
    expectFunctionDoc(new DocumentReference("xwiki", SPACE_NAME, functionName), "");

    replayDefault();
    assertEquals(ImmutableSet.of(),
        functionRightsAccess.getGroupsWithAccess(functionName));
    verifyDefault();
  }

  @Test
  public void test_hasUserAccess() {
    String functionName = "editSth";
    String group = "XWiki.SomeGroup";
    expectFunctionDoc(new DocumentReference(currentDb, SPACE_NAME, functionName), null);
    expectFunctionDoc(new DocumentReference("xwiki", SPACE_NAME, functionName), group);
    User userMock = createMockAndAddToDefault(User.class);
    expect(getMock(IRightsAccessFacadeRole.class).isInGroup(getModelUtils().resolveRef(group,
        DocumentReference.class), userMock)).andReturn(true);

    replayDefault();
    assertTrue(functionRightsAccess.hasUserAccess(userMock, functionName));
    verifyDefault();
  }

  @Test
  public void test_hasUserAccess_notInGroup() {
    String functionName = "editSth";
    String group = "XWiki.SomeGroup";
    expectFunctionDoc(new DocumentReference(currentDb, SPACE_NAME, functionName), null);
    expectFunctionDoc(new DocumentReference("xwiki", SPACE_NAME, functionName), group);
    User userMock = createMockAndAddToDefault(User.class);
    expect(getMock(IRightsAccessFacadeRole.class).isInGroup(getModelUtils().resolveRef(group,
        DocumentReference.class), userMock)).andReturn(false);

    replayDefault();
    assertFalse(functionRightsAccess.hasUserAccess(userMock, functionName));
    verifyDefault();
  }

  @Test
  public void test_hasUserAccess_noFunctionRightSet() {
    String functionName = "editSth";
    expectFunctionDoc(new DocumentReference(currentDb, SPACE_NAME, functionName), null);
    expectFunctionDoc(new DocumentReference("xwiki", SPACE_NAME, functionName), null);
    User userMock = createMockAndAddToDefault(User.class);

    replayDefault();
    assertFalse(functionRightsAccess.hasUserAccess(userMock, functionName));
    verifyDefault();
  }

  private XWikiDocument expectFunctionDoc(DocumentReference docRef, String group) {
    XWikiDocument doc = new XWikiDocument(docRef);
    if (group != null) {
      BaseObject obj = new BaseObject();
      obj.setDocumentReference(docRef);
      obj.setXClassReference(FunctionRightsClass.CLASS_REF);
      obj.setStringValue(FunctionRightsClass.FIELD_GROUP.getName(), group);
      doc.addXObject(obj);
    }
    expect(getMock(ModelAccessStrategy.class).exists(docRef, "")).andReturn(true);
    expect(getMock(ModelAccessStrategy.class).getDocument(docRef, "")).andReturn(doc);
    return doc;
  }

  private static final ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

}
