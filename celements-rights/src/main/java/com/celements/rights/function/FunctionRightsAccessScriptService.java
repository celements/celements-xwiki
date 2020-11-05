package com.celements.rights.function;

import static com.celements.common.lambda.LambdaExceptionUtil.*;

import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.celements.auth.user.UserInstantiationException;
import com.celements.auth.user.UserService;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;

@Component(FunctionRightsAccessScriptService.NAME)
public class FunctionRightsAccessScriptService implements ScriptService {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(FunctionRightsAccessScriptService.class);

  public static final String NAME = "funcRightsAccess";

  @Requirement
  private FunctionRightsAccess functionRightsAccess;

  @Requirement
  private UserService userService;

  public Set<DocumentReference> getGroupsWithAccess(String functionName) {
    if (!Strings.isNullOrEmpty(functionName)) {
      return functionRightsAccess.getGroupsWithAccess(functionName);
    } else {
      return ImmutableSet.of();
    }
  }

  public boolean hasGroupAccess(DocumentReference groupDocRef, String functionName) {
    if (!Strings.isNullOrEmpty(functionName)) {
      return functionRightsAccess.hasGroupAccess(groupDocRef, functionName);
    } else {
      return false;
    }
  }

  public boolean hasUserAccess(DocumentReference userDocRef, String functionName) {
    if (!Strings.isNullOrEmpty(functionName)) {
      try {
        return Optional.ofNullable(userDocRef)
            .map(rethrowFunction(userService::getUser))
            .filter(user -> functionRightsAccess.hasUserAccess(user, functionName))
            .isPresent();
      } catch (UserInstantiationException exc) {
        LOGGER.warn("hasUserAccess - failed for {}", userDocRef, exc);
      }
    }
    return false;
  }

  public boolean hasCurrentUserAccess(String functionName) {
    if (!Strings.isNullOrEmpty(functionName)) {
      return functionRightsAccess.hasCurrentUserAccess(functionName);
    } else {
      return false;
    }

  }

}
