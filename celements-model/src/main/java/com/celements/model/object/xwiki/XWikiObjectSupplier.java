package com.celements.model.object.xwiki;

import org.xwiki.model.reference.ClassReference;

import com.google.common.base.Supplier;
import com.xpn.xwiki.objects.BaseObject;

public class XWikiObjectSupplier implements Supplier<BaseObject> {

  private final ClassReference classRef;

  public XWikiObjectSupplier(ClassReference classRef) {
    this.classRef = classRef;
  }

  @Override
  public BaseObject get() {
    BaseObject obj = new BaseObject();
    obj.setXClassReference(classRef);
    return obj;
  }

}
