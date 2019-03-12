package com.celements.rights.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.celements.auth.user.User;
import com.celements.model.context.ModelContext;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.internal.IEntityReferenceRandomCompleterRole;
import com.celements.web.classes.oldcore.XWikiUsersClass.Type;
import com.google.common.base.Optional;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;

@Component
public class DefaultRightsAccessFacade implements IRightsAccessFacadeRole {

  private static Logger LOGGER = LoggerFactory.getLogger(DefaultRightsAccessFacade.class);

  @Requirement
  IEntityReferenceRandomCompleterRole randomCompleter;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  /**
   * @deprecated instead use {@link #context}
   */
  @Deprecated
  private XWikiContext getContext() {
    return context.getXWikiContext();
  }

  @Override
  public XWikiRightService getRightsService() {
    return getContext().getWiki().getRightService();
  }

  private XWikiGroupService getGroupService() {
    try {
      return getContext().getWiki().getGroupService(getContext());
    } catch (XWikiException xwe) {
      throw new IllegalStateException("failed getting GroupService", xwe);
    }
  }

  @Override
  @Deprecated
  public boolean hasAccessLevel(String right, XWikiUser user, EntityReference entityRef) {
    Optional<EAccessLevel> eAccessLevel = EAccessLevel.getAccessLevel(right);
    if (eAccessLevel.isPresent()) {
      return hasAccessLevel(entityRef, eAccessLevel.get(), user);
    }
    return false;
  }

  @Override
  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level) {
    return hasAccessLevel(ref, level, context.getCurrentUser().orNull());
  }

  @Override
  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level, XWikiUser user) {
    return hasAccessLevel(ref, level, user); // TODO get user
  }

  @Override
  public boolean hasAccessLevel(EntityReference ref, EAccessLevel level, User user) {
    boolean ret = false;
    DocumentReference docRef = null;
    EntityReference entityRef = randomCompleter.randomCompleteSpaceRef(ref);
    if (entityRef instanceof DocumentReference) {
      docRef = (DocumentReference) entityRef;
    } else if (entityRef instanceof AttachmentReference) {
      docRef = ((AttachmentReference) entityRef).getDocumentReference();
    }
    if ((docRef != null) && (level != null)) {
      String accountName = XWikiRightService.GUEST_USER_FULLNAME;
      if (user != null) {
        accountName = modelUtils.serializeRef(user.getDocRef());
      }
      ret = hasAccessLevelInternal(level.getIdentifier(), accountName, modelUtils.serializeRef(
          docRef));
    }
    LOGGER.debug("hasAccessLevel: for ref '{}', level '{}' and user '{}' returned '{}'", ref, level,
        user, ret);
    return ret;
  }

  private boolean hasAccessLevelInternal(String level, String accountName, String fullName) {
    try {
      return getRightsService().hasAccessLevel(level, accountName, fullName, getContext());
    } catch (XWikiException xwe) {
      // already being catched in XWikiRightServiceImpl.hasAccessLevel()
      LOGGER.error("should not happen", xwe);
      return false;
    }
  }

  @Override
  public boolean isInGroup(DocumentReference groupDocRef, User user) {
    try {
      return (user != null) && getGroupService().getAllGroupsReferencesForMember(user.getDocRef(),
          0, 0, getContext()).contains(groupDocRef);
    } catch (XWikiException xwe) {
      LOGGER.warn("isInGroup: failed for user [{}], group [{}]", user, groupDocRef, xwe);
      return false;
    }
  }

  @Override
  public boolean isLoggedIn() {
    boolean ret = context.getCurrentUser().isPresent();
    LOGGER.info("isLoggedIn: {}", ret);
    return ret;
  }

  @Override
  public boolean isAdminUser() {
    return isAdminUser(context.getCurrentUser().orNull());
  }

  @Override
  public boolean isAdminUser(User user) {
    boolean ret = (user != null) && (hasAdminRightsOnPreferences(user) || isInGroup(
        getAdminGroupDocRef(), user));
    LOGGER.debug("isAdminUser: [{}] for user [{}]", ret, user);
    return ret;
  }

  private DocumentReference getAdminGroupDocRef() {
    return new RefBuilder().doc("XWikiAdminGroup").space("XWiki").with(context.getWikiRef()).build(
        DocumentReference.class);
  }

  private boolean hasAdminRightsOnPreferences(User user) {
    boolean ret = false;
    String accountName = modelUtils.serializeRef(user.getDocRef());
    ret = hasAccessLevelInternal("admin", accountName, "XWiki.XWikiPreferences");
    if (!ret && context.getCurrentDoc().isPresent()) {
      String spacePrefFullName = context.getCurrentSpaceRef().get().getName() + ".WebPreferences";
      ret = hasAccessLevelInternal("admin", accountName, spacePrefFullName);
    }
    return ret;
  }

  @Override
  public boolean isAdvancedAdmin() {
    return isAdvancedAdmin(context.getCurrentUser().orNull());
  }

  @Override
  public boolean isAdvancedAdmin(User user) {
    boolean ret = isAdminUser(user) && (user.isGlobal() || (user.getType() == Type.Advanced));
    LOGGER.debug("isAdvancedAdmin: [{}] for user [{}]", ret, user);
    return ret;
  }

  @Override
  public boolean isSuperAdminUser() {
    return isSuperAdminUser(context.getCurrentUser().orNull());
  }

  @Override
  public boolean isSuperAdminUser(User user) {
    boolean ret = isAdminUser(user) && user.isGlobal();
    LOGGER.debug("isSuperAdminUser: [{}] for user [{}]", ret, user);
    return ret;
  }

  @Override
  public boolean isLayoutEditor() {
    return isLayoutEditor(context.getCurrentUser().orNull());
  }

  @Override
  public boolean isLayoutEditor(User user) {
    boolean ret = isAdvancedAdmin(user) || isInGroup(getLayoutEditorsGroupDocRef(), user);
    LOGGER.debug("isLayoutEditor: [{}] for user [{}]", ret, user);
    return ret;
  }

  private DocumentReference getLayoutEditorsGroupDocRef() {
    return new RefBuilder().doc("LayoutEditorsGroup").space("XWiki").with(
        context.getWikiRef()).build(DocumentReference.class);
  }

}
