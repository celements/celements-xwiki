package com.celements.rights.function;

import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Strings.*;
import static com.google.common.collect.ImmutableSet.*;

import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;

import com.celements.auth.user.User;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.ModelContext;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.rights.classes.FunctionRightsClass;

@Component
public class DefaultFunctionRightsAccess implements FunctionRightsAccess {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFunctionRightsAccess.class);

  @Requirement
  private IRightsAccessFacadeRole rightsAccess;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext context;

  @Override
  public Set<DocumentReference> getGroupsWithAccess(String functionName) {
    Set<DocumentReference> groups = streamGroupsWithAccess(functionName)
        .collect(toImmutableSet());
    LOGGER.debug("getGroupsWithAccess - for function [{}]: [{}]", functionName, groups);
    return groups;
  }

  private Stream<DocumentReference> streamGroupsWithAccess(String functionName) {
    checkNotNull(emptyToNull(functionName));
    return RefBuilder.from(context.getWikiRef()).space(SPACE_NAME).doc(functionName)
        .buildOpt(DocumentReference.class)
        .map(modelAccess::getOrCreateDocument)
        .map(doc -> XWikiObjectFetcher.on(doc)
            .filter(FunctionRightsClass.CLASS_REF)
            .fetchField(FunctionRightsClass.FIELD_GROUP)
            .stream())
        .orElse(Stream.empty());
  }

  @Override
  public boolean hasGroupAccess(DocumentReference groupDocRef, String functionName) {
    return streamGroupsWithAccess(functionName).anyMatch(group -> group.equals(groupDocRef));
  }

  @Override
  public boolean hasUserAccess(User user, String functionName) {
    return rightsAccess.isSuperAdmin(user) || streamGroupsWithAccess(functionName)
        .anyMatch(groupDocRef -> rightsAccess.isInGroup(groupDocRef, user));
  }

  @Override
  public boolean hasCurrentUserAccess(String functionName) {
    return context.getCurrentUser().toJavaUtil()
        .map(user -> hasUserAccess(user, functionName))
        .orElse(false);
  }

}
