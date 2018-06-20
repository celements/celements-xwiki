package com.celements.model.migration;

import static com.celements.common.test.CelementsTestUtils.*;
import static com.celements.model.migration.DanglingPropertiesMigration.*;
import static com.celements.model.migration.InformationSchema.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.RootClass;
import org.hibernate.mapping.Table;
import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.web.Utils;

public class DanglingPropertiesMigrationTest extends AbstractComponentTest {

  private DanglingPropertiesMigration migration;
  private IQueryExecutionServiceRole queryExecMock;
  private Configuration hibCfgMock;
  private Builder<List<String>> idColumnBuilder;

  @Before
  public void prepareTest() throws Exception {
    queryExecMock = registerComponentMock(IQueryExecutionServiceRole.class);
    hibCfgMock = createMockAndAddToDefault(Configuration.class);
    registerComponentMocks(HibernateSessionFactory.class, ModelUtils.class);
    expect(getMock(HibernateSessionFactory.class).getConfiguration()).andReturn(
        hibCfgMock).anyTimes();
    migration = (DanglingPropertiesMigration) Utils.getComponent(ICelementsMigrator.class, NAME);
    idColumnBuilder = ImmutableList.builder();
  }

  @Test
  public void test_migrate_noMappings() throws Exception {
    expect(hibCfgMock.getClassMappings()).andReturn(Collections.emptyList().iterator()).once();
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  public void test_migrate_noTableSchemaData() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator()).once();
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  public void test_migrate_noDanglingProperties() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator()).once();
    addColumn(table);
    expect(queryExecMock.executeReadSql(getSelectSql(table, createIdColumnName(table),
        className))).andReturn(Collections.<List<String>>emptyList());
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  public void test_migrate() throws Exception {
    String table = "tbl";
    String className = "Classes.Class";
    PersistentClass mapping = createMapping(className, table);
    expect(hibCfgMock.getClassMappings()).andReturn(Arrays.asList(mapping).iterator()).once();
    addColumn(table);
    expect(queryExecMock.executeReadSql(getSelectSql(table, createIdColumnName(table),
        className))).andReturn(Arrays.asList(Arrays.asList("data")));
    expectInformationSchemaLoad();

    replayDefault();
    migration.migrate(null, getContext());
    verifyDefault();
  }

  private PersistentClass createMapping(String entityName, String tableName) {
    RootClass mapping = new RootClass();
    mapping.setEntityName(entityName);
    Table table = new Table(tableName);
    mapping.setTable(table);
    return mapping;
  }

  private void addColumn(String table) {
    idColumnBuilder.add(ImmutableList.of(table, createIdColumnName(table), "long"));
  }

  private void expectInformationSchemaLoad() throws XWikiException {
    String database = "db";
    expect(getMock(ModelUtils.class).getDatabaseName(Utils.getComponent(
        ModelContext.class).getWikiRef())).andReturn(database).anyTimes();
    expect(queryExecMock.executeReadSql(getLoadColumnsSql(database))).andReturn(
        idColumnBuilder.build()).times(0, 1);
  }

  public void test_getSelectSql() throws Exception {
    replayDefault();
    assertEquals("select * from tbl left join xwikiobjects on XWO_ID = TBL_ID where XWO_ID is null "
        + "or XWO_CLASSNAME <> 'Classes.Class' order by XWO_CLASSNAME, XWO_NAME", getSelectSql(
            "tbl", createIdColumnName("tbl"), "Classes.Class"));
    verifyDefault();
  }

  @Test
  public void test_getDeleteSql() throws Exception {
    replayDefault();
    assertEquals("delete tbl from tbl left join xwikiobjects on XWO_ID = TBL_ID "
        + "where XWO_ID is null or XWO_CLASSNAME <> 'Classes.Class'", getDeleteSql("tbl",
            createIdColumnName("tbl"), "Classes.Class"));
    verifyDefault();
  }

  private String createIdColumnName(String table) {
    return table.toUpperCase() + "_ID";
  }

}
