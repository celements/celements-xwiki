package com.celements.model.migration;

import static com.google.common.base.Preconditions.*;
import static java.text.MessageFormat.format;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.concurrent.Immutable;

import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.Utils;

/**
 * holds schema information for the given database
 */
@Immutable
class InformationSchema {

  private final String database;
  private final Map<String, TableSchemaData> map;

  InformationSchema(String database) throws XWikiException {
    this(database, true);
  }

  InformationSchema(String database, boolean loadForeignKeys) throws XWikiException {
    this.database = database;
    map = load();
    if (loadForeignKeys) {
      loadForeignKeys();
    }
  }

  public String getDatabase() {
    return database;
  }

  public TableSchemaData get(String table) throws IllegalArgumentException {
    checkArgument(map.containsKey(table), "[" + table + "] has no TableSchemaData");
    return map.get(table);
  }

  private Map<String, TableSchemaData> load() throws XWikiException {
    Builder<String, TableSchemaData> builder = ImmutableMap.builder();
    String sql = getLoadColumnsSql(database);
    List<List<String>> result = executeReadSql(sql);
    if (result.isEmpty()) {
      throw new XWikiException(0, 0, format("empty result for: %s", sql));
    }
    for (List<String> row : result) {
      if (row.size() != 3) {
        throw new XWikiException(0, 0, format("illegal length on row [%s] for sql: %s", row, sql));
      }
      String table = row.get(0);
      builder.put(table, new TableSchemaData(table, row.get(1), row.get(2)));
    }
    return builder.build();

  }

  static String getLoadForeignKeysSql(String database) {
    return "select CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, "
        + "REFERENCED_COLUMN_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where TABLE_SCHEMA = '"
        + database + "' and REFERENCED_TABLE_NAME is not null "
        + "order by REFERENCED_TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION";
  }

  private void loadForeignKeys() throws XWikiException {
    for (List<String> row : executeReadSql(getLoadForeignKeysSql(database))) {
      String referencedTable = row.get(3);
      if (map.containsKey(referencedTable)) {
        Map<String, ForeignKey> foreignKeys = map.get(referencedTable).foreignKeys;
        String name = row.get(0);
        ForeignKey fk = foreignKeys.get(name);
        if (fk == null) {
          foreignKeys.put(name, fk = new ForeignKey(name, row.get(1), referencedTable));
        }
        fk.addColumn(row.get(2), row.get(4));
      }
    }
  }

  static String getLoadColumnsSql(String database) {
    String select = "select k.TABLE_NAME, k.COLUMN_NAME, c.DATA_TYPE";
    String from = " from INFORMATION_SCHEMA.KEY_COLUMN_USAGE as k, INFORMATION_SCHEMA.COLUMNS as c";
    String where = " where k.TABLE_SCHEMA = '" + database + "' and k.CONSTRAINT_NAME = 'PRIMARY'"
        + " and k.COLUMN_NAME like '%_ID'";
    String whereJoin = " and k.TABLE_SCHEMA = c.TABLE_SCHEMA and k.TABLE_NAME = c.TABLE_NAME"
        + " and k.COLUMN_NAME = c.COLUMN_NAME";
    String groupBy = " group by k.TABLE_NAME";
    return select + from + where + whereJoin + groupBy;
  }

  private List<List<String>> executeReadSql(String sql) throws XWikiException {
    return Utils.getComponent(IQueryExecutionServiceRole.class).executeReadSql(String.class, sql);
  }

  static class TableSchemaData {

    private final String tableName;
    private final String pkColumnName;
    private final String pkDataType;
    private final Map<String, ForeignKey> foreignKeys;

    TableSchemaData(String tableName, String pkColumnName, String pkDataType) {
      this.tableName = tableName;
      this.pkColumnName = pkColumnName;
      this.pkDataType = pkDataType;
      this.foreignKeys = new HashMap<>();
    }

    public String getTableName() {
      return tableName;
    }

    public String getPkColumnName() {
      return pkColumnName;
    }

    public String getPkDataType() {
      return pkDataType;
    }

    public Set<ForeignKey> getForeignKeys() {
      return ImmutableSet.copyOf(foreignKeys.values());
    }

  }

}
