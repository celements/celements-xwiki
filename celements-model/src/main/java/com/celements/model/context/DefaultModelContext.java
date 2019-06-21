package com.celements.model.context;

import static com.google.common.base.Preconditions.*;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.auth.user.User;
import com.celements.auth.user.UserInstantiationException;
import com.celements.auth.user.UserService;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

@Component
public class DefaultModelContext implements ModelContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultModelContext.class);

  @Requirement("XWikiStubContextInitializer")
  private ExecutionContextInitializer stubXWikiContextInitializer;

  @Requirement(CelementsFromWikiConfigurationSource.NAME)
  ConfigurationSource wikiConfigSrc;

  @Requirement
  ConfigurationSource defaultConfigSrc;

  @Requirement
  EntityReferenceValueProvider refValProvider;

  @Requirement
  private Execution execution;

  @Override
  public XWikiContext getXWikiContext() {
    XWikiContext context = getXWikiContextFromExecution();
    if (context == null) {
      try {
        stubXWikiContextInitializer.initialize(execution.getContext());
        context = getXWikiContextFromExecution();
        // TODO [CELDEV-347] context may still be null at this point, e.g. in first request
        // see DefaultXWikiStubContextProvider for explanation
        // see AbstractJob#createJobContext to create context from scratch
      } catch (ExecutionContextException exc) {
        new RuntimeException("failed to initialise stub context", exc);
      }
    }
    return checkNotNull(context);
  }

  private XWikiContext getXWikiContextFromExecution() {
    return (XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
  }

  @Override
  public WikiReference getWikiRef() {
    return new WikiReference(getXWikiContext().getDatabase());
  }

  @Override
  public WikiReference setWikiRef(WikiReference wikiRef) {
    WikiReference oldWiki = getWikiRef();
    getXWikiContext().setDatabase(wikiRef.getName());
    return oldWiki;
  }

  @Override
  public boolean isMainWiki() {
    return getModelUtils().getMainWikiRef().equals(getWikiRef());
  }

  @Deprecated
  @Override
  public WikiReference getMainWikiRef() {
    return getModelUtils().getMainWikiRef();
  }

  @Override
  @Deprecated
  public XWikiDocument getDoc() {
    return getDocInternal();
  }

  @Override
  public Optional<XWikiDocument> getCurrentDoc() {
    return Optional.fromNullable(getDocInternal());
  }

  @Override
  public Optional<DocumentReference> getCurrentDocRef() {
    if (getDocInternal() != null) {
      return Optional.of(getDocInternal().getDocumentReference());
    }
    return Optional.absent();
  }

  @Override
  public Optional<SpaceReference> getCurrentSpaceRef() {
    if (getDocInternal() != null) {
      return Optional.of(getDocInternal().getDocumentReference().getLastSpaceReference());
    }
    return Optional.absent();
  }

  @Override
  public SpaceReference getCurrentSpaceRefOrDefault() {
    Optional<DocumentReference> docRef = getCurrentDocRef();
    if (docRef.isPresent()) {
      return getModelUtils().extractRef(docRef.get(), SpaceReference.class).get();
    } else {
      return new RefBuilder().wiki(getWikiRef().getName()).space(refValProvider.getDefaultValue(
          EntityType.SPACE)).build(SpaceReference.class);
    }
  }

  private XWikiDocument getDocInternal() {
    return getXWikiContext().getDoc();
  }

  @Override
  public XWikiDocument setDoc(XWikiDocument doc) {
    XWikiDocument oldDoc = getCurrentDoc().orNull();
    getXWikiContext().setDoc(doc);
    return oldDoc;
  }

  @Override
  @Deprecated
  public XWikiUser getUser() {
    return getXWikiContext().getXWikiUser();
  }

  @Override
  public Optional<User> getCurrentUser() {
    XWikiUser xUser = getXWikiContext().getXWikiUser();
    if (isValidUser(xUser)) {
      DocumentReference userDocRef = getUserService().resolveUserDocRef(xUser.getUser());
      try {
        return Optional.of(getUserService().getUser(userDocRef));
      } catch (UserInstantiationException exc) {
        LOGGER.warn("failed loading user '{}'", userDocRef, exc);
      }
    }
    return Optional.absent();
  }

  private boolean isValidUser(XWikiUser xUser) {
    return (xUser != null) && !Strings.isNullOrEmpty(xUser.getUser()) && !xUser.getUser().equals(
        XWikiRightService.GUEST_USER_FULLNAME);
  }

  @Override
  @Deprecated
  public XWikiUser setUser(XWikiUser xUser) {
    XWikiUser oldUser = getUser();
    if (xUser != null) {
      setCurrentXUser(xUser);
    } else {
      clearCurrentUser();
    }
    return oldUser;
  }

  @Override
  public void setCurrentUser(User user) {
    if (user != null) {
      setCurrentXUser(user.asXWikiUser());
    } else {
      clearCurrentUser();
    }
  }

  private void setCurrentXUser(XWikiUser xUser) {
    getXWikiContext().setUser(xUser.getUser(), xUser.isMain());
  }

  private void clearCurrentUser() {
    getXWikiContext().setUser(null);
  }

  @Override
  @Deprecated
  public String getUserName() {
    return getXWikiContext().getUser();
  }

  @Override
  public Optional<XWikiRequest> getRequest() {
    return Optional.fromNullable(getXWikiContext().getRequest());
  }

  @Override
  public Optional<String> getRequestParameter(String name) {
    Optional<String> ret = Optional.absent();
    if (getRequest().isPresent()) {
      ret = Optional.fromNullable(Strings.emptyToNull(getRequest().get().get(name)));
    }
    return ret;
  }

  @Override
  public Optional<XWikiResponse> getResponse() {
    return Optional.fromNullable(getXWikiContext().getResponse());
  }

  @Override
  public java.util.Optional<String> getLanguage() {
    return java.util.Optional.ofNullable(getXWikiContext().getLanguage());
  }

  @Override
  public String getDefaultLanguage() {
    return getDefaultLanguage(getWikiRef());
  }

  @Override
  public String getDefaultLanguage(EntityReference ref) {
    String ret = getDefaultLangFromDoc(ref);
    if (ret.isEmpty()) {
      ret = getDefaultLangFromConfigSrc(ref);
    }
    LOGGER.trace("getDefaultLanguage: for '{}' got lang" + " '{}'", ref, ret);
    return ret;
  }

  private String getDefaultLangFromDoc(EntityReference ref) {
    String ret = "";
    Optional<DocumentReference> docRef = getModelUtils().extractRef(ref, DocumentReference.class);
    if (docRef.isPresent()) {
      try {
        ret = getModelAccess().getDocument(docRef.get()).getDefaultLanguage();
      } catch (DocumentNotExistsException exc) {
        LOGGER.info("trying to get default language for inexistent document '{}'", docRef);
      }
    }
    return ret;
  }

  private String getDefaultLangFromConfigSrc(EntityReference ref) {
    WikiReference wikiBefore = getWikiRef();
    XWikiDocument docBefore = getCurrentDoc().orNull();
    try {
      ConfigurationSource configSrc;
      setWikiRef(getModelUtils().extractRef(ref, WikiReference.class).or(getWikiRef()));
      XWikiDocument spacePrefDoc = getSpacePrefDoc(ref);
      if (spacePrefDoc != null) {
        setDoc(spacePrefDoc);
        configSrc = defaultConfigSrc; // checks space preferences
      } else {
        configSrc = wikiConfigSrc; // skips space preferences
      }
      return configSrc.getProperty(CFG_KEY_DEFAULT_LANG, FALLBACK_DEFAULT_LANG);
    } finally {
      setWikiRef(wikiBefore);
      setDoc(docBefore);
    }
  }

  @Override
  public XWikiDocument getOrCreateXWikiPreferenceDoc() {
    DocumentReference docRef = new RefBuilder().wiki(getWikiRef().getName()).space(XWIKI_SPACE).doc(
        XWIKI_PREF_DOC_NAME).build(DocumentReference.class);
    return getModelAccess().getOrCreateDocument(docRef);
  }

  @Override
  public XWikiDocument getOrCreateSpacePreferenceDoc(SpaceReference spaceRef) {
    checkNotNull(spaceRef);
    return getModelAccess().getOrCreateDocument(new DocumentReference(WEB_PREF_DOC_NAME, spaceRef));
  }

  private XWikiDocument getSpacePrefDoc(EntityReference ref) {
    XWikiDocument ret = null;
    Optional<SpaceReference> spaceRef = getModelUtils().extractRef(ref, SpaceReference.class);
    if (spaceRef.isPresent()) {
      try {
        ret = getModelAccess().getDocument(new DocumentReference(WEB_PREF_DOC_NAME,
            spaceRef.get()));
      } catch (DocumentNotExistsException exc) {
        LOGGER.debug("no web preferences for space '{}'", spaceRef);
      }
    }
    return ret;
  }

  @Override
  public Optional<URL> getUrl() {
    return Optional.fromNullable(getXWikiContext().getURL());
  }

  @Override
  public Optional<URL> setUrl(URL url) {
    URL oldUrl = getXWikiContext().getURL();
    getXWikiContext().setURL(url);
    return Optional.fromNullable(oldUrl);
  }

  private ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  private IModelAccessFacade getModelAccess() {
    return Utils.getComponent(IModelAccessFacade.class);
  }

  private UserService getUserService() {
    return Utils.getComponent(UserService.class);
  }

}
