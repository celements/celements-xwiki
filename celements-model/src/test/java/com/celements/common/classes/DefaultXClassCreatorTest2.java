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

  private ModelAccessStrategy modelMock;

  @Before
  public void prepareTest() throws Exception {
    modelMock = registerComponentMock(ModelAccessStrategy.class);
  }

  @Test
  public void test_createXClass() throws Exception {
    ClassDefinition classDef = new TestPageTypeClass();
    XWikiDocument doc = new XWikiDocument(classDef.getDocRef());
    BaseClass bclass = new BaseClass();
    System.out.println(getXClassXML());
    bclass.fromXML(getXClassXML());
    bclass.setDocumentReference(classDef.getDocRef());
    bclass.setXClassReference(classDef.getDocRef());
    doc.setXClass(bclass);

    expect(modelMock.exists(classDef.getDocRef(), "")).andReturn(true);
    expect(modelMock.getDocument(classDef.getDocRef(), "")).andReturn(doc);

    replayDefault();
    Utils.getComponent(XClassCreator.class).createXClass(classDef);
    verifyDefault();
  }

  private String getXClassXML() throws IOException {
    String fileName = "pagetype_class.xml";
    StringWriter writer = new StringWriter();
    IOUtils.copy(getClass().getClassLoader().getResourceAsStream(fileName), writer,
        StandardCharsets.UTF_8.name());
    return writer.toString();
    // return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "" + "<class>"
    // + "<name>Celements2.PageType</name>" + "<customClass></customClass>"
    // + "<customMapping></customMapping>" + "<defaultViewSheet></defaultViewSheet>"
    // + "<defaultEditSheet></defaultEditSheet>" + "<defaultWeb></defaultWeb>"
    // + "<nameField></nameField>" + "<validationScript></validationScript>" + "<page_layout>"
    // + "<disabled>0</disabled>" + "<name>page_layout</name>" + "<number>2</number>"
    // + "<prettyName>Page Layout</prettyName>" + "<size>30</size>"
    // + "<unmodifiable>0</unmodifiable>"
    // + "<classType>com.xpn.xwiki.objects.classes.StringClass</classType>" + "</page_layout>"
    // + "<page_type>" + "<disabled>0</disabled>" + "<name>page_type</name>" + "<number>1</number>"
    // + "<prettyName>Page Type</prettyName>" + "<size>30</size>"
    // + "<unmodifiable>0</unmodifiable>"
    // + "<classType>com.xpn.xwiki.objects.classes.StringClass</classType>" + "</page_type>"
    // + "</class>";
  }

}
