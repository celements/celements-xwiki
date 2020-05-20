package com.celements.model.access.lock;

import static com.celements.model.util.ReferenceSerializationMode.*;
import static com.celements.model.util.References.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;
import static java.text.MessageFormat.*;
import static org.glassfish.jersey.internal.guava.Predicates.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ImmutableDocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.ModelAccessRuntimeException;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.model.util.References;
import com.celements.query.IQueryExecutionServiceRole;
import com.google.common.base.Splitter;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiLock;

public class ModelLockService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelLockService.class);

  @Requirement
  private IQueryExecutionServiceRole queryExecService;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  public boolean isLocked(DocumentReference docRef, Predicate<LockState> predicate) {
    return getLockState(docRef).filter(predicate).isPresent();
  }

  public boolean isLockedByAny(DocumentReference docRef) {
    return isLocked(docRef, x -> true);
  }

  public boolean isLockedByMeForAny(DocumentReference docRef) {
    return isLocked(docRef, state -> LockId.forCurrentThread("").equalsIgnoreName(state.lockId));
  }

  public boolean isLockedByMeFor(DocumentReference docRef, String name) {
    return isLocked(docRef, state -> LockId.forCurrentThread(name).equals(state.lockId));
  }

  private static final String HQL_INSERT = "insert into XWikiLock(docId, userName, date) "
      + "select doc.id, '{0}', current_timestamp from XWikiDocument doc "
      + "where doc.fullName = :fullName and doc.language = ''";

  public boolean tryLock(DocumentReference docRef, String name) {
    invalidateCache(docRef); // force lock reload on every tryLock
    boolean hasLock = isLockedByMeFor(docRef, name);
    if (!hasLock) {
      checkState(!isLockedByMeForAny(docRef),
          "lock on this document is already held by this thread");
      if (!isLockedByAny(docRef)) {
        String hql = format(HQL_INSERT, LockId.forCurrentThread(name).serialize());
        hasLock = (execute(hql, docRef, null) > 0);
      }
    }
    return hasLock;
  }

  private static final String HQL_DELETE = "delete XWikiLock lock where ";
  private static final String WHERE_DOC_ID = " lock.docId in (select doc.id from XWikiDocument doc "
      + "where doc.fullName = :fullName and doc.language = ''))";

  public boolean unlockByMeFor(DocumentReference docRef, String name) {
    String hql = HQL_DELETE + "lock.userName = :lockId and " + WHERE_DOC_ID;
    return isLockedByMeFor(docRef, name) && (execute(hql, docRef, name) > 0);
  }

  public int unlockAllByMe(WikiReference wikiRef) {
    String hql = HQL_DELETE + "lock.userName like :lockId";
    return execute(hql, wikiRef, "%");
  }

  public boolean unlockForce(DocumentReference docRef) {
    String hql = HQL_DELETE + WHERE_DOC_ID;
    return (execute(hql, docRef, null) > 0);
  }

  private int execute(String hql, EntityReference ref, String name) {
    try {
      ImmutableMap.Builder<String, Object> binds = new ImmutableMap.Builder<>();
      References.extractRef(ref, EntityType.DOCUMENT).toJavaUtil()
          .ifPresent(x -> binds.put("fullName", modelUtils.serializeRef(ref, LOCAL)));
      Optional.ofNullable(name).map(LockId::forCurrentThread)
          .ifPresent(x -> binds.put("lockId", x.serialize()));
      return queryExecService.executeWriteHQL(hql, binds.build(),
          References.extractRef(ref, WikiReference.class).orNull());
    } catch (XWikiException exc) {
      LOGGER.debug("execute: unable to execute hql for [{}] with name [{}]: {}",
          ref, name, hql, exc);
      return 0;
    }
  }

  public Duration getCacheDuration() {
    return Duration.ofSeconds(1); // TODO from config
  }

  public Optional<LockState> getLockState(DocumentReference docRef) {
    try {
      return stateCache.get().get(cloneRef(docRef, ImmutableDocumentReference.class));
    } catch (ExecutionException exc) {
      throw new ModelAccessRuntimeException(docRef, exc);
    }
  }

  public void invalidateCache(DocumentReference docRef) {
    stateCache.get().invalidate(docRef);
  }

  /**
   * loads the current lock and caches it for {@link #getCacheDuration()}.
   */
  private final Supplier<LoadingCache<ImmutableDocumentReference, Optional<LockState>>> stateCache = Suppliers
      .memoize(() -> CacheBuilder.newBuilder()
          .expireAfterWrite(getCacheDuration())
          .maximumSize(1000)
          .build(new CacheLoader<ImmutableDocumentReference, Optional<LockState>>() {

            @Override
            public Optional<LockState> load(ImmutableDocumentReference docRef) throws Exception {
              return Optional.ofNullable(modelAccess.getOrCreateDocument(docRef)
                  .getLock(context.getXWikiContext()))
                  .map(xlock -> new LockState(docRef, xlock));
            }
          }));

  /**
   * Holds all relevant information to describe a lock. Comparable to {@link XWikiLock}.
   */
  @Immutable
  public class LockState {

    /**
     * the document being locked on (the actual mutex)
     */
    public final ImmutableDocumentReference docRef;

    /**
     * the locker / "author"
     */
    public final LockId lockId;

    /**
     * instant when the lock was aquired
     */
    public final Instant instant;

    LockState(DocumentReference docRef, XWikiLock xlock) {
      this.docRef = cloneRef(docRef, ImmutableDocumentReference.class);
      lockId = LockId.resolve(xlock.getUserName());
      this.instant = xlock.getDate().toInstant();
    }

  }

  /**
   * identifies a locker / "author" globally. consists of
   * {@link #clusterId}, {@link #threadId} and {@link #name}.
   */
  @Immutable
  public static class LockId {

    private static final Splitter SPLITTER = Splitter.on('|').omitEmptyStrings();

    private static final Supplier<String> CLUSTER_ID = Suppliers.memoize(() -> {
      try {
        return InetAddress.getLocalHost().getHostName();
      } catch (UnknownHostException exc) {
        throw new IllegalStateException(exc);
      }
    });

    /**
     * host name of the server
     */
    public final String clusterId;

    /**
     * id number of the thread
     */
    public final String threadId;

    /**
     * the name of the logic part locking for, e.g. "docForm"
     */
    public final String name;

    private LockId(String clusterId, String threadId, String name) {
      this.clusterId = nullToEmpty(clusterId);
      this.threadId = nullToEmpty(threadId);
      this.name = nullToEmpty(name);
    }

    public String serialize() {
      return Stream.of(clusterId, threadId, name)
          .filter(not(String::isEmpty))
          .collect(Collectors.joining("|"));
    }

    static LockId resolve(String str) {
      ListIterator<String> parts = SPLITTER.splitToList(str).listIterator();
      String name = parts.hasPrevious() ? parts.previous() : "";
      String threadId = parts.hasPrevious() ? parts.previous() : "";
      String clusterId = parts.hasPrevious() ? parts.previous() : "";
      return new LockId(clusterId, threadId, name);
    }

    static LockId forCurrentThread(String name) {
      String clusterId = CLUSTER_ID.get();
      String threadId = Objects.toString(Thread.currentThread().getId());
      return new LockId(clusterId, threadId, name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(clusterId, threadId, name);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof LockId) {
        LockId other = (LockId) obj;
        return equalsIgnoreName(other)
            && Objects.equals(name, other.name);
      }
      return false;
    }

    public boolean equalsIgnoreName(LockId other) {
      return Objects.equals(clusterId, other.clusterId)
          && Objects.equals(threadId, other.threadId);
    }
  }

}
