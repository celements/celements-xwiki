package com.celements.model.migration;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.migration.DanglingObjectsMigration.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

public class DanglingObjectsMigrationTest extends AbstractComponentTest {

  private DanglingObjectsMigration migration;
  private IQueryExecutionServiceRole queryExecMock;

  @Before
  public void prepareTest() throws Exception {
    queryExecMock = registerComponentMock(IQueryExecutionServiceRole.class);
    migration = (DanglingObjectsMigration) Utils.getComponent(ICelementsMigrator.class, NAME);
  }

  @Test
  public void test_migrate_noDanglingObjects() throws Exception {
    expect(queryExecMock.executeReadSql(getSelectSql())).andReturn(
        Collections.<List<String>>emptyList());

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate() throws Exception {
    expect(queryExecMock.executeReadSql(getSelectSql())).andReturn(Arrays.asList(Arrays.asList(
        "data1"), Arrays.asList("data2")));
    expect(queryExecMock.executeWriteSQL(getDeleteSql())).andReturn(2);

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_XWE() throws Exception {
    XWikiException xwe = new XWikiException();
    expect(queryExecMock.executeReadSql(getSelectSql())).andThrow(xwe);

    replayDefault();
    new ExceptionAsserter<XWikiException>(XWikiException.class, xwe) {

      @Override
      protected void execute() throws Exception {
        migration.migrate(null, getContext());
      }
    }.evaluate();
    verifyDefault();
  }

  @Test
  public void test_getSelectSql() throws Exception {
    assertEquals("select XWO_ID, XWO_NAME, XWO_CLASSNAME, XWO_NUMBER from xwikiobjects left join "
        + "xwikidoc on XWO_NAME = XWD_FULLNAME where XWD_FULLNAME is null", getSelectSql());
  }

  @Test
  public void test_getDeleteSql() throws Exception {
    replayDefault();
    assertEquals("delete xwikiobjects from xwikiobjects left join xwikidoc on XWO_NAME = "
        + "XWD_FULLNAME where XWD_FULLNAME is null", getDeleteSql());
    verifyDefault();
  }

}
