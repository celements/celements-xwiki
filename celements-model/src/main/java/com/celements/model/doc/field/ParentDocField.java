package com.celements.model.doc.field;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.XWikiDocument;

@Component(ParentDocField.NAME)
public class ParentDocField extends AbstractDocField<DocumentReference> {

  public static final String NAME = "parent";

  protected ParentDocField() {
    super(NAME, DocumentReference.class);
  }

  @Override
  protected DocumentReference getValueInternal(XWikiDocument doc) {
    return doc.getParentReference();
  }

  @Override
  protected void setValueInternal(XWikiDocument doc, DocumentReference value) {
    doc.setParentReference((EntityReference) value);
  }

};
