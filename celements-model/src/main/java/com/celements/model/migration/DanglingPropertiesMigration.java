package com.celements.model.migration;

import static com.google.common.base.Preconditions.*;

import java.util.Iterator;
import java.util.List;

import org.hibernate.cfg.Configuration;
import org.hibernate.mapping.PersistentClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.context.ModelContext;
import com.celements.model.migration.InformationSchema.TableSchemaData;
import com.celements.model.util.ModelUtils;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component(DanglingPropertiesMigration.NAME)
public class DanglingPropertiesMigration extends AbstractCelementsHibernateMigrator {

  static final Logger LOGGER = LoggerFactory.getLogger(DanglingPropertiesMigration.class);

  public static final String NAME = "DanglingPropertiesMigration";

  @Requirement
  private HibernateSessionFactory sessionFactory;

  @Requirement
  private IQueryExecutionServiceRole queryExecutor;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  private InformationSchema informationSchema;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "deletes dangling properties due to hash collisions from mapped tables";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this migration
   * 15.04.2018 -> 3025 http://www.wolframalpha.com/input/?i=days+since+01.01.2010
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(3026);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    LOGGER.info("deleting dangling properties for database [{}]", getDatabaseWithPrefix());
    try {
      for (Iterator<PersistentClass> iter = getHibConfig().getClassMappings(); iter.hasNext();) {
        PersistentClass mapping = iter.next();
        String table = mapping.getTable().getName();
        String className = mapping.getEntityName();
        if (validateTableAndLogData(table, className)) {
          migrateTable(table, className);
        }
      }
    } catch (Exception exc) {
      LOGGER.error("Failed to delete dangling properties for database [{}]",
          getDatabaseWithPrefix(), exc);
      throw exc;
    } finally {
      clearMigrationData();
    }
  }

  private boolean validateTableAndLogData(String table, String className) throws XWikiException {
    if (hasInternalCustomMapping(table, className)) {
      try {
        TableSchemaData data = getInformationSchema().get(table);
        String sql = getSelectSql(table, data.getPkColumnName(), className);
        LOGGER.trace("[{}] select sql: {}", table, sql);
        List<List<String>> result = queryExecutor.executeReadSql(sql);
        if (result.size() > 0) {
          logData(table, result);
          return true;
        } else {
          LOGGER.info("[{}] skip table, no dangling properties to delete", table);
        }
      } catch (IllegalArgumentException iae) {
        LOGGER.warn("[{}] skip table, no TableSchemaData", table, iae);
      }
    } else {
      LOGGER.debug("[{}] skip table, class [{}] has no internal custom mapping", table, className);
    }
    return false;
  }

  private void logData(String table, List<List<String>> result) {
    checkState(LOGGER.isWarnEnabled(), "logging on level 'WARN' disabled");
    LOGGER.warn("[{}] dangling properties:", table);
    for (List<String> row : result) {
      LOGGER.warn("[{}] {}", table, row.toString());
    }
  }

  private boolean hasInternalCustomMapping(String table, String className) {
    try {
      DocumentReference docRef = modelUtils.resolveRef(className, DocumentReference.class);
      return modelAccess.getDocument(docRef).getXClass().hasInternalCustomMapping();
    } catch (DocumentNotExistsException | IllegalArgumentException exc) {
      return false;
    }
  }

  private void migrateTable(String table, String className) throws XWikiException {
    TableSchemaData data = getInformationSchema().get(table);
    String sql = getDeleteSql(table, data.getPkColumnName(), className);
    LOGGER.trace("[{}] delete sql: {}", table, sql);
    int count = queryExecutor.executeWriteSQL(sql);
    LOGGER.info("[{}] deleted {} dangling properties", table, count);
  }

  static String getSelectSql(String table, String column, String className) {
    return "select XWO_ID, XWO_NAME, XWO_CLASSNAME, XWO_NUMBER, " + table + ".* from " + table
        + getJoinAndWhereSql(column, className) + " order by XWO_CLASSNAME, XWO_NAME";
  }

  static String getDeleteSql(String table, String column, String className) {
    return "delete " + table + " from " + table + getJoinAndWhereSql(column, className);
  }

  private static String getJoinAndWhereSql(String column, String className) {
    return " left join xwikiobjects on XWO_ID = " + column
        + " where XWO_ID is null or XWO_CLASSNAME <> '" + className + "'";
  }

  private void clearMigrationData() {
    informationSchema = null;
  }

  private InformationSchema getInformationSchema() throws XWikiException {
    if (informationSchema == null) {
      informationSchema = new InformationSchema(getDatabaseWithPrefix(), false);
    }
    return informationSchema;
  }

  String getDatabaseWithPrefix() {
    return modelUtils.getDatabaseName(context.getWikiRef());
  }

  private Configuration getHibConfig() {
    return sessionFactory.getConfiguration();
  }

}
