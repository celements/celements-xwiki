package com.celements.rights.function;

import java.util.Set;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;

@ComponentRole
public interface FunctionRightsAccess {

  @NotNull
  Set<DocumentReference> getGroupsWithAccess(@Nullable String... functionNames);

  boolean hasGroupAccess(@Nullable DocumentReference groupDocRef,
      @Nullable String... functionNames);

  boolean hasUserAccess(@Nullable User user, @Nullable String... functionNames);

  boolean hasCurrentUserAccess(@Nullable String... functionNames);

}
