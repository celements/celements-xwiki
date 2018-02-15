package com.celements.model.migration;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.migration.BaseCollectionIdMigration.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.easymock.IExpectationSetters;
import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.mapping.Table;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.hibernate.type.Type;
import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.common.test.ExceptionAsserter;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.web.Utils;

public class BaseCollectionIdMigrationTest extends AbstractComponentTest {

  private static final String PREFIX = "cel_";

  private BaseCollectionIdMigration migration;
  private IQueryExecutionServiceRole queryExecMock;
  private Configuration hibCfgMock;
  private Builder<List<String>> idColumnBuilder;

  @Before
  public void prepareTest() throws Exception {
    expect(getWikiMock().Param("xwiki.db.prefix", "")).andReturn(PREFIX).anyTimes();
    queryExecMock = registerComponentMock(IQueryExecutionServiceRole.class);
    hibCfgMock = createMockAndAddToDefault(Configuration.class);
    expect(registerComponentMock(HibernateSessionFactory.class).getConfiguration()).andReturn(
        hibCfgMock).anyTimes();
    migration = (BaseCollectionIdMigration) Utils.getComponent(ICelementsMigrator.class, NAME);
    idColumnBuilder = ImmutableList.builder();
  }

  @Test
  public void test_migrate_xwikitables() throws Exception {
    expectModifyIdColumn("xwikiclasses");
    expectModifyIdColumn("xwikiclassesprop");
    expectModifyIdColumn("xwikiobjects");
    expectModifyFkProperties();
    expectModifyIdColumn("xwikistatsdoc");
    expectModifyIdColumn("xwikistatsreferer");
    expectModifyIdColumn("xwikistatsvisit");
    expect(hibCfgMock.getClassMappings()).andReturn(ImmutableList.of().iterator()).once();
    expectIdColumnLoad();
    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  @Test
  public void test_migrate_xwikitables_fail() throws Exception {
    expectModifyIdColumn("xwikiclasses");
    expectModifyIdColumn("xwikiclassesprop");
    expectModifyIdColumn("xwikiobjects");
    expectModifyFkPropertiesFail();
    expectIdColumnLoad();
    replayDefault();
    new ExceptionAsserter<XWikiException>(XWikiException.class) {

      @Override
      protected void execute() throws Exception {
        migration.migrate(null, getContext());
      }

    }.evaluate();
    verifyDefault();
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
    expectModifyIdColumn(table, fks.build(), 1);
    expectModifyIdColumn(fkTable1);
    expectModifyFkLists();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " drop foreign key "
        + fkName1)).andReturn(1).once();
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable2 + " drop foreign key "
        + fkName2)).andReturn(1).once();
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
    expectModifyIdColumn(table, fks.build(), 1);
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable + " drop foreign key "
        + fkName)).andReturn(1).once();
    expectModifyIdColumn("xwikilistitems");
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable + " add constraint " + fkName
        + " foreign key (XWL_ID,XWL_NAME) references " + table + " (XWL_ID,XWL_NAME)")).andReturn(
            1).once();
  }

  private void expectModifyFkPropertiesFail() throws XWikiException {
    String table = "xwikiproperties";
    String fkTable1 = "xwikistrings";
    String fkName1 = "FK2780715A3433FD87";
    Builder<List<String>> fks = ImmutableList.builder();
    fks.add(Arrays.asList(fkName1, fkTable1, "XWS_ID", "XWP_ID"));
    fks.add(Arrays.asList(fkName1, fkTable1, "XWS_NAME", "XWP_NAME"));
    expectModifyIdColumn(table, fks.build(), new XWikiException());
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " drop foreign key "
        + fkName1)).andReturn(1).once();
    // FK has to be readded, even after exception occured
    expect(queryExecMock.executeWriteSQL("alter table " + fkTable1 + " add constraint " + fkName1
        + " foreign key (XWS_ID,XWS_NAME) references " + table + " (XWP_ID,XWP_NAME)")).andReturn(
            1).once();
  }

  @Test
  public void test_migrate_mappedtables() throws Exception {
    for (String table : XWIKI_TABLES) {
      expectModifyIdColumn(table);
    }
    String table = "table_mapped";
    Builder<PersistentClass> builder = ImmutableList.builder();
    builder.add(createMapping("entity", table, LongType.class));
    builder.add(createMapping("entity_Integer", table + "_int", IntegerType.class));
    builder.add(createMapping("entity_String", table + "_str", StringType.class));
    builder.add(createMapping("entity_None", table + "_none", null));
    expectModifyIdColumn(table);
    expect(hibCfgMock.getClassMappings()).andReturn(builder.build().iterator()).once();
    expectIdColumnLoad();
    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  private PersistentClass createMapping(String entityName, String tableName,
      Class<? extends Type> hibType) {
    RootClass mapping = new RootClass();
    mapping.setEntityName(entityName);
    Table table = new Table(tableName);
    mapping.setTable(table);
    if (hibType != null) {
      SimpleValue identifier = new SimpleValue(table);
      identifier.setTypeName(hibType.getName());
      mapping.setIdentifier(identifier);
    }
    return mapping;
  }

  private void expectModifyIdColumn(String table) throws XWikiException {
    expectModifyIdColumn(table, Collections.<List<String>>emptyList(), 1);
  }

  private void expectModifyIdColumn(String table, List<List<String>> fks, Object ret)
      throws XWikiException {
    addColumn(table);
    expect(queryExecMock.executeReadSql(String.class, getLoadForeignKeysSql(table, PREFIX
        + getContext().getDatabase()))).andReturn(fks).once();
    IExpectationSetters<Integer> exp = expect(queryExecMock.executeWriteSQL(
        getExpectedModifyIdColumnSql(table)));
    if (ret instanceof Integer) {
      exp.andReturn((Integer) ret).once();
    } else if (ret instanceof Throwable) {
      exp.andThrow((Throwable) ret).once();
    }
  }

  @Test
  public void test_getModifyIdColumnSql() throws Exception {
    String table = "xwikiobjects";
    addColumn(table);
    expectIdColumnLoad();

    replayDefault();
    migration.idColumns.load();
    assertEquals(getExpectedModifyIdColumnSql(table), migration.getModifyIdColumnSql(table));
    verifyDefault();
  }

  private String getExpectedModifyIdColumnSql(String table) {
    return "alter table " + table + " modify column " + createIdColumnName(table)
        + " bigint not null";
  }

  @Test
  public void test_getLoadForeignKeysSql() throws Exception {
    assertEquals("select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME "
        + "from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where TABLE_SCHEMA = 'db' and "
        + "REFERENCED_TABLE_NAME = 'xwikiproperties' order by CONSTRAINT_NAME, ORDINAL_POSITION",
        getLoadForeignKeysSql("xwikiproperties", "db"));
  }

  @Test
  public void test_getLoadColumnsSql() throws Exception {
    assertEquals("select k.TABLE_NAME, k.COLUMN_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE as k,"
        + " INFORMATION_SCHEMA.COLUMNS as c where k.TABLE_SCHEMA = 'db' and"
        + " k.CONSTRAINT_NAME = 'PRIMARY' and k.COLUMN_NAME like '%_ID' and c.DATA_TYPE = 'int' and"
        + " k.TABLE_SCHEMA = c.TABLE_SCHEMA and k.TABLE_NAME = c.TABLE_NAME and"
        + " k.COLUMN_NAME = c.COLUMN_NAME group by k.TABLE_NAME", getLoadColumnsSql("db"));
  }

  private void expectIdColumnLoad() throws XWikiException {
    expect(queryExecMock.executeReadSql(String.class, getLoadColumnsSql(PREFIX
        + getContext().getDatabase()))).andReturn(idColumnBuilder.build()).once();
  }

  private void addColumn(String table) {
    idColumnBuilder.add(ImmutableList.of(table, createIdColumnName(table)));
  }

  private String createIdColumnName(String table) {
    return table.toUpperCase() + "_ID";
  }

}
