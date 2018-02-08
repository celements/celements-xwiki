package com.celements.model.migration;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

public class BaseCollectionIdMigrationTest extends AbstractComponentTest {

  private BaseCollectionIdMigration migration;
  private IQueryExecutionServiceRole queryExecMock;

  @Before
  public void prepareTest() throws Exception {
    queryExecMock = registerComponentMock(IQueryExecutionServiceRole.class);
    migration = (BaseCollectionIdMigration) Utils.getComponent(ICelementsMigrator.class,
        BaseCollectionIdMigration.NAME);

  }

  @Test
  public void test_migrate() throws Exception {
    expectSql("xwikiclasses", "XWO_ID");
    expectSql("xwikiclassesprop", "XWP_ID");
    expectSql("xwikiobjects", "XWO_ID");
    expectSql("xwikiproperties", "XWP_ID");
    expectSql("xwikistatsdoc", "XWS_ID");
    expectSql("xwikistatsreferer", "XWR_ID");
    expectSql("xwikistatsvisit", "XWV_ID");
    replayDefault();
    migration.migrate(null, null);
    verifyDefault();
  }

  private void expectSql(String table, String column) throws XWikiException {
    expect(queryExecMock.executeWriteSQL(migration.createAlterSql(table, column))).andReturn(
        1).once();
  }

  @Test
  public void test_createAlterSql() throws Exception {
    assertEquals("alter table tbl modify column col bigint not null", migration.createAlterSql(
        "tbl", "col"));
  }

}
