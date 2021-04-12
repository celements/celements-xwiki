package com.celements.debug;

import static com.google.common.base.Predicates.*;

import java.lang.reflect.Executable;
import java.util.function.Function;
import java.util.stream.Stream;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;

import com.celements.common.lambda.LambdaExceptionUtil.ThrowingFunction;
import com.celements.common.lambda.LambdaExceptionUtil.ThrowingSupplier;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.store.DocumentCacheStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

/**
 * IMPORTANT: For debugging purposes only, never use in productive scripts!
 * Use with caution, it may be a security concern or cause performance issues.
 */
@Component("debug")
public class DebugScriptService implements ScriptService {

  @Requirement
  private IRightsAccessFacadeRole rightsAccess;

  @Requirement
  private ComponentManager componentManager;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement(DocumentCacheStore.COMPONENT_NAME)
  private XWikiStoreInterface docCacheStore;

  public Class<?> getClass(String className) {
    return guard(() -> Class.forName(className));
  }

  public Object getService(Class<?> role, String hint) {
    return guard(() -> componentManager.lookup(role, hint));
  }

  public Object newInstance(Class<?> type, Object... args) {
    return guard(() -> tryExecuteAny(Stream.of(type.getConstructors()),
        constructor -> constructor.newInstance(args)));
  }

  public Object callStaticMethod(Class<?> type, String methodName, Object... args) {
    return guard(() -> tryExecuteAny(Stream.of(type.getMethods())
        .filter(method -> method.getName().equals(methodName)),
        method -> method.invoke(null, args)));
  }

  private <E extends Executable> Object tryExecuteAny(Stream<E> executables,
      ThrowingFunction<E, ?, ReflectiveOperationException> invoker) throws Exception {
    Function<E, Object> evaluate = exec -> {
      try {
        return invoker.apply(exec);
      } catch (Exception exc) {
        return exc;
      }
    };
    return executables.map(evaluate)
        .filter(not(Exception.class::isInstance))
        .findFirst()
        .orElseThrow(() -> (Exception) executables.map(evaluate)
            .findAny().orElseGet(NoSuchMethodException::new));
  }

  public boolean removeDocFromCache(DocumentReference docRef) {
    return guard(() -> removeDocFromCacheInternal(docRef));
  }

  public long removeDocsInSpaceFromCache(SpaceReference spaceRef) {
    return guard(() -> modelUtils.getAllDocsForSpace(spaceRef)
        .map(this::removeDocFromCacheInternal)
        .count());
  }

  public long removeDocsInWikiFromCache(WikiReference wikiRef) {
    return guard(() -> modelUtils.getAllSpaces(wikiRef)
        .flatMap(spaceRef -> modelUtils.getAllDocsForSpace(spaceRef))
        .map(this::removeDocFromCacheInternal)
        .count());
  }

  private boolean removeDocFromCacheInternal(DocumentReference docRef) {
    return getDocCache().remove(modelAccess.getOrCreateDocument(docRef));
  }

  public boolean clearDocCache() {
    return guard(() -> {
      getDocCache().clearCache();
      return true;
    });
  }

  private DocumentCacheStore getDocCache() {
    return ((DocumentCacheStore) docCacheStore);
  }

  private <T, E extends Exception> T guard(ThrowingSupplier<T, E> toGuard) {
    if (rightsAccess.isSuperAdmin()) {
      try {
        return toGuard.get();
      } catch (Exception exc) {
        throw new IllegalArgumentException(exc);
      }
    }
    return null;
  }

}
