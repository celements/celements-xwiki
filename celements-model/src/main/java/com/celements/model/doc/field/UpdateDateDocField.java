package com.celements.model.doc.field;

import java.util.Date;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;

@Component(UpdateDateDocField.NAME)
public class UpdateDateDocField extends AbstractDocField<Date> {

  public static final String NAME = "updateDate";

  protected UpdateDateDocField() {
    super(NAME, Date.class);
  }

  @Override
  protected Date getValueInternal(XWikiDocument doc) {
    return doc.getDate();
  }

  @Override
  protected void setValueInternal(XWikiDocument doc, Date value) {
    doc.setDate(value);
  }

};
