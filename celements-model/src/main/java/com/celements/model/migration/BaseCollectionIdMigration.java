package com.celements.model.migration;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.model.context.ModelContext;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component(BaseCollectionIdMigration.NAME)
public class BaseCollectionIdMigration extends AbstractCelementsHibernateMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseCollectionIdMigration.class);

  public static final String NAME = "BaseCollectionIdMigration";

  private static final List<String> TABLES = ImmutableList.of("xwikiclasses", "xwikiclassesprop",
      "xwikiobjects", "xwikiproperties", "xwikistatsdoc", "xwikistatsreferer", "xwikistatsvisit");
  private static final Map<String, String> TABLE_ID_MAP;

  static {
    Builder<String, String> builder = ImmutableMap.builder();
    builder.put("xwikiclasses", "XWO_ID");
    builder.put("xwikiclassesprop", "XWP_ID");
    builder.put("xwikiobjects", "XWO_ID");
    builder.put("xwikiproperties", "XWP_ID");
    builder.put("xwikistatsdoc", "XWS_ID");
    builder.put("xwikistatsreferer", "XWR_ID");
    builder.put("xwikistatsvisit", "XWV_ID");
    TABLE_ID_MAP = builder.build();
    checkState(TABLE_ID_MAP.keySet().containsAll(TABLES));
  }

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
    return "changes id columns for BaseCollections from int to bigint";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this migration
   * 13.02.2018 -> 2965 http://www.wolframalpha.com/input/?i=days+since+01.01.2010
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(2965);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext xwikiContext)
      throws XWikiException {
    try {
      for (String table : TABLES) {
        LOGGER.info("migrating id for [{}]", table);
        migrateTable(table);
      }
    } catch (Exception exc) {
      LOGGER.error("Failed to migrate database [{}]", context.getWikiRef(), exc);
      throw exc;
    }
  }

  private void migrateTable(String table) throws XWikiException {
    Collection<ForeignKey> droppedForeignKeys = new HashSet<>();
    try {
      dropReferencingForeignKeys(table, droppedForeignKeys);
      int count = queryExecutor.executeWriteSQL(getModifyIdSql(table));
      LOGGER.debug("updated [{}] id for {} rows", table, count);
    } finally {
      readdDroppedForeignKeys(droppedForeignKeys);
    }
  }

  String getModifyIdSql(String table) {
    return "alter table " + table + " modify column " + TABLE_ID_MAP.get(table)
        + " bigint not null";
  }

  private void dropReferencingForeignKeys(String table, Collection<ForeignKey> droppedForeignKeys)
      throws XWikiException {
    for (ForeignKey fk : loadForeignKeys(table)) {
      LOGGER.debug("dropping {}", fk);
      queryExecutor.executeWriteSQL(fk.getDropForeignKeySql());
      droppedForeignKeys.add(fk);
    }
  }

  private void readdDroppedForeignKeys(Collection<ForeignKey> droppedForeignKeys) {
    for (ForeignKey fk : droppedForeignKeys) {
      try {
        LOGGER.debug("adding {}", fk);
        queryExecutor.executeWriteSQL(fk.getAddForeignKeySql());
      } catch (XWikiException xwe) {
        LOGGER.error("failed to readd {}", fk, xwe);
      }
    }
  }

  private Collection<ForeignKey> loadForeignKeys(String table) throws XWikiException {
    String sql = getLoadForeignKeysSql(table, getDatabaseWithPrefix());
    LOGGER.trace("loadForeignKeys - {}", sql);
    Map<String, ForeignKey> map = new HashMap<>();
    for (List<String> row : queryExecutor.executeReadSql(String.class, sql)) {
      LOGGER.trace("loadForeignKeys - row: {}", row);
      String name = row.get(0);
      ForeignKey fk = map.get(name);
      if (fk == null) {
        map.put(name, fk = new ForeignKey(name, row.get(1), table));
      }
      fk.addColumn(row.get(2), row.get(3));
    }
    LOGGER.trace("loadForeignKeys - {}", map.values());
    return map.values();
  }

  String getLoadForeignKeysSql(String table, String database) {
    return "select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME "
        + "from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where TABLE_SCHEMA = '" + database
        + "' and REFERENCED_TABLE_NAME = '" + table
        + "' order by CONSTRAINT_NAME, ORDINAL_POSITION";
  }

  private String getDatabaseWithPrefix() {
    String prefix = context.getXWikiContext().getWiki().Param("xwiki.db.prefix", "");
    return prefix + context.getWikiRef().getName();
  }

  class ForeignKey {

    private final String name;
    private final String table;
    private final String referencedTable;
    private final List<String> columns = new ArrayList<>();
    private final List<String> referencedColumns = new ArrayList<>();

    ForeignKey(String name, String table, String referencedTable) {
      this.name = validate(name);
      this.table = validate(table);
      this.referencedTable = validate(referencedTable);
    }

    public ForeignKey addColumn(String column, String referencedColumn) {
      columns.add(validate(column));
      referencedColumns.add(validate(referencedColumn));
      return this;
    }

    public String getAddForeignKeySql() {
      return "alter table " + table + " add constraint " + name + " foreign key (" + Joiner.on(
          ',').join(columns) + ")" + " references " + referencedTable + " (" + Joiner.on(',').join(
              referencedColumns) + ")";
    }

    public String getDropForeignKeySql() {
      return "alter table " + table + " drop foreign key " + name;
    }

    private String validate(String str) {
      return checkNotNull(Strings.emptyToNull(str));
    }

    @Override
    public int hashCode() {
      return name.hashCode();
    }

    @Override
    public String toString() {
      return "ForeignKey [name=" + name + ", table=" + table + ", referencedTable="
          + referencedTable + ", columns=" + columns + ", referencedColumns=" + referencedColumns
          + "]";
    }

  }

}
