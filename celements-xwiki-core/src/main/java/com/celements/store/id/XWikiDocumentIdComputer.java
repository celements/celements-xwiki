package com.celements.store.id;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

@Component("xwiki")
public class XWikiDocumentIdComputer implements DocumentIdComputer {

  @Requirement("local")
  private EntityReferenceSerializer<String> localEntityReferenceSerializer;

  @Override
  public long compute(@NotNull DocumentReference docRef, String lang) {
    String uniqueName = localEntityReferenceSerializer.serialize(docRef);
    if ((lang != null) && !lang.trim().isEmpty()) {
      uniqueName += ":" + lang;
    }
    return uniqueName.hashCode();
  }

}
