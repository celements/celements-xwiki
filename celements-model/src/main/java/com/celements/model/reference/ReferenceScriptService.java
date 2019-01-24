package com.celements.model.reference;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.script.service.ScriptService;

import com.celements.model.context.ModelContext;

@Component(ReferenceScriptService.NAME)
public class ReferenceScriptService implements ScriptService {

  public static final String NAME = "reference";

  @Requirement
  private ModelContext context;

  public RefBuilder create() {
    return new RefBuilder().nullable().with(context.getWikiRef());
  }

}
