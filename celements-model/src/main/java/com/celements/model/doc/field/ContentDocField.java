package com.celements.model.doc.field;

import static com.google.common.base.Strings.*;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;

@Component(ContentDocField.NAME)
public class ContentDocField extends AbstractDocField<String> {

  public static final String NAME = "title";

  protected ContentDocField() {
    super(NAME, String.class);
  }

  @Override
  protected String getValueInternal(XWikiDocument doc) {
    return emptyToNull(doc.getContent());
  }

  @Override
  protected void setValueInternal(XWikiDocument doc, String value) {
    doc.setContent(value);
  }

};
