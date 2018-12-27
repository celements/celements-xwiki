package com.celements.model.migration;

import static com.google.common.base.Preconditions.*;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component(InvalidXObjectsMigration.NAME)
public class InvalidXObjectsMigration extends AbstractCelementsHibernateMigrator {

  static final Logger LOGGER = LoggerFactory.getLogger(InvalidXObjectsMigration.class);

  public static final String NAME = "InvalidXObjectsMigration";

  @Requirement
  private IQueryExecutionServiceRole queryExecutor;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return "deletes and logs invalid base objects, e.g. with missing xclass";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this migration
   * http://www.wolframalpha.com/input/?i=days+since+01.01.2010
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(3278);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    String database = context.getDatabase();
    LOGGER.info("[{}] deleting invalid xobjects", database);
    try {
      List<List<String>> result = queryExecutor.executeReadSql(getSelectSql());
      if (result.size() > 0) {
        logData(database, result);
        int count = queryExecutor.executeWriteSQL(getDeleteSql());
        LOGGER.info("[{}] deleted {} invalid xobjects", database, count);
      } else {
        LOGGER.info("[{}] no invalid xobjects to delete", database);
      }
    } catch (Exception exc) {
      LOGGER.error("[{}] failed to delete invalid xobjects", database, exc);
      throw exc;
    }
  }

  private void logData(String database, List<List<String>> result) {
    checkState(LOGGER.isWarnEnabled(), "logging on level 'WARN' disabled");
    LOGGER.warn("[{}] {} invalid xobjects:", database, result.size());
    for (List<String> row : result) {
      LOGGER.warn("{}", row.toString());
    }
  }

  static String getSelectSql() {
    return "select XWO_ID, XWO_NAME, XWO_CLASSNAME, XWO_NUMBER from xwikiobjects"
        + getJoinAndWhereSql();
  }

  static String getDeleteSql() {
    return "delete xwikiobjects from xwikiobjects" + getJoinAndWhereSql();
  }

  private static String getJoinAndWhereSql() {
    return " where XWO_NAME = '' or XWO_CLASSNAME = '' or XWO_NUMBER is null or XWO_NUMBER < 0";
  }

}
