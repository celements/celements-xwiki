package com.celements.mandatory;

import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;

import com.celements.model.context.ModelContext;
import com.celements.query.IQueryExecutionServiceRole;
import com.xpn.xwiki.XWikiException;

public abstract class AbstractMandatoryIndex implements IMandatoryDocumentRole {

  @Requirement
  private IQueryExecutionServiceRole queryExecService;

  @Requirement
  private ModelContext modelContext;

  @Override
  public List<String> dependsOnMandatoryDocuments() {
    return Collections.emptyList();
  }

  @Override
  public void checkDocuments() throws XWikiException {
    if (!queryExecService.existsIndex(modelContext.getWikiRef(), getTableName(), getIndexName())) {
      queryExecService.executeWriteSQL(getAddSql());
      getLogger().info("created index '{}'", getIndexName());
    } else {
      getLogger().debug("skipped index '{}', already created", getIndexName());
    }
  }

  protected abstract String getTableName();

  protected abstract String getIndexName();

  protected abstract String getAddSql();

  protected Logger getLogger() {
    return LoggerFactory.getLogger(this.getClass());
  }

}
