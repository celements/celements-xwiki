package com.celements.model.doc;

import static com.google.common.base.Preconditions.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.util.ModelUtils;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Verify;
import com.google.common.base.VerifyException;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.doc.XWikiDocument;

@Component(UniqueHashIdComputer.NAME)
public class UniqueHashIdComputer implements CelementsIdComputer {

  public static final String NAME = "uniqueHash";

  private static final String HASH_ALGO = "MD5";

  @Requirement
  private ModelUtils modelUtils;

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang)
      throws IdComputationException {
    return computeDocumentId(docRef, lang, 0);
  }

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang, int collisionCount)
      throws IdComputationException {
    verifyCollisionCount(collisionCount);
    long docId = hashMD5(serializeLocalUid(docRef, lang)) << getObjectBits();
    return combine(docId, collisionCount, getCollisionBits());
  }

  @Override
  public long computeDocumentId(XWikiDocument doc) throws IdComputationException {
    checkNotNull(doc);
    return computeDocumentId(doc.getDocumentReference(), doc.getLanguage());
  }

  /**
   * bitwise combination of the long 'base' with 'bits' bits of the long 'msb' placed at the
   * left (most significant bits)
   */
  private long combine(long base, long msb, int bits) {
    base = ~(~(base << bits) >>> bits); // set msb's to 1 for and'ing
    msb = ~(~msb << (64 - bits)); // shift to left and set right side to 1 for and'ing
    return base & msb;
  }

  /**
   * @return first 8 bytes of MD5 hash from given string
   */
  private long hashMD5(String str) throws IdComputationException {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGO);
      digest.update(str.getBytes("utf-8"));
      return Longs.fromByteArray(digest.digest());
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException exc) {
      throw new IdComputationException("failed calculating hash", exc);
    }
  }

  private void verifyCollisionCount(int collisionCount) throws IdComputationException {
    try {
      Verify.verify(collisionCount >= 0, "negative collision count '%s' not allowed",
          collisionCount);
      Verify.verify(collisionCount < (1 << getCollisionBits()),
          "collision count '%s' outside of defined range '2^%s'", collisionCount,
          getCollisionBits());
    } catch (VerifyException exc) {
      throw new IdComputationException(exc);
    }
  }

  private int getCollisionBits() {
    return 2; // TODO configurable?
  }

  private int getObjectBits() {
    return 12; // TODO configurable?
  }

  /**
   * @return calculated local uid like LocalUidStringEntityReferenceSerializer from XWiki 4.0+
   */
  private String serializeLocalUid(DocumentReference docRef, String lang) {
    StringBuilder key = new StringBuilder();
    for (String name : Splitter.on('.').split(serialize(docRef, lang))) {
      if (!name.isEmpty()) {
        key.append(name.length()).append(':').append(name);
      }
    }
    return key.toString();
  }

  private String serialize(DocumentReference docRef, String lang) {
    return modelUtils.serializeRefLocal(docRef) + '.' + Strings.nullToEmpty(lang).trim();
  }

}
