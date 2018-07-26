package com.celements.store;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;

import com.xpn.xwiki.store.XWikiStoreInterface;

@ComponentRole
public interface CelementsStore extends XWikiStoreInterface, MetaDataStoreExtension {

  @NotNull
  CelementsStore getBackingStore();

}
