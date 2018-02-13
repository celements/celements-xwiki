package com.celements.model.migration;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

public class BaseCollectionIdMigrationTest extends AbstractComponentTest {

  private static final String PREFIX = "cel_";

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
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn(PREFIX).anyTimes();
    expectModify("xwikiclasses");
    expectModify("xwikiclassesprop");
    expectModify("xwikiobjects");
    expectModifyWithFk("xwikiproperties");
    expectModify("xwikistatsdoc");
    expectModify("xwikistatsreferer");
    expectModify("xwikistatsvisit");
    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  private void expectModify(String table) throws XWikiException {
    List<List<String>> fks = new ArrayList<>();
    expect(queryExecMock.executeReadSql(String.class, migration.getLoadForeignKeysSql(table, PREFIX
        + getContext().getDatabase()))).andReturn(fks).once();
    expect(queryExecMock.executeWriteSQL(migration.getModifyIdSql(table))).andReturn(1).once();
  }

  private void expectModifyWithFk(String table) throws XWikiException {
    List<List<String>> fks = new ArrayList<>();
    fks.add(Arrays.asList("FK2780715A3433FD87", "xwikistrings", "XWS_ID", "XWP_ID"));
    fks.add(Arrays.asList("FK2780715A3433FD87", "xwikistrings", "XWS_NAME", "XWP_NAME"));
    fks.add(Arrays.asList("FKDF25AE4F283EE295", "xwikilongs", "XWL_ID", "XWP_ID"));
    fks.add(Arrays.asList("FKDF25AE4F283EE295", "xwikilongs", "XWL_NAME", "XWP_NAME"));
    expect(queryExecMock.executeReadSql(String.class, migration.getLoadForeignKeysSql(table, PREFIX
        + getContext().getDatabase()))).andReturn(fks).once();
    expect(queryExecMock.executeWriteSQL("alter table xwikistrings "
        + "drop foreign key FK2780715A3433FD87")).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL("alter table xwikilongs "
        + "drop foreign key FKDF25AE4F283EE295")).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL(migration.getModifyIdSql(table))).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL("alter table xwikistrings "
        + "add constraint FK2780715A3433FD87 foreign key (XWS_ID,XWS_NAME) references " + table
        + " (XWP_ID,XWP_NAME)")).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL("alter table xwikilongs "
        + "add constraint FKDF25AE4F283EE295 foreign key (XWL_ID,XWL_NAME) references " + table
        + " (XWP_ID,XWP_NAME)")).andReturn(1).once();
  }

  @Test
  public void test_getModifyIdSql() throws Exception {
    assertEquals("alter table xwikiobjects modify column XWO_ID bigint not null",
        migration.getModifyIdSql("xwikiobjects"));
  }

  @Test
  public void test_getLoadForeignKeysSql() throws Exception {
    assertEquals("select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME "
        + "from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where TABLE_SCHEMA = 'db' and "
        + "REFERENCED_TABLE_NAME = 'xwikiproperties' order by CONSTRAINT_NAME, ORDINAL_POSITION",
        migration.getLoadForeignKeysSql("xwikiproperties", "db"));
  }

}
