package com.celements.store.part;

import java.util.Comparator;

import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiDummyDocComparator implements Comparator<XWikiDocument> {

  @Override
  public int compare(XWikiDocument doc1, XWikiDocument doc2) {
    int cmp = doc1.getDocumentReference().compareTo(doc2.getDocumentReference());
    if (cmp == 0) {
      cmp = doc1.getLanguage().compareTo(doc2.getLanguage());
    }
    return cmp;
  }

}
