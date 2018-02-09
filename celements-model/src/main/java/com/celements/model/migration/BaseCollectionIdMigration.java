package com.celements.model.migration;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component(BaseCollectionIdMigration.NAME)
public class BaseCollectionIdMigration extends AbstractCelementsHibernateMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseCollectionIdMigration.class);

  public static final String NAME = "BaseCollectionIdMigration";

  private static final Map<String, String> TABLE_COL_MAP;

  static {
    Builder<String, String> builder = ImmutableMap.builder();
    builder.put("xwikiclasses", "XWO_ID");
    builder.put("xwikiclassesprop", "XWP_ID");
    builder.put("xwikiobjects", "XWO_ID");
    builder.put("xwikiproperties", "XWP_ID");
    builder.put("xwikistatsdoc", "XWS_ID");
    builder.put("xwikistatsreferer", "XWR_ID");
    builder.put("xwikistatsvisit", "XWV_ID");
    TABLE_COL_MAP = builder.build();
  }

  @Requirement
  private IQueryExecutionServiceRole queryExecutor;

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
   * 08.02.2018 -> 2960 http://www.wolframalpha.com/input/?i=days+since+01.01.2010
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(2960);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    for (String table : TABLE_COL_MAP.keySet()) {
      String column = TABLE_COL_MAP.get(table);
      int resultCount = queryExecutor.executeWriteSQL(createAlterSql(table, column));
      LOGGER.info("altered table {} with {} entries to bigint", table, resultCount);
    }
  }

  String createAlterSql(String table, String column) {
    return "alter table " + table + " modify column " + column + " bigint not null";
  }

}
