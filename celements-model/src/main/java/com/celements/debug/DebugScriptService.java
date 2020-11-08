package com.celements.debug;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.celements.model.access.IModelAccessFacade;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.store.DocumentCacheStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

@Component("debug")
public class DebugScriptService implements ScriptService {

  @Requirement
  private IRightsAccessFacadeRole rightsAccess;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement(DocumentCacheStore.COMPONENT_NAME)
  private XWikiStoreInterface docCacheStore;

  public Object getService(String className, String hint) throws ClassNotFoundException {
    if (rightsAccess.isSuperAdmin()) {
      return Utils.getComponent(Class.forName(className), hint);
    }
    return null;
  }

  public String removeDocFromCache(DocumentReference docRef) {
    if (rightsAccess.isSuperAdmin()) {
      return ((DocumentCacheStore) docCacheStore).remove(modelAccess.getOrCreateDocument(docRef));
    }
    return "";
  }

}
