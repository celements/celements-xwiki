package com.celements.model.object.xwiki;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.xwiki.model.reference.ImmutableDocumentReference;

import com.celements.model.classes.ClassIdentity;
import com.celements.model.object.AbstractObjectFetcher;
import com.celements.model.object.ObjectBridge;
import com.celements.model.object.ObjectHandler;
import com.google.common.collect.FluentIterable;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

@NotThreadSafe
public class XWikiObjectFetcher extends
    AbstractObjectFetcher<XWikiObjectFetcher, XWikiDocument, BaseObject> {

  public static XWikiObjectFetcher on(@NotNull XWikiDocument doc) {
    return new XWikiObjectFetcher(doc);
  }

  public static XWikiObjectFetcher from(
      @NotNull ObjectHandler<XWikiDocument, BaseObject> objHandler) {
    return XWikiObjectFetcher.on(objHandler.getDocument()).with(objHandler.getQuery());
  }

  public static XWikiObjectFetcher empty() {
    XWikiDocument dummyDoc = new XWikiDocument(new ImmutableDocumentReference("$", "$", "$"));
    return new XWikiObjectFetcher(dummyDoc, XWikiEmptyObjectBridge.INSTANCE);
  }

  private XWikiObjectBridge bridge;

  private XWikiObjectFetcher(XWikiDocument doc) {
    super(doc);
  }

  private XWikiObjectFetcher(XWikiDocument doc, XWikiObjectBridge bridge) {
    this(doc);
    this.bridge = bridge;
  }

  @Override
  public XWikiObjectFetcher clone() {
    return from(getThis());
  }

  @Override
  protected XWikiObjectFetcher disableCloning() {
    return super.disableCloning();
  }

  @Override
  protected XWikiObjectBridge getBridge() {
    if (bridge == null) {
      bridge = (XWikiObjectBridge) Utils.getComponent(ObjectBridge.class, XWikiObjectBridge.NAME);
    }
    return bridge;
  }

  @Override
  protected XWikiObjectFetcher getThis() {
    return this;
  }

  private static class XWikiEmptyObjectBridge extends XWikiObjectBridge {

    final static XWikiObjectBridge INSTANCE = new XWikiEmptyObjectBridge();

    @Override
    public FluentIterable<? extends ClassIdentity> getDocClasses(XWikiDocument doc) {
      return FluentIterable.of();
    }

    @Override
    public FluentIterable<BaseObject> getObjects(XWikiDocument doc, ClassIdentity classId) {
      return FluentIterable.of();
    }
  }

}
