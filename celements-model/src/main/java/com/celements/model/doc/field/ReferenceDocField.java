package com.celements.model.doc.field;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiDocument;

@Component(ReferenceDocField.NAME)
public class ReferenceDocField extends AbstractDocField<DocumentReference> {

  public static final String NAME = "documentReference";

  protected ReferenceDocField() {
    super(NAME, DocumentReference.class);
  }

  @Override
  protected DocumentReference getValueInternal(XWikiDocument doc) {
    return doc.getDocumentReference();
  }

  @Override
  protected void setValueInternal(XWikiDocument doc, DocumentReference value) {
    throw new UnsupportedOperationException();
  }

};
