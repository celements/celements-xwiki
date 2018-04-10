package com.celements.model.doc.field;

import static com.google.common.base.Strings.*;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;

@Component(TitleDocField.NAME)
public class TitleDocField extends AbstractDocField<String> {

  public static final String NAME = "title";

  protected TitleDocField() {
    super(NAME, String.class);
  }

  @Override
  protected String getValueInternal(XWikiDocument doc) {
    return emptyToNull(doc.getTitle());
  }

  @Override
  protected void setValueInternal(XWikiDocument doc, String value) {
    doc.setTitle(value);
  }

};
