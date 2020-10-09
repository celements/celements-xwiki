package com.celements.rights.function;

import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;

@ComponentRole
public interface FunctionRightsAccess {

  String SPACE_NAME = "FunctionRights";

  @NotNull
  Set<DocumentReference> getGroupsWithAccess(@NotEmpty String functionName);

  boolean hasGroupAccess(@Nullable DocumentReference groupDocRef, @NotEmpty String functionName);

  boolean hasUserAccess(@Nullable User user, @NotEmpty String functionName);

  boolean hasCurrentUserAccess(@NotEmpty String functionName);

}
