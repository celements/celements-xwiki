package com.celements.model.migration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.SimpleValue;
import org.hibernate.type.LongType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.model.context.ModelContext;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component(BaseCollectionIdColumnMigration.NAME)
public class BaseCollectionIdColumnMigration extends AbstractCelementsHibernateMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      BaseCollectionIdColumnMigration.class);

  public static final String NAME = "BaseCollectionIdColumnMigration";

  static final List<String> XWIKI_TABLES = ImmutableList.of("xwikiclasses", "xwikiclassesprop",
      "xwikiobjects", "xwikiproperties", "xwikistatsdoc", "xwikistatsreferer", "xwikistatsvisit");

  IdColumnLoader idColumns = new IdColumnLoader();

  @Requirement
  private HibernateSessionFactory sessionFactory;

  @Requirement
  private IQueryExecutionServiceRole queryExecutor;

  @Requirement
  private ModelContext context;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "migrates id columns for BaseCollections from int to bigint";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this migration
   * 14.02.2018 -> 2966 http://www.wolframalpha.com/input/?i=days+since+01.01.2010
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(2966);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext xwikiContext)
      throws XWikiException {
    LOGGER.info("migrating id columns for database [{}]", getDatabaseWithPrefix());
    try {
      idColumns.load();
      migrateXWikiTables();
      migrateMappedTables();
    } catch (Exception exc) {
      LOGGER.error("Failed to migrate id columns for database [{}]", getDatabaseWithPrefix(), exc);
      throw exc;
    } finally {
      idColumns.clear();
    }
  }

  private void migrateXWikiTables() throws XWikiException {
    for (String table : XWIKI_TABLES) {
      LOGGER.info("[{}] try id column migration for xwiki table", table);
      migrateTable(table);
    }
  }

  @SuppressWarnings("unchecked")
  private void migrateMappedTables() throws XWikiException {
    for (Iterator<PersistentClass> iter = getHibConfig().getClassMappings(); iter.hasNext();) {
      PersistentClass mapping = iter.next();
      String table = mapping.getTable().getName();
      LOGGER.info("[{}] try id column migration for mapped table", table);
      if (isMappingWithLongPrimaryKey(mapping)) {
        migrateTable(table);
      } else {
        LOGGER.info("[{}] skip table, id isn't mapped as long", table);
      }
    }
  }

  private boolean isMappingWithLongPrimaryKey(PersistentClass mapping) {
    try {
      return (((SimpleValue) mapping.getIdentifier()).getType().getClass() == LongType.class);
    } catch (ClassCastException | NullPointerException exc) {
      return false;
    }
  }

  private void migrateTable(String table) throws XWikiException {
    Collection<ForeignKey> droppedForeignKeys = new HashSet<>();
    try {
      String sql = getModifyIdColumnSql(table);
      dropReferencingForeignKeys(table, droppedForeignKeys);
      for (ForeignKey fk : droppedForeignKeys) {
        migrateTable(fk.getTable());
      }
      int count = queryExecutor.executeWriteSQL(sql);
      LOGGER.info("[{}] updated id column for {} rows", table, count);
    } catch (IllegalArgumentException iae) {
      LOGGER.info("[{}] skip table, id column isn't int", table, iae);
    } finally {
      addForeignKeys(table, droppedForeignKeys);
    }
  }

  String getModifyIdColumnSql(String table) throws IllegalArgumentException {
    return "alter table " + table + " modify column " + idColumns.get(table) + " bigint not null";
  }

  private void addForeignKeys(String table, Collection<ForeignKey> foreignKeys) {
    if (foreignKeys.size() > 0) {
      LOGGER.info("[{}] add {} FKs", table, foreignKeys.size());
    }
    for (ForeignKey fk : foreignKeys) {
      try {
        LOGGER.debug("[{}] adding {}", table, fk);
        queryExecutor.executeWriteSQL(fk.getAddSql());
      } catch (XWikiException xwe) {
        LOGGER.error("[{}] failed to add {}", table, fk, xwe);
      }
    }
  }

  private void dropReferencingForeignKeys(String table, Collection<ForeignKey> droppedForeignKeys)
      throws XWikiException {
    for (ForeignKey fk : loadForeignKeys(table)) {
      LOGGER.debug("[{}] dropping {}", table, fk);
      queryExecutor.executeWriteSQL(fk.getDropSql());
      droppedForeignKeys.add(fk);
    }
    if (droppedForeignKeys.size() > 0) {
      LOGGER.info("[{}] dropped {} FKs", table, droppedForeignKeys.size());
    }
  }

  private Collection<ForeignKey> loadForeignKeys(String table) throws XWikiException {
    String sql = getLoadForeignKeysSql(table, getDatabaseWithPrefix());
    Map<String, ForeignKey> map = new HashMap<>();
    for (List<String> row : queryExecutor.executeReadSql(String.class, sql)) {
      String name = row.get(0);
      ForeignKey fk = map.get(name);
      if (fk == null) {
        map.put(name, fk = new ForeignKey(name, row.get(1), table));
      }
      fk.addColumn(row.get(2), row.get(3));
    }
    if (map.size() > 0) {
      LOGGER.trace("[{}] loaded {} FKs", table, map.size());
    }
    return map.values();
  }

  static String getLoadForeignKeysSql(String table, String database) {
    return "select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME "
        + "from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where TABLE_SCHEMA = '" + database
        + "' and REFERENCED_TABLE_NAME = '" + table
        + "' order by CONSTRAINT_NAME, ORDINAL_POSITION";
  }

  String getDatabaseWithPrefix() {
    return context.getXWikiContext().getWiki().Param("xwiki.db.prefix", "")
        + context.getWikiRef().getName();
  }

  private Configuration getHibConfig() {
    return sessionFactory.getConfiguration();
  }

  /**
   * loads all id columns of type 'int' for current database
   */
  class IdColumnLoader {

    private Map<String, String> map = ImmutableMap.of();

    public String get(String table) throws IllegalArgumentException {
      if (map.containsKey(table)) {
        return map.get(table);
      } else {
        throw new IllegalArgumentException("no integer id column for table [" + table + "]");
      }
    }

    public void load() throws XWikiException {
      map = loadIdColumnNames();
    }

    public void clear() {
      map = ImmutableMap.of();
    }

    private Map<String, String> loadIdColumnNames() throws XWikiException {
      String sql = getLoadColumnsSql(getDatabaseWithPrefix());
      Builder<String, String> builder = ImmutableMap.builder();
      for (List<String> row : queryExecutor.executeReadSql(String.class, sql)) {
        builder.put(row.get(0), row.get(1));
      }
      return builder.build();
    }

  }

  static String getLoadColumnsSql(String database) {
    String select = "select k.TABLE_NAME, k.COLUMN_NAME";
    String from = " from INFORMATION_SCHEMA.KEY_COLUMN_USAGE as k, INFORMATION_SCHEMA.COLUMNS as c";
    String where = " where k.TABLE_SCHEMA = '" + database + "' and k.CONSTRAINT_NAME = 'PRIMARY'"
        + " and k.COLUMN_NAME like '%_ID' and c.DATA_TYPE = 'int'";
    String whereJoin = " and k.TABLE_SCHEMA = c.TABLE_SCHEMA and k.TABLE_NAME = c.TABLE_NAME"
        + " and k.COLUMN_NAME = c.COLUMN_NAME";
    String groupBy = " group by k.TABLE_NAME";
    return select + from + where + whereJoin + groupBy;
  }

}
