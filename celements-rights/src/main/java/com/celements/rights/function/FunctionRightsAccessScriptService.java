package com.celements.rights.function;

import static com.celements.common.lambda.LambdaExceptionUtil.*;

import java.util.List;
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

  public Set<DocumentReference> getGroupsWithAccess(String... functionNames) {
    return functionRightsAccess.getGroupsWithAccess(functionNames);
  }

  public Set<DocumentReference> getGroupsWithAccess(List<String> functionNames) {
    return getGroupsWithAccess(functionNames.toArray(new String[0]));
  }

  public boolean hasGroupAccess(DocumentReference groupDocRef, String... functionNames) {
    return functionRightsAccess.hasGroupAccess(groupDocRef, functionNames);
  }

  public boolean hasGroupAccess(DocumentReference groupDocRef, List<String> functionNames) {
    return hasGroupAccess(groupDocRef, functionNames.toArray(new String[0]));
  }

  public boolean hasUserAccess(DocumentReference userDocRef, String... functionNames) {
    try {
      return Optional.ofNullable(userDocRef)
          .map(rethrowFunction(userService::getUser))
          .filter(user -> functionRightsAccess.hasUserAccess(user, functionNames))
          .isPresent();
    } catch (UserInstantiationException exc) {
      return false;
    }
  }

  public boolean hasUserAccess(DocumentReference userDocRef, List<String> functionNames) {
    return hasUserAccess(userDocRef, functionNames.toArray(new String[0]));
  }

  public boolean hasCurrentUserAccess(String... functionNames) {
    return functionRightsAccess.hasCurrentUserAccess(functionNames);
  }

  public boolean hasCurrentUserAccess(List<String> functionNames) {
    return hasCurrentUserAccess(functionNames.toArray(new String[0]));
  }

}
