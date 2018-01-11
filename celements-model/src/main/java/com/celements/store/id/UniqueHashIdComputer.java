package com.celements.store.id;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Verify.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.util.ModelUtils;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.VerifyException;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.doc.XWikiDocument;

@Component(UniqueHashIdComputer.NAME)
public class UniqueHashIdComputer implements CelementsIdComputer {

  public static final String NAME = "uniqueHash";

  private static final String HASH_ALGO = "MD5";
  private static final byte BITS_COLLISION_COUNT = 2;
  private static final byte BITS_OBJECT_COUNT = 12;

  @Requirement
  private ModelUtils modelUtils;

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang)
      throws IdComputationException {
    return computeDocumentId(docRef, lang, 0);
  }

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang, long collisionCount)
      throws IdComputationException {
    return computeId(docRef, lang, collisionCount, 0);
  }

  @Override
  public long computeDocumentId(XWikiDocument doc) throws IdComputationException {
    checkNotNull(doc);
    return computeDocumentId(doc.getDocumentReference(), doc.getLanguage());
  }

  private long computeId(DocumentReference docRef, String lang, long collisionCount,
      long objectCount) throws IdComputationException {
    long docId = hashMD5(serializeLocalUid(docRef, lang));
    verifyCount(collisionCount, BITS_COLLISION_COUNT);
    verifyCount(objectCount, BITS_OBJECT_COUNT);
    docId = andifyLeft(andifyRight(docId, BITS_OBJECT_COUNT), BITS_COLLISION_COUNT);
    byte bitsRight = 64 - BITS_COLLISION_COUNT;
    collisionCount = andifyRight(collisionCount << bitsRight, bitsRight);
    byte bitsLeft = 64 - BITS_OBJECT_COUNT;
    objectCount = andifyLeft(objectCount, bitsLeft);
    return collisionCount & docId & objectCount;
  }

  private long andifyLeft(long base, byte bits) {
    return ~(~(base << bits) >>> bits);
  }

  private long andifyRight(long base, byte bits) {
    return ~(~(base >> bits) << bits);
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

  private void verifyCount(long count, byte bits) throws IdComputationException {
    try {
      verify(count >= 0, "negative count '%s' not allowed", count);
      verify(count < (1L << bits), "count '%s' outside of defined range '2^%s'", count, bits);
    } catch (VerifyException exc) {
      throw new IdComputationException(exc);
    }
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
