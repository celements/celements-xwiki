package com.celements.model.doc;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Splitter;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component(UniqueHashIdComputer.NAME)
public class UniqueHashIdComputer implements CelementsIdComputer {

  public static final String NAME = "uniqueHash";

  private static final String HASH_ALGO = "MD5";
  private static final int BITS_COLLISION = 2;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  @Override
  public long computeId(DocumentReference docRef, String lang)
      throws DocumentIdComputationException {
    long id = hashMD5(getLocalKey(docRef, lang)) << (BITS_COLLISION + getOffsetBits());
    int collisionCount = 0;
    while (isIdClaimed(docRef, lang, id)) {
      if (collisionCount++ < (1 << BITS_COLLISION)) {
        id = incrementId(id);
      } else {
        throw new DocumentIdComputationException("too many id collisions occured on id '" + id
            + "' for: " + serialize(docRef, lang));
      }
    }
    return id;
  }

  /**
   * @return true if ID is already claimed by another document, hence a hash collision happened
   */
  private boolean isIdClaimed(DocumentReference docRef, String lang, long id)
      throws DocumentIdComputationException {
    try {
      XWikiDocument doc = new CelementsDocument(docRef);
      doc.setLanguage(lang);
      doc.setId(id);
      // use #loadXWikiDoc since #exists checks FN and not ID
      doc = context.getXWikiContext().getWiki().getStore().loadXWikiDoc(doc,
          context.getXWikiContext());
      boolean isSameDoc = docRef.equals(doc.getDocumentReference()) && lang.equals(
          doc.getLanguage());
      // TODO warn log if not same
      return !isSameDoc;
    } catch (XWikiException exc) {
      throw new DocumentIdComputationException("unable do load document: " + serialize(docRef,
          lang), exc);
    }
  }

  /**
   * @return calculated local key like LocalUidStringEntityReferenceSerializer from XWiki 4.0+
   */
  private String getLocalKey(DocumentReference docRef, String lang) {
    StringBuilder key = new StringBuilder();
    for (String name : Splitter.on('.').split(serialize(docRef, lang))) {
      if (!name.isEmpty()) {
        key.append(name.length()).append(':').append(name);
      }
    }
    return key.toString();
  }

  /**
   * @return first 8 bytes of MD5 hash from given string
   */
  private long hashMD5(String str) throws DocumentIdComputationException {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGO);
      digest.update(str.getBytes("utf-8"));
      return Longs.fromByteArray(digest.digest());
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException exc) {
      throw new DocumentIdComputationException("failed calculating hash", exc);
    }
  }

  private long incrementId(long id) {
    // increment at offset defined position
    return ((id >> getOffsetBits()) + 1) << getOffsetBits();
  }

  private int getOffsetBits() {
    return CelementsDocument.BITS_OBJECT_OFFSET;
  }

  private String serialize(DocumentReference docRef, String lang) {
    return modelUtils.serializeRefLocal(docRef) + '.' + lang;
  }

}
