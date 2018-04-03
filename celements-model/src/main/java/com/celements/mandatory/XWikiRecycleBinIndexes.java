package com.celements.mandatory;

import org.xwiki.component.annotation.Component;

@Component(XWikiRecycleBinIndexes.NAME)
public class XWikiRecycleBinIndexes extends AbstractMandatoryIndex {

  public static final String NAME = "celements.mandatory.addIndexToRecycleBin";

  @Override
  protected String getTableName() {
    return "xwikirecyclebin";
  }

  @Override
  protected String getIndexName() {
    return "dateIDX";
  }

  @Override
  protected String getAddSql() {
    return "alter table " + getTableName() + " add index `" + getIndexName()
        + "` (XDD_DATE, XDD_ID)";
  }

}
