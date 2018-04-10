package com.celements.model.doc.field;

import java.util.Date;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;

@Component(CreationDateDocField.NAME)
public class CreationDateDocField extends AbstractDocField<Date> {

  public static final String NAME = "creationDate";

  protected CreationDateDocField() {
    super(NAME, Date.class);
  }

  @Override
  protected Date getValueInternal(XWikiDocument doc) {
    return doc.getCreationDate();
  }

  @Override
  protected void setValueInternal(XWikiDocument doc, Date value) {
    doc.setCreationDate(value);
  }

};
