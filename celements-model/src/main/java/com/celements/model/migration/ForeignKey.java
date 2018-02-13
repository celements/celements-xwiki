package com.celements.model.migration;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;

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

  public String getName() {
    return name;
  }

  public String getTable() {
    return table;
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

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ForeignKey) {
      return this.name.equals(((ForeignKey) obj).name);
    }
    return false;
  }

  @Override
  public String toString() {
    return "ForeignKey [name=" + name + ", table=" + table + ", referencedTable=" + referencedTable
        + ", columns=" + columns + ", referencedColumns=" + referencedColumns + "]";
  }

  private static String validate(String str) {
    return checkNotNull(Strings.emptyToNull(str));
  }

}
