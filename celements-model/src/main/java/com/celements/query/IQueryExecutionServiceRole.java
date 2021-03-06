package com.celements.query;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWikiException;

@ComponentRole
public interface IQueryExecutionServiceRole {

  @NotNull
  public List<List<String>> executeReadSql(@NotNull String sql) throws XWikiException;

  @NotNull
  public <T> List<List<T>> executeReadSql(@NotNull Class<T> type, @NotNull String sql)
      throws XWikiException;

  public int executeWriteSQL(String sql) throws XWikiException;

  public List<Integer> executeWriteSQLs(List<String> sqls) throws XWikiException;

  public int executeWriteHQL(String hql, Map<String, Object> binds) throws XWikiException;

  public int executeWriteHQL(String hql, Map<String, Object> binds, WikiReference wikiRef)
      throws XWikiException;

  public DocumentReference executeAndGetDocRef(Query query) throws QueryException;

  public List<DocumentReference> executeAndGetDocRefs(Query query) throws QueryException;

  public boolean existsIndex(@NotNull WikiReference wikiRef, @NotNull String table,
      @NotNull String name) throws XWikiException;

}
