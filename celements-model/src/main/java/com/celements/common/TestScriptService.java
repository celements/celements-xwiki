package com.celements.common;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.celements.model.context.ModelContext;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component("celtest")
public class TestScriptService implements ScriptService {

  @Requirement
  private ModelContext modelContext;

  @Requirement
  private IRightsAccessFacadeRole rightsAccess;

  public void readFromTemplate(XWikiDocument doc, DocumentReference templateDocRef)
      throws XWikiException {
    doc.readFromTemplate(templateDocRef, modelContext.getXWikiContext());
  }

}
