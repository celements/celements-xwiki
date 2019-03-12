package com.celements.model.context;

import java.net.URL;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Optional;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

@ComponentRole
public interface ModelContext {

  public static final String WEB_PREF_DOC_NAME = "WebPreferences";
  public static final String CFG_KEY_DEFAULT_LANG = "default_language";
  public static final String FALLBACK_DEFAULT_LANG = "en";

  /**
   * WARNING: This call is discouraged, use other methods of this service. It will be deprecated
   * once we'll have a replacement for all of them.
   *
   * @return the old, discouraged {@link XWikiContext}
   */
  @NotNull
  XWikiContext getXWikiContext();

  /**
   * @return the current wiki set in context
   */
  @NotNull
  WikiReference getWikiRef();

  /**
   * @param wikiRef
   *          to be set in context
   * @return the wiki which was set before
   */
  @NotNull
  WikiReference setWikiRef(@NotNull WikiReference wikiRef);

  /**
   * @return true if the current context wiki is main
   */
  boolean isMainWiki();

  /**
   * @deprecated instead use {@link ModelUtils#getMainWikiRef()}
   */
  @Deprecated
  @NotNull
  WikiReference getMainWikiRef();

  /**
   * @deprecated instead use {@link #getCurrentDoc}
   */
  @Deprecated
  @Nullable
  XWikiDocument getDoc();

  /**
   * @return the current doc set in context
   */
  @NotNull
  Optional<XWikiDocument> getCurrentDoc();

  /**
   * @return the current doc reference set in context
   */
  @NotNull
  Optional<DocumentReference> getCurrentDocRef();

  /**
   * @return the current space set in context
   */
  @NotNull
  Optional<SpaceReference> getCurrentSpaceRef();

  /**
   * @param doc
   *          to be set in context
   * @return the doc which was set before
   */
  @Nullable
  XWikiDocument setDoc(@Nullable XWikiDocument doc);

  /**
   * @deprecated instead use {@link #getCurrentUser()}
   */
  @Deprecated
  @Nullable
  XWikiUser getUser();

  @NotNull
  Optional<User> getCurrentUser();

  /**
   * @deprecated instead use {@link #setCurrentUser(User)} or {@link #clearCurrentUser()}
   */
  @Deprecated
  @Nullable
  XWikiUser setUser(@Nullable XWikiUser user);

  void setCurrentUser(@NotNull User user);

  void clearCurrentUser();

  /**
   * @deprecated instead use {@link #getUserDocRef()}
   */
  @NotNull
  @Deprecated
  String getUserName();

  @NotNull
  Optional<XWikiRequest> getRequest();

  @NotNull
  Optional<String> getRequestParameter(String key);

  @NotNull
  Optional<XWikiResponse> getResponse();

  /**
   * @return the default language for the current wiki
   */
  @NotNull
  String getDefaultLanguage();

  /**
   * @param ref
   *          from which the default language is extracted (document, space, or wiki)
   * @return the default language for the given reference
   */
  @NotNull
  String getDefaultLanguage(@NotNull EntityReference ref);

  /**
   * @return the current url set in context
   */
  @NotNull
  Optional<URL> getUrl();

  /**
   * @param url
   *          to be set in context
   * @return the url which was set before
   */
  @NotNull
  Optional<URL> setUrl(@Nullable URL url);

}
