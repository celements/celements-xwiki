package com.celements.rights.function;

import static com.celements.common.lambda.LambdaExceptionUtil.*;

import java.util.Optional;
import java.util.Set;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;

import com.celements.auth.user.UserInstantiationException;
import com.celements.auth.user.UserService;

@Component(FunctionRightsAccessScriptService.NAME)
public class FunctionRightsAccessScriptService implements ScriptService {

  public static final String NAME = "funcRightsAccess";

  @Requirement
  private FunctionRightsAccess functionRightsAccess;

  @Requirement
  private UserService userService;

  public Set<DocumentReference> getGroupsWithAccess(String functionName) {
    return functionRightsAccess.getGroupsWithAccess(functionName);
  }

  public boolean hasGroupAccess(DocumentReference groupDocRef, String functionName) {
    return functionRightsAccess.hasGroupAccess(groupDocRef, functionName);
  }

  public boolean hasUserAccess(DocumentReference userDocRef, String functionName) {
    try {
      return Optional.ofNullable(userDocRef)
          .map(rethrowFunction(userService::getUser))
          .filter(user -> functionRightsAccess.hasUserAccess(user, functionName))
          .isPresent();
    } catch (UserInstantiationException exc) {
      return false;
    }
  }

  public boolean hasCurrentUserAccess(String functionName) {
    return functionRightsAccess.hasCurrentUserAccess(functionName);
  }

}
