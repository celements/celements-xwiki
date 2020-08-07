package com.celements.auth.user;

import javax.annotation.concurrent.NotThreadSafe;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.celements.web.classes.oldcore.XWikiUsersClass.Type;
import com.google.common.base.Optional;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;

@NotThreadSafe
@ComponentRole
public interface User {

  public void initialize(@NotNull DocumentReference userDocRef) throws UserInstantiationException;

  @NotNull
  DocumentReference getDocRef();

  @NotNull
  XWikiDocument getDocument();

  boolean isGlobal();

  @NotNull
  XWikiUser asXWikiUser();

  @NotNull
  Optional<String> getEmail();

  @NotNull
  Optional<String> getFirstName();

  @NotNull
  Optional<String> getLastName();

  @NotNull
  Optional<String> getPrettyName();

  @NotNull
  Optional<String> getAdminLanguage();

  @NotNull
  Type getType();

  boolean isActive();

  boolean isSuspended();

}
