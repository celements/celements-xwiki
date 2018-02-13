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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
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

    Builder<List<String>> builder = ImmutableList.builder();
    builder.add(ImmutableList.of("xwikiclasses", "XWO_ID"));
    builder.add(ImmutableList.of("xwikiclassesprop", "XWP_ID"));
    builder.add(ImmutableList.of("xwikiobjects", "XWO_ID"));
    builder.add(ImmutableList.of("xwikiproperties", "XWP_ID"));
    builder.add(ImmutableList.of("xwikistatsdoc", "XWS_ID"));
    builder.add(ImmutableList.of("xwikistatsreferer", "XWR_ID"));
    builder.add(ImmutableList.of("xwikistatsvisit", "XWV_ID"));
    builder.add(ImmutableList.of("xwikistrings", "XWS_ID"));
    builder.add(ImmutableList.of("xwikilists", "XWL_ID"));
    builder.add(ImmutableList.of("xwikilistitems", "XWL_ID"));
    migration.injectIdColumns(builder.build());
  }

  @Test
  public void test_migrate() throws Exception {
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn(PREFIX).anyTimes();
    expectModify("xwikiclasses");
    expectModify("xwikiclassesprop");
    expectModify("xwikiobjects");
    expectModifyFkProperties();
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
    expect(queryExecMock.executeWriteSQL(migration.getModifyIdColumnSql(table))).andReturn(
        1).once();
  }

  private void expectModifyFkProperties() throws XWikiException {
    String table = "xwikiproperties";
    String fkTable1 = "xwikistrings";
    String fkName1 = "FK2780715A3433FD87";
    String fkTable2 = "xwikilists";
    String fkName2 = "FKDF25AE4F283EE295";
    Builder<List<String>> fks = ImmutableList.builder();
    fks.add(Arrays.asList(fkName1, fkTable1, "XWS_ID", "XWP_ID"));
    fks.add(Arrays.asList(fkName1, fkTable1, "XWS_NAME", "XWP_NAME"));
    fks.add(Arrays.asList(fkName2, fkTable2, "XWL_ID", "XWP_ID"));
    fks.add(Arrays.asList(fkName2, fkTable2, "XWL_NAME", "XWP_NAME"));
    expect(queryExecMock.executeReadSql(String.class, migration.getLoadForeignKeysSql(table, PREFIX
        + getContext().getDatabase()))).andReturn(fks.build()).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " drop foreign key "
        + fkName1)).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable2 + " drop foreign key "
        + fkName2)).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL(migration.getModifyIdColumnSql(table))).andReturn(
        1).once();
    expectModify(fkTable1);
    expectModifyFkLists();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " add constraint " + fkName1
        + " foreign key (XWS_ID,XWS_NAME) references " + table + " (XWP_ID,XWP_NAME)")).andReturn(
            1).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable2 + " add constraint " + fkName2
        + " foreign key (XWL_ID,XWL_NAME) references " + table + " (XWP_ID,XWP_NAME)")).andReturn(
            1).once();
  }

  private void expectModifyFkLists() throws XWikiException {
    String table = "xwikilists";
    String fkTable = "xwikilistitems";
    String fkName = "FKC0862BA3FB72A11";
    Builder<List<String>> fks = ImmutableList.builder();
    fks.add(Arrays.asList(fkName, fkTable, "XWL_ID", "XWL_ID"));
    fks.add(Arrays.asList(fkName, fkTable, "XWL_NAME", "XWL_NAME"));
    expect(queryExecMock.executeReadSql(String.class, migration.getLoadForeignKeysSql(table, PREFIX
        + getContext().getDatabase()))).andReturn(fks.build()).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable + " drop foreign key "
        + fkName)).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL(migration.getModifyIdColumnSql(table))).andReturn(
        1).once();
    expectModify("xwikilistitems");
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable + " add constraint " + fkName
        + " foreign key (XWL_ID,XWL_NAME) references " + table + " (XWL_ID,XWL_NAME)")).andReturn(
            1).once();
  }

  @Test
  public void test_getModifyIdSql() throws Exception {
    assertEquals("alter table xwikiobjects modify column XWO_ID bigint not null",
        migration.getModifyIdColumnSql("xwikiobjects"));
  }

  @Test
  public void test_getLoadForeignKeysSql() throws Exception {
    assertEquals("select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME "
        + "from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where TABLE_SCHEMA = 'db' and "
        + "REFERENCED_TABLE_NAME = 'xwikiproperties' order by CONSTRAINT_NAME, ORDINAL_POSITION",
        migration.getLoadForeignKeysSql("xwikiproperties", "db"));
  }

  @Test
  public void test_getLoadColumnsSql() throws Exception {
    assertEquals("select TABLE_NAME, COLUMN_NAME "
        + "from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where TABLE_SCHEMA = 'db' and "
        + "CONSTRAINT_NAME = 'PRIMARY' and COLUMN_NAME like '%_ID'", migration.getLoadColumnsSql(
            "db"));
  }

}
