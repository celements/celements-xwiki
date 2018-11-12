package com.celements.common.classes;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.ModelAccessStrategy;
import com.celements.model.classes.ClassDefinition;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

public class DefaultXClassCreatorTest2 extends AbstractComponentTest {

  private XClassCreator xClassCreator;
  private ModelAccessStrategy modelMock;

  @Before
  public void prepareTest() throws Exception {
    modelMock = registerComponentMock(ModelAccessStrategy.class);
    xClassCreator = Utils.getComponent(XClassCreator.class);
  }

  @Test
  public void test_createXClass_pagetype() throws Exception {
    ClassDefinition classDef = new TestPageTypeClass();
    XWikiDocument doc = new XWikiDocument(classDef.getDocRef());
    BaseClass bclass = new BaseClass();
    bclass.fromXML(getXClassXML("class_pagetype.xml"));
    bclass.setDocumentReference(classDef.getDocRef());
    bclass.setXClassReference(classDef.getDocRef());
    doc.setXClass(bclass);

    expect(modelMock.exists(classDef.getDocRef(), "")).andReturn(true);
    expect(modelMock.getDocument(classDef.getDocRef(), "")).andReturn(doc);

    replayDefault();
    xClassCreator.createXClass(classDef);
    verifyDefault();
  }

  @Test
  public void test_createXClass_webSearch() throws Exception {
    ClassDefinition classDef = new TestWebSearchClass();
    XWikiDocument doc = new XWikiDocument(classDef.getDocRef());
    BaseClass bclass = new BaseClass();
    bclass.fromXML(getXClassXML("class_websearch.xml"));
    bclass.setDocumentReference(classDef.getDocRef());
    bclass.setXClassReference(classDef.getDocRef());
    doc.setXClass(bclass);

    expect(modelMock.exists(classDef.getDocRef(), "")).andReturn(true);
    expect(modelMock.getDocument(classDef.getDocRef(), "")).andReturn(doc);

    replayDefault();
    xClassCreator.createXClass(classDef);
    verifyDefault();
  }

  private String getXClassXML(String fileName) throws IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(getClass().getClassLoader().getResourceAsStream(fileName), writer,
        StandardCharsets.UTF_8.name());
    return writer.toString();
  }

}
