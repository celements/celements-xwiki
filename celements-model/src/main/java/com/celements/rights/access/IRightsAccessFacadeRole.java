package com.celements.rights.access;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.auth.user.User;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

@ComponentRole
public interface IRightsAccessFacadeRole {

  /**
   * instead use hasAccessLevel(EntityReference, EAccessLevel, User)
   */
  @Deprecated
  boolean hasAccessLevel(String right, XWikiUser user, EntityReference entityRef);

  XWikiRightService getRightsService();

  boolean hasAccessLevel(EntityReference ref, EAccessLevel level);

  boolean hasAccessLevel(EntityReference ref, EAccessLevel level, User user);

  /**
   * instead use hasAccessLevel(EntityReference, EAccessLevel, User)
   */
  @Deprecated
  boolean hasAccessLevel(EntityReference ref, EAccessLevel level, XWikiUser user);

  boolean isInGroup(DocumentReference groupDocRef, User user);

  boolean isLoggedIn();

  boolean isAdminUser();

  boolean isAdminUser(User user);

  boolean isAdvancedAdmin();

  boolean isAdvancedAdmin(User user);

  boolean isSuperAdminUser();

  boolean isSuperAdminUser(User user);

  boolean isLayoutEditor();

  boolean isLayoutEditor(User user);

}
