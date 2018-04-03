package com.celements.mandatory;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.web.Utils;

public class XWikiRecycleBinIndexesTest extends AbstractComponentTest {

  private XWikiRecycleBinIndexes mandatory;
  private IQueryExecutionServiceRole queryExecServiceMock;
  private WikiReference wikiRef;

  @Before
  public void prepareTest() throws Exception {
    queryExecServiceMock = registerComponentMock(IQueryExecutionServiceRole.class);
    mandatory = (XWikiRecycleBinIndexes) Utils.getComponent(IMandatoryDocumentRole.class,
        XWikiRecycleBinIndexes.NAME);
    wikiRef = new WikiReference("wiki");
    getContext().setDatabase(wikiRef.getName());
  }

  @Test
  public void test_dependsOnMandatoryDocuments() throws Exception {
    assertEquals(0, mandatory.dependsOnMandatoryDocuments().size());
  }

  @Test
  public void test_checkDocuments() throws Exception {
    String sql = mandatory.getAddSql();
    expect(queryExecServiceMock.existsIndex(wikiRef, mandatory.getTableName(),
        mandatory.getIndexName())).andReturn(false).once();
    expect(queryExecServiceMock.executeWriteSQL(sql)).andReturn(0).once();

    replayDefault();
    mandatory.checkDocuments();
    verifyDefault();
  }

  @Test
  public void test_checkDocuments_indexExists() throws Exception {
    expect(queryExecServiceMock.existsIndex(wikiRef, mandatory.getTableName(),
        mandatory.getIndexName())).andReturn(true).once();

    replayDefault();
    mandatory.checkDocuments();
    verifyDefault();
  }

  @Test
  public void test_getTableName() throws Exception {
    assertEquals("xwikirecyclebin", mandatory.getTableName());
  }

  @Test
  public void test_getIndexName() throws Exception {
    assertEquals("dateIDX", mandatory.getIndexName());
  }

  @Test
  public void test_getAddSql() throws Exception {
    String expSQL = "alter table xwikirecyclebin add index `dateIDX` (XDD_DATE, XDD_ID)";
    assertEquals(expSQL, mandatory.getAddSql());
  }

}
