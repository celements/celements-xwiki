package com.celements.model.access.lock;

import static com.google.common.base.Preconditions.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeoutException;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableDocumentReference;

import com.celements.model.util.References;
import com.google.common.base.Strings;

public class ModelLock {

  private final ImmutableDocumentReference docRef;
  private final String name;
  private final Duration retry;

  public ModelLock(DocumentReference docRef, String name, Duration retry) {
    this.docRef = References.cloneRef(docRef, ImmutableDocumentReference.class);
    this.name = checkNotNull(Strings.emptyToNull(name));
    this.retry = checkNotNull(retry);
  }

  public ImmutableDocumentReference getDocRef() {
    return docRef;
  }

  public String getName() {
    return name;
  }

  public boolean tryLock() {
    return getLockService().tryLock(getDocRef(), getName());
  }

  public void lock(Duration timeout) throws InterruptedException, TimeoutException {
    Instant timeToOut = Instant.now().plus(timeout);
    while (!tryLock()) {
      Instant now = Instant.now();
      Duration retryMax = Duration.between(now, timeToOut);
      if (!retryMax.isNegative()) {
        Thread.sleep((retry.compareTo(retryMax) < 0 ? retry : retryMax).toMillis());
      } else {
        throw new TimeoutException();
      }
    }
  }

  public void unlock() {
    getLockService().unlockByMeFor(getDocRef(), getName());
  }

  private ModelLockService getLockService() {
    return null; // TODO ModelLockService as component
  }

}
