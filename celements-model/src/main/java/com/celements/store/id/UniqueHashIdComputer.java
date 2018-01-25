package com.celements.store.id;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Verify.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.VerifyException;
import com.google.common.primitives.Longs;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component(UniqueHashIdComputer.NAME)
public class UniqueHashIdComputer implements CelementsIdComputer {

  public static final String NAME = "uniqueHash";

  private static final String HASH_ALGO = "MD5";
  private static final byte BITS_COLLISION_COUNT = 2;
  private static final byte BITS_OBJECT_COUNT = 12;

  @Requirement
  private ModelUtils modelUtils;

  @Override
  public IdVersion getIdVersion() {
    return IdVersion.CELEMENTS_3;
  }

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang)
      throws IdComputationException {
    byte collisionCount = 0;
    return computeDocumentId(docRef, lang, collisionCount);
  }

  @Override
  public long computeDocumentId(DocumentReference docRef, String lang, byte collisionCount)
      throws IdComputationException {
    return computeId(docRef, lang, collisionCount, 0);
  }

  @Override
  public long computeDocumentId(XWikiDocument doc) throws IdComputationException {
    return computeId(doc, 0);
  }

  @Override
  public long computeNextObjectId(XWikiDocument doc) throws IdComputationException {
    Set<Long> existingObjIds = new HashSet<>();
    for (BaseObject obj : XWikiObjectEditor.on(doc).fetch().iter()) {
      if (obj.hasValidId() && (obj.getIdVersion() == getIdVersion())) {
        existingObjIds.add(obj.getId());
      }
    }
    long nextObjectId;
    int objectCount = 1;
    do {
      nextObjectId = computeId(doc, objectCount++);
    } while (existingObjIds.contains(nextObjectId));
    return nextObjectId;
  }

  private long computeId(XWikiDocument doc, int objectCount) throws IdComputationException {
    checkNotNull(doc);
    byte collisionCount = 0;
    return computeId(doc.getDocumentReference(), doc.getLanguage(), collisionCount, objectCount);
  }

  long computeId(DocumentReference docRef, String lang, byte collisionCount, int objectCount)
      throws IdComputationException {
    verifyCount(collisionCount, BITS_COLLISION_COUNT);
    verifyCount(objectCount, BITS_OBJECT_COUNT);
    long docId = hashMD5(serializeLocalUid(docRef, lang));
    long center = andifyLeft(andifyRight(docId, BITS_OBJECT_COUNT), BITS_COLLISION_COUNT);
    byte bitsRight = 64 - BITS_COLLISION_COUNT;
    long left = andifyRight(((long) collisionCount) << bitsRight, bitsRight);
    byte bitsLeft = 64 - BITS_OBJECT_COUNT;
    long right = andifyLeft(objectCount, bitsLeft);
    return left & center & right;
  }

  long andifyLeft(long base, byte bits) {
    return ~(~(base << bits) >>> bits);
  }

  long andifyRight(long base, byte bits) {
    return ~(~(base >>> bits) << bits);
  }

  private void verifyCount(long count, byte bits) throws IdComputationException {
    try {
      verify(count >= 0, "negative count '%s' not allowed", count);
      verify((count >>> bits) == 0, "count '%s' outside of defined range '2^%s'", count, bits);
    } catch (VerifyException exc) {
      throw new IdComputationException(exc);
    }
  }

  /**
   * @return first 8 bytes of MD5 hash from given string
   */
  long hashMD5(String str) throws IdComputationException {
    try {
      MessageDigest digest = MessageDigest.getInstance(HASH_ALGO);
      digest.update(str.getBytes(StandardCharsets.UTF_8));
      return Longs.fromByteArray(digest.digest());
    } catch (NoSuchAlgorithmException exc) {
      throw new IdComputationException("failed calculating hash", exc);
    }
  }

  /**
   * @return calculated local uid like LocalUidStringEntityReferenceSerializer from XWiki 4.0+
   */
  String serializeLocalUid(DocumentReference docRef, String lang) {
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
