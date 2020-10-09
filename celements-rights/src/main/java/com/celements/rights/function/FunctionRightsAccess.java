package com.celements.rights.function;

import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;

@ComponentRole
public interface FunctionRightsAccess {

  String SPACE_NAME = "FunctionRights";

  @NotNull
  Set<DocumentReference> getGroupsWithAccess(@Nullable String functionName);

  boolean hasGroupAccess(@Nullable DocumentReference groupDocRef, @Nullable String functionName);

  boolean hasUserAccess(@Nullable User user, @Nullable String functionName);

  boolean hasCurrentUserAccess(@Nullable String functionName);

}
