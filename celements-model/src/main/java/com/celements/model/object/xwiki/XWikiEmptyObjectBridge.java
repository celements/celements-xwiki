package com.celements.model.object.xwiki;

import javax.annotation.concurrent.Immutable;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.ClassIdentity;
import com.google.common.collect.FluentIterable;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Immutable
@Component(XWikiEmptyObjectBridge.NAME)
public class XWikiEmptyObjectBridge extends XWikiObjectBridge {

  public static final String NAME = "xwikiempty";

  @Override
  public FluentIterable<? extends ClassIdentity> getDocClasses(XWikiDocument doc) {
    return FluentIterable.of();
  }

  @Override
  public FluentIterable<BaseObject> getObjects(XWikiDocument doc, ClassIdentity classId) {
    return FluentIterable.of();
  }

}
