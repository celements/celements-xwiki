package com.celements.model.migration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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
import com.celements.model.migration.InformationSchema.TableSchemaData;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableList;
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
      "xwikinumberclasses", "xwikibooleanclasses", "xwikistringclasses", "xwikidateclasses",
      "xwikislistclasses", "xwikidblistclasses", "xwikiobjects", "xwikiproperties", "xwikiintegers",
      "xwikilongs", "xwikifloats", "xwikidoubles", "xwikistrings", "xwikidates",
      "xwikilargestrings", "xwikilists", "xwikilistitems", "xwikistatsdoc", "xwikistatsreferer",
      "xwikistatsvisit");

  @Requirement
  private HibernateSessionFactory sessionFactory;

  @Requirement
  private IQueryExecutionServiceRole queryExecutor;

  @Requirement
  private ModelContext context;

  private InformationSchema informationSchema;

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
      informationSchema = new InformationSchema(getDatabaseWithPrefix());
      migrateXWikiTables();
      migrateMappedTables();
    } catch (Exception exc) {
      LOGGER.error("Failed to migrate id columns for database [{}]", getDatabaseWithPrefix(), exc);
      throw exc;
    } finally {
      informationSchema = null;
    }
  }

  private void migrateXWikiTables() throws XWikiException {
    for (String table : XWIKI_TABLES) {
      LOGGER.debug("[{}] try id column migration for xwiki table", table);
      migrateTable(table);
    }
  }

  @SuppressWarnings("unchecked")
  private void migrateMappedTables() throws XWikiException {
    for (Iterator<PersistentClass> iter = getHibConfig().getClassMappings(); iter.hasNext();) {
      PersistentClass mapping = iter.next();
      String table = mapping.getTable().getName();
      LOGGER.debug("[{}] try id column migration for mapped table", table);
      if (isMappingWithLongPrimaryKey(mapping)) {
        migrateTable(table);
      } else {
        LOGGER.debug("[{}] skip table, id isn't mapped as long", table);
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
    if (validateSchemaDataForTable(table)) {
      Collection<ForeignKey> droppedForeignKeys = new HashSet<>();
      try {
        dropReferencingForeignKeys(table, droppedForeignKeys);
        for (ForeignKey fk : droppedForeignKeys) {
          migrateTable(fk.getTable());
        }
        String column = informationSchema.get(table).getPkColumnName();
        int count = queryExecutor.executeWriteSQL(getModifyIdColumnSql(table, column));
        LOGGER.info("[{}] updated id column for {} rows", table, count);
      } finally {
        addForeignKeys(table, droppedForeignKeys);
      }
    }
  }

  private boolean validateSchemaDataForTable(String table) {
    boolean validated = false;
    try {
      TableSchemaData data = informationSchema.get(table);
      if ("int".equals(data.getPkDataType())) {
        validated = true;
      } else {
        LOGGER.debug("[{}] skip table, id column type is [{}]", table, data.getPkDataType());
      }
    } catch (IllegalArgumentException iae) {
      LOGGER.warn("[{}] skip table, no TableSchemaData", table, iae);
    }
    return validated;
  }

  static String getModifyIdColumnSql(String table, String column) {
    return "alter table " + table + " modify column " + column + " bigint not null";
  }

  private void addForeignKeys(String table, Collection<ForeignKey> foreignKeys) {
    if (foreignKeys.size() > 0) {
      LOGGER.debug("[{}] add {} FKs", table, foreignKeys.size());
    }
    for (ForeignKey fk : foreignKeys) {
      try {
        LOGGER.trace("[{}] adding {}", table, fk);
        queryExecutor.executeWriteSQL(fk.getAddSql());
      } catch (XWikiException xwe) {
        LOGGER.error("[{}] failed to add {}", table, fk, xwe);
      }
    }
  }

  private void dropReferencingForeignKeys(String table, Collection<ForeignKey> droppedForeignKeys)
      throws XWikiException {
    for (ForeignKey fk : informationSchema.get(table).getForeignKeys()) {
      LOGGER.debug("[{}] dropping {}", table, fk);
      queryExecutor.executeWriteSQL(fk.getDropSql());
      droppedForeignKeys.add(fk);
    }
    if (droppedForeignKeys.size() > 0) {
      LOGGER.info("[{}] dropped {} FKs", table, droppedForeignKeys.size());
    }
  }

  String getDatabaseWithPrefix() {
    return context.getXWikiContext().getWiki().Param("xwiki.db.prefix", "")
        + context.getWikiRef().getName();
  }

  private Configuration getHibConfig() {
    return sessionFactory.getConfiguration();
  }

}
